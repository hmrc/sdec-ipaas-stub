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

import cats.Monad
import cats.effect.Sync
import cats.syntax.all.*
import uk.gov.hmrc.sdecipaasstub.dto.{Staff, StaffTeam}
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.{StaffDataAccessAlgebra, StaffRoleDataAccessAlgebra, TeamDataAccessAlgebra}
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.model.SRSEnrollment
import uk.gov.hmrc.sdecipaasstub.service.algebra.StaffServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.errors.StaffInsertError

import javax.inject.Inject

class StaffService[F[_]: Monad: Sync] @Inject() (
  staffDataAccess:     StaffDataAccessAlgebra[F],
  teamDataAccess:      TeamDataAccessAlgebra[F],
  staffRoleDataAccess: StaffRoleDataAccessAlgebra[F]
) extends StaffServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def upsert(staff: Staff): F[StaffTeam] = {
    val enrolment = SRSEnrollment.extractFromEnrollment(staff.srs)
    for {
      _      <- logger.info(s"Upsert: $staff")
      staffF <- staffDataAccess.findByPID(staff.pid)
      staffId = staffF match {
                  case Some(s) => s.id
                  case None    => 0L
                }
      team <- teamDataAccess.getByTeamName(enrolment.team)
      teamId = team match {
                 case Some(t) => t.id
                 case None    => 0L
               }
      staffTeam <- checkAndInsert(staffId, teamId, staff, enrolment)
    } yield staffTeam
  }.recoverWith { e =>
    logger.warn(s"Staff/Team Upsert failed with ${e.getMessage}")
    throw new StaffInsertError(e.getMessage)
  }

  private def checkAndInsert(staffId: Long, teamId: Long, staff: Staff, enrollment: SRSEnrollment): F[StaffTeam] =
    (staffId, teamId) match {
      case (sid, tid) if sid > 0 && tid > 0 =>
        StaffTeam(sid, tid).pure[F]
      case (0, tid) if tid > 0 =>
        saveStaffAndAssign(staff, tid, enrollment)
      case (sid, 0) if sid > 0 =>
        saveTeamAndAssign(sid, enrollment)
      case (_, _) =>
        saveStaffAndTeam(staff, enrollment)
    }

  private def saveStaffAndAssign(staff: Staff, teamId: Long, enrollment: SRSEnrollment): F[StaffTeam] =
    for {
      staffId <- staffDataAccess.insert(staff)
      _       <- staffRoleDataAccess.insert(staffId, teamId, enrollment)
    } yield StaffTeam(staffId, teamId)

  private def saveTeamAndAssign(staffId: Long, enrollment: SRSEnrollment): F[StaffTeam] =
    for {
      teamId <- teamDataAccess.insert(enrollment.team)
      _      <- staffRoleDataAccess.insert(staffId, teamId, enrollment)
    } yield StaffTeam(staffId, teamId)

  private def saveStaffAndTeam(staff: Staff, enrollment: SRSEnrollment): F[StaffTeam] =
    for {
      teamId  <- teamDataAccess.insert(enrollment.team)
      staffId <- staffDataAccess.insert(staff)
      _       <- staffRoleDataAccess.insert(staffId, teamId, enrollment)
    } yield StaffTeam(staffId, teamId)
}
