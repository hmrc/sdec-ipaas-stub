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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.SdecThread

import java.time.LocalDate

case class ThreadSummary(
  reference:     String,
  caseReference: Option[String],
  contactName:   String,
  status:        ThreadStatus,
  lastReplyDate: Option[LocalDate],
  deadline:      Option[LocalDate],
  contactStatus: ContactStatus
)

object ThreadSummary {
  given format: OFormat[ThreadSummary] = Json.format[ThreadSummary]

  def fromEntity(entity: SdecThread): ThreadSummary =
    ThreadSummary(
      reference = entity.reference,
      caseReference = entity.caseReference,
      contactName = entity.name,
      status = ThreadStatus.fromEntity(entity.status),
      lastReplyDate = entity.lastUpdatedTimestamp.map(_.toLocalDate),
      deadline = entity.requiredBy,
      contactStatus = ContactStatus.ReplyReceived //Default for now
    )
}
