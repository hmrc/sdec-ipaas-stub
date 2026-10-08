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
import doobie.util.transactor.Transactor
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logging}
import uk.gov.hmrc.sdecipaasstub.config.Database

import javax.inject.{Inject, Singleton}

@Singleton
class DatabaseTransactor @Inject() (
  configuration: Configuration,
  lifecycle:     ApplicationLifecycle
)(implicit
  runtime: IORuntime
) extends Logging:

  private val resource =
    Database.transactor[IO](
      configuration.underlying
    )

  private val (transactor, release) =
    resource.allocated.unsafeRunSync()

  lifecycle.addStopHook { () =>
    release.as(()).unsafeToFuture()
  }

  val xa: Transactor[IO] = transactor
