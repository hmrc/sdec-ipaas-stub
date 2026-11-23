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

import cats.effect.IO
import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.sdecipaasstub.dto.{ErrorMessage, Staff, StaffUpdate}
import uk.gov.hmrc.sdecipaasstub.implicits.RequestSyntax.JsonRequestOps
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, IOActionBuilder}
import uk.gov.hmrc.sdecipaasstub.service.StubMappingServiceAlgebra

import scala.language.postfixOps

@Singleton
class StaffController @Inject() (
  cc:         ControllerComponents,
  action:     IOActionBuilder,
  stubMapper: StubMappingServiceAlgebra
) extends BackendController(cc)
    with ApplicationLogger[IO] {

  def updateOrInsert(): Action[JsValue] =
    action.asyncIOJsValue { request =>
      request.bodyAs[StaffUpdate] match {
        case Right(staffUpdate) => IO.pure(stubMapper.mapIdentifierToResponse(staffUpdate.pid))
        case Left(errorMessage) => IO.pure(BadRequest(Json.toJson(errorMessage)))
      }
    }

  def insert(): Action[JsValue] =
    action.asyncIOJsValue { request =>
      request.bodyAs[Staff] match {
        case Right(staff)       => IO.pure(stubMapper.mapIdentifierToResponse(staff.pid))
        case Left(errorMessage) => IO.pure(BadRequest(Json.toJson(errorMessage)))
      }
    }

}
