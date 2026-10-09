package uk.gov.hmrc.sdecipaasstub.dto

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.Role

case class TeamRole(team: String, role: Role)

object TeamRole:
  given format: OFormat[TeamRole] = Json.format[TeamRole]
