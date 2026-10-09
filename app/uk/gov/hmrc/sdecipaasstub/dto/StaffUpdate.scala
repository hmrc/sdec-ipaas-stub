package uk.gov.hmrc.sdecipaasstub.dto

import play.api.libs.json.{JsResult, JsValue, Json, OFormat}

case class StaffUpdate(
  pid:         String,
  name:        String,
  email:       String,
  enrollments: List[TeamRole]
)

object StaffUpdate:
  given format: OFormat[StaffUpdate] = Json.format[StaffUpdate]

  def fromJson(json: JsValue): JsResult[StaffUpdate] = json.validate[StaffUpdate]