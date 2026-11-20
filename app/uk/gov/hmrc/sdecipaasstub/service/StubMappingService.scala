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

package uk.gov.hmrc.sdecipaasstub.service

import jakarta.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import uk.gov.hmrc.sdecipaasstub.mappings.{Stub, StubId}

import scala.io.Source
import scala.util.Try

trait StubMappingServiceAlgebra {
  def mapIdentifierToResponse(id: String): Result
}

class StubMappingService @Inject() extends StubMappingServiceAlgebra {

  def mapIdentifierToResponse(id: String): Result = {
    val maybeStub = Try(Stub.values.find(stub => stub.id == StubId.valueOf(id))).toOption.flatten
    maybeStub match {
      case Some(stub) =>
        val content = readJsonFile(stub.responsePath)
        stub.resultStatus(content)
      case None => NotFound("stub not found")
    }
  }

  private def readJsonFile(filePath: String): JsValue = {
    val bufferedSource = Source.fromResource(filePath)
    val jsonString     = bufferedSource.getLines().mkString("\n")
    Json.parse(jsonString)
  }
}
