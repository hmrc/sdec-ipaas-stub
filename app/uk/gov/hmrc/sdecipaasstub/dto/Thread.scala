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

import play.api.libs.json.{JsResult, JsValue, Json, OFormat}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecThread, ThreadStatus}

import java.time.{LocalDate, LocalDateTime}

final case class Thread(
  status:               Option[ThreadStatus],
  createdBy:            Long,
  createdTimestamp:     Option[LocalDateTime],
  lastUpdatedTimestamp: Option[LocalDateTime],
  threadExpiryDate:     Option[LocalDate],
  requestedItems:       Option[String],
  caseReference:        Option[String],
  recipientId:          Option[Long],
  email:                Option[String],
  nino:                 Option[String],
  message:              Option[String],
  requiredBy:           Option[LocalDate],
  teamId:               Long
)

object Thread:
  given format: OFormat[Thread] = Json.format[Thread]

  def fromJson(json: JsValue): JsResult[Thread] = json.validate[Thread]

  def fromEntity(entity: SdecThread): Thread =
    Thread(
      status = Option(entity.status),
      createdBy = entity.createdBy,
      createdTimestamp = entity.createdTimestamp,
      lastUpdatedTimestamp = entity.lastUpdatedTimestamp,
      threadExpiryDate = entity.threadExpiryDate,
      requestedItems = entity.requestedItems,
      caseReference = entity.caseReference,
      recipientId = entity.recipientId,
      email = Some(entity.email),
      nino = entity.nino,
      message = entity.message,
      requiredBy = entity.requiredBy,
      teamId = entity.teamId
    )
