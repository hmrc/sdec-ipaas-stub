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
import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.sdecipaasstub.dto.{CreateThreadRequest, ErrorMessage}
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, IOActionBuilder}
import uk.gov.hmrc.sdecipaasstub.service.algebra.ThreadServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.errors.{StaffLookupError, TeamLookupError}

@Singleton
class ThreadController @Inject() (
  cc:            ControllerComponents,
  action:        IOActionBuilder,
  threadService: ThreadServiceAlgebra[IO]
) extends BackendController(cc)
    with ApplicationLogger[IO] {

  def insert(): Action[AnyContent] =
    action.asyncIO { request =>
      request.body.asJson match {
        case Some(json) =>
          for {
            _        <- logger.info(s"Handling POST for $json")
            response <- handleCreateThread(CreateThreadRequest.fromJson(json))
          } yield response
        case None =>
          getBadRequest(Seq("Request body must contain JSON"))
      }
    }

  private def handleCreateThread(jsResult: JsResult[CreateThreadRequest]): IO[Result] =
    jsResult match {
      case JsSuccess(createRequest, _) =>
        threadService
          .insert(createRequest)
          .map(response => Created(Json.toJson(response)))
          .recoverWith {
            case e: StaffLookupError => getBadRequest(Seq(e.getMessage))
            case e: TeamLookupError  => getBadRequest(Seq(e.getMessage))
          }
      case JsError(errors) =>
        handleJsErrors(errors)
    }

  private def handleJsErrors(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): IO[Result] =
    val jsonErrors = errors.map { (path, validationErrors) =>
      s"${path.toString} Errors: ${validationErrors.mkString(",")}"
    }.toList
    getBadRequest(jsonErrors)

  private def getBadRequest(errors: Seq[String]): IO[Result] =
    IO.pure(BadRequest(Json.toJson(ErrorMessage(errors))))

}
