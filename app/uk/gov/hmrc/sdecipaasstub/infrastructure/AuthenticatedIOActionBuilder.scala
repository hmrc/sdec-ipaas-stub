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
import pdi.jwt.*
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*

import scala.concurrent.ExecutionContext

@Singleton
class AuthenticatedIOActionBuilder @Inject() (
  cc:      ControllerComponents,
  runtime: IORuntime,
  ec:      ExecutionContext
) extends AbstractController(cc) {

  private val secret = "secretKey"

  def asyncIO(
    block: AuthenticatedRequest[AnyContent] => IO[Result]
  ): Action[AnyContent] =
    Action.async(cc.parsers.default) { request =>
      validateRequest(request) match {
        case Left(error) =>
          IO.pure(error).unsafeToFuture()(runtime)

        case Right(user) =>
          block(
            new AuthenticatedRequest(
              user,
              request
            )
          ).unsafeToFuture()(runtime)
      }
    }

  private def validateRequest(
    request: Request[AnyContent]
  ): Either[Result, JwtUser] =
    for {
      token <- extractToken(request)
      json  <- decodeToken(token)
      user  <- parseUser(json)
      valid <- validateConfidenceLevel(user)
    } yield valid

  private def extractToken(
    request: Request[AnyContent]
  ): Either[Result, String] =
    request.headers
      .get("Authorization")
      .map(_.stripPrefix("Bearer ").trim)
      .filter(_.nonEmpty)
      .toRight(
        Unauthorized(
          Json.obj(
            "message" -> "Bearer token not supplied"
          )
        )
      )

  private def decodeToken(
    token: String
  ): Either[Result, JsValue] =
    JwtJson
      .decodeJson(
        token,
        secret,
        Seq(JwtAlgorithm.HS256)
      )
      .toEither
      .left
      .map { _ =>
        Unauthorized(
          Json.obj(
            "message" -> "Invalid bearer token"
          )
        )
      }

  private def parseUser(
    json: JsValue
  ): Either[Result, JwtUser] =
    json
      .validate[JwtUser]
      .asEither
      .left
      .map { _ =>
        BadRequest(
          Json.obj(
            "message" -> "Invalid JWT payload"
          )
        )
      }

  private def validateConfidenceLevel(
    user: JwtUser
  ): Either[Result, JwtUser] =
    if user.confidenceLevel >= 250 then {
      Right(user)
    } else {
      Left(
        BadRequest(
          Json.obj(
            "message" -> "Confidence level must be >= 250"
          )
        )
      )
    }
}
