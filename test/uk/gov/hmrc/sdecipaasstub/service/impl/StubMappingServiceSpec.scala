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

package uk.gov.hmrc.sdecipaasstub.service.impl

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, NotFound, Ok}
import uk.gov.hmrc.sdecipaasstub.mappings.StubId
import uk.gov.hmrc.sdecipaasstub.service.StubMappingService

class StubMappingServiceSpec extends AnyWordSpec with Matchers {

  val underTest = new StubMappingService()

  "mapIdentifierToResponse" should {

    "map a string identifier to a stub correctly" in {

      val expectedRepsonse = """{
                               |  "response": "this is a test"
                               |}""".stripMargin

      underTest.mapIdentifierToResponse(StubId.Test.toString) shouldBe Ok(Json.parse(expectedRepsonse))
    }

    "map return Notfound if the stub does not exist" in {

      underTest.mapIdentifierToResponse("MISSING") shouldBe NotFound(Json.toJson("message" -> "stub not found"))
    }

    "map return Notfound if the file associated with the stub does not exist" in {

      underTest.mapIdentifierToResponse(StubId.MissingFileTestOnly.toString) shouldBe NotFound(
        Json.toJson("message" -> s"file not found for stubId MissingFileTestOnly")
      )
    }

    "map return InternalServerError if the content of the response file is not json" in {

      underTest.mapIdentifierToResponse(StubId.MissingJsonTestOnly.toString) shouldBe InternalServerError(
        Json.toJson(
          Json.toJson("message" -> s"an error occurred while attempting to read stub file; stubId MissingJsonTestOnly")
        )
      )
    }
  }

}
