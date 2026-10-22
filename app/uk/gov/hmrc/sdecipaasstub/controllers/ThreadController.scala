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
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.sdecipaasstub.dto.{ErrorMessage, ThreadUpdate}
import uk.gov.hmrc.sdecipaasstub.implicits.RequestSyntax.JsonRequestOps
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, IOActionBuilder}
import uk.gov.hmrc.sdecipaasstub.service.algebra.ThreadServiceAlgebra

@Singleton
class ThreadController @Inject() (
  cc:            ControllerComponents,
  action:        IOActionBuilder,
  threadService: ThreadServiceAlgebra[IO]
) extends BackendController(cc)
    with ApplicationLogger[IO] {

  def updateThread(threadRef: String): Action[JsValue] =
    action.asyncIOJsValue { request =>
      request.bodyAs[ThreadUpdate] match {
        case Right(threadUpdate) => handleUpdateThread(threadUpdate, threadRef)
        case Left(errorMessage)  => IO.pure(BadRequest(Json.toJson(errorMessage)))
      }
    }

  private def handleUpdateThread(threadUpdate: ThreadUpdate, threadRef: String): IO[Result] =
    threadService
      .updateThreadAndRecipient(threadUpdate, threadRef)
      .map(response => Ok(Json.toJson(response)))
      .recover(_ => InternalServerError)
}
