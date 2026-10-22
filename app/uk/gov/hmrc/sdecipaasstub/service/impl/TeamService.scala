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

package uk.gov.hmrc.sdecipaasstub.service.impl

import cats.effect.Sync
import cats.syntax.all.*
import uk.gov.hmrc.sdecipaasstub.dto.Team
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.TeamDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.service.algebra.TeamServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.errors.{TeamFindError, TeamInsertError}

import javax.inject.Inject

class TeamService[F[_]: Sync] @Inject() (
  teamDataAccess: TeamDataAccessAlgebra[F]
) extends TeamServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def getByTeamName(teamName: String): F[Option[Team]] = {
    for {
      _    <- logger.info(s"Getting by Team Name: $teamName")
      team <- teamDataAccess.getByTeamName(teamName)
    } yield team.map(entity => Team.fromEntity(entity))
  }.recoverWith { e =>
    logger.warn(s"Get By Team Name failed with ${e.getMessage}")
    throw new TeamFindError(e.getMessage)
  }

  override def insert(team: String, isTaskBased: Boolean): F[Long] = {
    for {
      _      <- logger.info(s"Team Insert: $team, $isTaskBased")
      teamId <- teamDataAccess.insert(team, isTaskBased)
    } yield teamId
  }.recoverWith { e =>
    logger.warn(s"Team Insert failed with ${e.getMessage}")
    throw new TeamInsertError(e.getMessage)
  }

}
