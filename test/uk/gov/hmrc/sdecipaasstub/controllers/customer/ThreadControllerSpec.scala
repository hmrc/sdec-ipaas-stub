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

package uk.gov.hmrc.sdecipaasstub.controllers.customer

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.scalatestplus.play.PlaySpec
import pdi.jwt.*
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{NotFound, Ok}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.sdecipaasstub.dto.*
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus
import uk.gov.hmrc.sdecipaasstub.infrastructure.{AuthenticatedIOActionBuilder, JwtUser}
import uk.gov.hmrc.sdecipaasstub.service.StubMappingServiceAlgebra

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContextExecutor

class ThreadControllerSpec extends PlaySpec {

  given ec: ExecutionContextExecutor =
    scala.concurrent.ExecutionContext.global

  given actorSystem: ActorSystem =
    ActorSystem("CustomerThreadControllerSpec")

  given materializer: Materializer =
    SystemMaterializer(actorSystem).materializer

  private lazy val cc         = stubControllerComponents()
  private lazy val runtime    = cats.effect.unsafe.IORuntime.global
  private lazy val authAction =
    new AuthenticatedIOActionBuilder(
      cc,
      runtime,
      ec
    )

  private val validJwtUser =
    JwtUser(
      internalId = Some("12345"),
      firstName = Some("John"),
      lastName = Some("Smith"),
      email = Some("john.smith@example.com"),
      phoneNumber = None,
      nino = "WM111111D",
      confidenceLevel = 250
    )

  private val validToken =
    JwtJson.encode(
      Json.obj(
        "internalId"      -> validJwtUser.internalId,
        "firstName"       -> validJwtUser.firstName,
        "lastName"        -> validJwtUser.lastName,
        "email"           -> validJwtUser.email,
        "phoneNumber"     -> None,
        "nino"            -> validJwtUser.nino,
        "confidenceLevel" -> validJwtUser.confidenceLevel
      ),
      "secretKey",
      JwtAlgorithm.HS256
    )

  private val threadReference =
    ThreadReference(
      id = "THREAD1000AA",
      status = ThreadStatus.ACTIVE,
      createdBy = StaffDetails(
        id = 1L,
        pid = "1001",
        name = "John Test"
      ),
      createdTimeStamp = LocalDateTime.now,
      lastUpdatedTimeStamp = LocalDateTime.now,
      threadExpiryDate = LocalDate.now.plusMonths(1),
      associatedCaseReference = "CASE-100001",
      threadOwner = None,
      owningTeam = Team(
        id = 1L,
        srsName = "VAT"
      ),
      recipientDetails = RecipientDetails(
        firstName = "John",
        lastName = "Smith",
        email = "user@test.com",
        phoneNumber = "No phone number",
        nationalInsuranceNumber = "WM111111D",
        hasRelatedCase = true,
        caseReferenceNumber = Some("CASE-100001")
      ),
      threadDetails = ThreadDetails(
        message = "Where are the files?",
        responseDate = LocalDate.now.plusMonths(1)
      )
    )

  val stubMappingServiceSuccess: StubMappingServiceAlgebra = new StubMappingServiceAlgebra {
    def mapIdentifierToResponse(id: String): Result = Ok(Json.toJson(threadReference))
  }

  val stubMappingServiceBadRequest: StubMappingServiceAlgebra = new StubMappingServiceAlgebra {
    def mapIdentifierToResponse(id: String): Result = NotFound("User details do not match recipient")
  }

  private def controllerWith(
    stubMappingService: StubMappingServiceAlgebra
  ): ThreadController =
    new ThreadController(cc, authAction, stubMappingService)

  "ThreadController" should {

    "return OK when thread lookup succeeds" in {

      val controller = controllerWith(stubMappingServiceSuccess)

      val request = FakeRequest(PUT, "/api/v1/customer/thread/THREAD1000AA")
        .withHeaders("Authorization" -> s"Bearer $validToken")

      val result = call(controller.getThreadReference("THREAD1000AA"), request)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(threadReference)
    }

    "return NotFound when stubId does not exist" in {

      val controller = controllerWith(stubMappingServiceBadRequest)

      val request = FakeRequest(PUT, "/api/v1/customer/thread/THREAD1000AA")
        .withHeaders("Authorization" -> s"Bearer $validToken")

      val result = call(controller.getThreadReference("THREAD1000AA"), request)

      status(result) mustBe NOT_FOUND

      contentAsString(result) mustBe "User details do not match recipient"
    }

    "return Unauthorized when bearer token is missing" in {

      val controller = controllerWith(stubMappingServiceSuccess)

      val request = FakeRequest(PUT, "/api/v1/customer/thread/THREAD1000AA")

      val result = call(controller.getThreadReference("THREAD1000AA"), request)

      status(result) mustBe UNAUTHORIZED
    }
  }
}
