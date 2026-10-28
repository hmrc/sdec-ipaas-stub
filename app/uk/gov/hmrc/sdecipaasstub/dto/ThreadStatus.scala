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

package uk.gov.hmrc.sdecipaasstub.dto

import play.api.libs.json.*
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus as EntityStatus

enum ThreadStatus {
  case Pending
  case Active
  case Closed
  case Reopened
  case Undefined
}

object ThreadStatus {

  def parseStatus(value: String): ThreadStatus =
    ThreadStatus.values.find(_.toString.equalsIgnoreCase(value)).getOrElse(Undefined)

  given Format[ThreadStatus] = Format(
    Reads {
      case JsString(value) =>
        JsSuccess(parseStatus(value))

      case _ =>
        JsError("Role must be a string")
    },
    Writes(status => JsString(status.toString))
  )

  def fromEntity(status: EntityStatus): ThreadStatus =
    status match {
      case EntityStatus.PENDING  => Pending
      case EntityStatus.ACTIVE   => Active
      case EntityStatus.CLOSED   => Closed
      case EntityStatus.REOPENED => Reopened
      case _                     => Undefined
    }
}
