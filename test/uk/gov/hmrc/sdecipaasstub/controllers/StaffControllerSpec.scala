/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.sdecipaasstub.controllers
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.sdecipaasstub.dto.{Staff, StaffTeam}
import uk.gov.hmrc.sdecipaasstub.infrastructure.IOActionBuilder
import uk.gov.hmrc.sdecipaasstub.service.StubMappingServiceAlgebra

import scala.concurrent.ExecutionContextExecutor

class StaffControllerSpec extends PlaySpec {
  given ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  given actorSystem: ActorSystem =
    ActorSystem("StaffControllerSpec")

  given materializer: Materializer =
    SystemMaterializer(actorSystem).materializer

  private lazy val cc            = stubControllerComponents()
  private lazy val runtime       = cats.effect.unsafe.IORuntime.global
  private lazy val actionBuilder = new IOActionBuilder(cc, runtime, ec)

  private val staff = Staff(pid = "1001", name = "Test User", email = "tu@tu.gov.uk", srs = "sdec_vat_user")
  private val staffTeam: StaffTeam = StaffTeam(staffId = 1L, teamId = 1L)
  private val staffJson = Json.toJson(staff)

  private val stubMappingService = new StubMappingServiceAlgebra {
    override def mapIdentifierToResponse(id: String): Result = Ok(Json.toJson(staffTeam))
  }

  val controller = new StaffController(cc, actionBuilder, stubMappingService)

  "StaffController" should {

    "return Bad Request when JSON is not defined" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/staff")

      val result = call(controller.insert(), request)

      status(result) mustBe BAD_REQUEST
    }

    "handle POST successfully" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/staff")
        .withJsonBody(staffJson)

      val result = call(controller.insert(), request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(staffTeam)
    }
  }

}
