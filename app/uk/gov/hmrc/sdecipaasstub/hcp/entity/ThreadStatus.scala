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

package uk.gov.hmrc.sdecipaasstub.hcp.entity

import doobie.util.meta.Meta
import play.api.libs.json.*

enum ThreadStatus {
  case PENDING
  case ACTIVE
  case CLOSED
  case REOPENED
}

object ThreadStatus {
  private def parseThreadStatus(value: String): ThreadStatus =
    ThreadStatus.values.find(_.toString.equalsIgnoreCase(value)).getOrElse(PENDING)

  given Meta[ThreadStatus] =
    Meta[String].timap(parseThreadStatus)(_.toString)

  given Format[ThreadStatus] = Format(
    Reads {
      case JsString(value) =>
        JsSuccess(parseThreadStatus(value))

      case _ =>
        JsError("Thread Status must be a string")
    },
    Writes(threadStatus => JsString(threadStatus.toString))
  )

}
