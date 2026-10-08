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

package uk.gov.hmrc.sdecipaasstub.infrastructure

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import jakarta.inject.{Inject, Singleton}
import play.api.mvc.*

import scala.concurrent.ExecutionContext

@Singleton
class IOActionBuilder @Inject()(
  cc:      ControllerComponents,
  runtime: IORuntime,
  ec: ExecutionContext
) extends AbstractController(cc) {

  def asyncIO(
    block: Request[AnyContent] => IO[Result]
  ): Action[AnyContent] =
    Action.async { request =>
      block(request).unsafeToFuture()(runtime)
    }

}
