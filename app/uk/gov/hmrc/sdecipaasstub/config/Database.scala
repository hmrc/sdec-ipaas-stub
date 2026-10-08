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

package uk.gov.hmrc.sdecipaasstub.config

import cats.effect.Async
import cats.effect.kernel.Resource
import com.typesafe.config.Config
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database:

  def transactor[F[_]: Async](
    config: Config
  ): Resource[F, HikariTransactor[F]] =
    HikariTransactor.newHikariTransactor(
      driverClassName = "org.h2.Driver",
      url = config.getString("db.url"),
      user = config.getString("db.user"),
      pass = config.getString("db.password"),
      connectEC = ExecutionContexts.synchronous
    )
