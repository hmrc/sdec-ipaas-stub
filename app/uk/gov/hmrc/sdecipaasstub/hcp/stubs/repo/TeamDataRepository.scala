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

package uk.gov.hmrc.sdecipaasstub.hcp.stubs.repo

import cats.effect.MonadCancelThrow
import doobie.*
import doobie.implicits.*
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.TeamDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.SdecTeam

class TeamDataRepository[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends TeamDataAccessAlgebra[F]:

  override def getByTeamName(team: String): F[Option[SdecTeam]] =
    sql"""
      SELECT
        id,
        srs_name,
        is_task_based
      FROM sdec_team
      WHERE srs_name = $team
    """
      .query[SdecTeam]
      .option
      .transact(xa)

  override def insert(
    team:        String,
    isTaskBased: Boolean
  ): F[Long] =
    sql"""
      INSERT INTO sdec_team (
        srs_name,
        is_task_based
      )
      VALUES (
        $team,
        $isTaskBased
      )
    """.update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)
