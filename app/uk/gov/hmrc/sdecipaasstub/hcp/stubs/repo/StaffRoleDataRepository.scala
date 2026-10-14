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

import cats.effect.kernel.MonadCancelThrow
import doobie.*
import doobie.implicits.*
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.StaffRoleDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.StaffRole
import uk.gov.hmrc.sdecipaasstub.model.SRSEnrollment

class StaffRoleDataRepository[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends StaffRoleDataAccessAlgebra[F]:

  override def findByStaffAndTeam(
    staffId: Long,
    teamId:  Long
  ): F[StaffRole] =
    sql"""
      SELECT
        id,
        staff_id,
        team_id,
        srs_role
      FROM staff_role
      WHERE staff_id = $staffId
        AND team_id = $teamId
    """
      .query[StaffRole]
      .unique
      .transact(xa)

  override def insert(
    staffId:   Long,
    teamId:    Long,
    enrolment: SRSEnrollment
  ): F[Long] =
    sql"""
      INSERT INTO staff_role (
        staff_id,
        team_id,
        srs_role
      )
      VALUES (
        $staffId,
        $teamId,
        ${enrolment.role}
      )
    """.update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)

  override def delete(
    staffId: Long,
    teamId:  Long
  ): F[Int] =
    sql"""
      DELETE FROM staff_role
      WHERE staff_id = $staffId
        AND team_id = $teamId
    """.update.run
      .transact(xa)
