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

package uk.gov.hmrc.sdecipaasstub.model

import uk.gov.hmrc.sdecipaasstub.dto.TeamRole

case class SRSEnrollment(
  sdec: String,
  team: String,
  role: String
) {
  override def toString: String = s"${this.sdec}_${this.team}${this.role.toString}"
}

object SRSEnrollment:
  private val TokenDelimiter = '_'
  private val SrsIdentifier  = "SDEC"

  def extractFromEnrollment(enrollment: String): SRSEnrollment =
    val firstToken = enrollment.indexOf(TokenDelimiter)
    val lastToken  = enrollment.lastIndexOf(TokenDelimiter)
    SRSEnrollment(
      enrollment.substring(0, firstToken),
      enrollment.substring(firstToken + 1, lastToken),
      enrollment.substring(lastToken + 1)
    )

  def toSRSEnrollment(teamRole: TeamRole): SRSEnrollment =
    SRSEnrollment(
      sdec = SrsIdentifier,
      team = teamRole.team,
      role = teamRole.role.toString
    )
