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

final case class StaffUpdate(
  pid:         String,
  name:        String,
  email:       String,
  enrollments: List[TeamRole]
)

object StaffUpdate:
  given format: OFormat[StaffUpdate] = Json.format[StaffUpdate]

  def fromJson(json: JsValue): JsResult[StaffUpdate] = json.validate[StaffUpdate]
