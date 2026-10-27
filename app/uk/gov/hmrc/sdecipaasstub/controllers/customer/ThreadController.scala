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

import cats.effect.IO
import jakarta.inject.{Inject, Singleton}
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, AuthenticatedIOActionBuilder, AuthenticatedRequest, IOActionBuilder}
import uk.gov.hmrc.sdecipaasstub.service.algebra.ThreadServiceAlgebra

@Singleton
class ThreadController @Inject() (
                                   cc: ControllerComponents,
                                   authAction: AuthenticatedIOActionBuilder,
                                   threadService: ThreadServiceAlgebra[IO]
                                 ) extends BackendController(cc)
  with ApplicationLogger[IO] {

  def getThreadReference(threadId: String): Action[AnyContent] =
    authAction.asyncIO { request =>
      given AuthenticatedRequest[AnyContent] = request

      for {
        _ <- logger.info(s"Getting thread information for $threadId")
        thread <- threadService.getThreadInfoByThreadId(threadId)
      } yield Ok(Json.toJson(thread))
    }
}