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

package uk.gov.hmrc.sdecipaasstub.scripts

import cats.effect.{IO, IOApp}
import pdi.jwt.*
import play.api.libs.json.Json
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger

import java.time.Instant

object JwtGenerator extends IOApp.Simple with ApplicationLogger[IO] {

  override def run: IO[Unit] = {
    val claim =
      JwtClaim(
        Json
          .obj(
            "internalId"      -> "12345",
            "firstName"       -> "John",
            "lastName"        -> "Smith",
            "email"           -> "john.smith@example.com",
            "phoneNumber"     -> None,
            "nino"            -> "WM111111D",
            "confidenceLevel" -> 250
          )
          .toString(),
        expiration = Some(
          Instant
            .now()
            .plusSeconds(3600)
            .getEpochSecond
        )
      )

    val token =
      Jwt.encode(
        claim,
        "secretKey",
        JwtAlgorithm.HS256
      )

    // Run sbt "runMain uk.gov.hmrc.sdecipaasstub.scripts.JwtGenerator" in terminal
    logger.info(s"Generating JWT Token: $token")

  }
}
