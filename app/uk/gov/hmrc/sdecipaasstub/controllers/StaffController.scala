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
import play.api.libs.json.{JsError, JsPath, JsResult, JsSuccess, Json, JsonValidationError}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.sdecipaasstub.dto.{ErrorMessage, Staff, StaffUpdate}
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, IOActionBuilder}
import uk.gov.hmrc.sdecipaasstub.service.algebra.StaffServiceAlgebra

import scala.language.postfixOps

@Singleton
class StaffController @Inject() (
  cc:           ControllerComponents,
  action:       IOActionBuilder,
  staffService: StaffServiceAlgebra[IO]
) extends BackendController(cc)
    with ApplicationLogger[IO] {

  def updateOrInsert: Action[AnyContent] =
    action.asyncIO { implicit request =>
      request.body.asJson match {
        case Some(json) =>
          for {
            _ <- logger.info(s"Handling PUT for $json")
            staffUpdate = StaffUpdate.fromJson(json)
            response <- handleStaffUpdate(staffUpdate)
          } yield response
        case None => getBadRequest(Seq("Request body must contain JSON"))
      }
    }

  def insert: Action[AnyContent] =
    action.asyncIO { implicit request =>
      request.body.asJson match {
        case Some(json) =>
          for {
            _ <- logger.info(s"Handling POST for $json")
            staff = Staff.fromJson(json)
            response <- handleStaffUpsert(staff)
          } yield response
        case None =>
          getBadRequest(Seq("Request body must contain JSON"))
      }
    }

  private def handleStaffUpdate(jsResult: JsResult[StaffUpdate]): IO[Result] =
    jsResult match {
      case JsSuccess(staffUpdate, _) =>
        staffService.upsert(staffUpdate).map(r => Json.toJson(r))
      case JsError(errors)        =>
        handleJsErrors(errors)
    }

  private def handleStaffUpsert(jsResult: JsResult[Staff]): IO[Result] =
    jsResult match {
      case JsSuccess(staff, _) =>
        staffService.upsert(staff).map(r => Ok(Json.toJson(r)))
      case JsError(errors) =>
        handleJsErrors(errors)
    }

  private def handleJsErrors(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =
    val jsonErrors = errors.map { (path, validationErrors) =>
      s"${path.toString} Errors: ${validationErrors.mkString(",")}"
      }.toList
    getBadRequest(jsonErrors)

  private def getBadRequest(errors: Seq[String]): IO[Result] =
    IO.pure(BadRequest(Json.toJson(ErrorMessage(errors))))

}
