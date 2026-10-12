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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.sdecipaasstub.dto.Message
import uk.gov.hmrc.sdecipaasstub.infrastructure.IOActionBuilder

import scala.concurrent.ExecutionContextExecutor

class MicroserviceHelloWorldControllerSpec extends PlaySpec {
  given ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  private lazy val cc            = stubControllerComponents()
  private lazy val runtime       = cats.effect.unsafe.IORuntime.global
  private lazy val actionBuilder = new IOActionBuilder(cc, runtime, ec)

  val controller = new MicroserviceHelloWorldController(cc, actionBuilder)

  "MicroserviceHelloController" should {
    "return ok" in {
      val request = FakeRequest(GET, "")

      val result = controller.hello(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(Message("hello"))
    }
  }

}
