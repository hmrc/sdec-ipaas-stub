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

package uk.gov.hmrc.sdecipaasstub.controllers.staff

import cats.effect.IO
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.sdecipaasstub.controllers.staff.ThreadController
import uk.gov.hmrc.sdecipaasstub.dto.{CreateThreadRequest, ThreadResponse}
import uk.gov.hmrc.sdecipaasstub.infrastructure.IOActionBuilder
import uk.gov.hmrc.sdecipaasstub.service.algebra.ThreadServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.errors.{StaffLookupError, TeamLookupError}

import scala.concurrent.ExecutionContextExecutor

class ThreadControllerSpec extends PlaySpec {
  given ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  given actorSystem: ActorSystem =
    ActorSystem("ThreadControllerSpec")

  given materializer: Materializer =
    SystemMaterializer(actorSystem).materializer

  private lazy val cc            = stubControllerComponents()
  private lazy val runtime       = cats.effect.unsafe.IORuntime.global
  private lazy val actionBuilder = new IOActionBuilder(cc, runtime, ec)

  private val unknownPid  = "9999"
  private val unknownTeam = "Unknown_Team"
  private val reference   = "ABCD12345"

  private lazy val threadService = new ThreadServiceAlgebra[IO] {

    override def insert(request: CreateThreadRequest): IO[ThreadResponse] =
      if request.pid == unknownPid then IO.raiseError(new StaffLookupError(s"No staff found for pid ${request.pid}"))
      else if request.team == unknownTeam then
        IO.raiseError(new TeamLookupError(s"No team found with name ${request.team}"))
      else IO.pure(ThreadResponse(reference))

    override def findIdByRef(threadRef: String): IO[Option[Long]] =
      IO.pure(Some(1L))
  }

  val controller = new ThreadController(cc, actionBuilder, threadService)

  "ThreadController" should {
    val createRequest = CreateThreadRequest(pid = "1001", team = "Child_Benefit")
    val requestJson   = Json.toJson(createRequest)

    "return Bad Request when JSON is not defined" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/thread")

      val result = call(controller.insert(), request)

      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request when JSON is invalid" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/thread")
        .withJsonBody(Json.obj("pid" -> "1001"))

      val result = call(controller.insert(), request)

      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request when staff cannot be found" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/thread")
        .withJsonBody(Json.toJson(createRequest.copy(pid = unknownPid)))

      val result = call(controller.insert(), request)

      status(result) mustBe BAD_REQUEST
    }

    "return Bad Request when team cannot be found" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/thread")
        .withJsonBody(Json.toJson(createRequest.copy(team = unknownTeam)))

      val result = call(controller.insert(), request)

      status(result) mustBe BAD_REQUEST
    }

    "handle POST successfully" in {
      val request = FakeRequest(POST, "/sdec-ipaas-stub/api/v1/thread")
        .withJsonBody(requestJson)

      val result = call(controller.insert(), request)

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.toJson(ThreadResponse(reference))
    }
  }

}
