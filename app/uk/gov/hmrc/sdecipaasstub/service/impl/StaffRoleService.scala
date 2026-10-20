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
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.StaffRoleDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.StaffRole
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.model.SRSEnrollment
import uk.gov.hmrc.sdecipaasstub.service.algebra.StaffRoleServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.errors.StaffRoleInsertError

import javax.inject.Inject

class StaffRoleService[F[_]: Sync] @Inject() (
  staffRoleDataAccess: StaffRoleDataAccessAlgebra[F]
) extends StaffRoleServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def findByStaffAndTeam(staffId: Long, teamId: Long): F[StaffRole] = ???

  override def insert(staffId: Long, teamId: Long, enrolment: SRSEnrollment): F[Long] = {
    for {
      _           <- logger.info(s"StaffRole Insert: StaffId: $staffId, TeamId: $teamId, Enrolment: $SRSEnrollment")
      staffRoleId <- staffRoleDataAccess.insert(staffId, teamId, enrolment)
    } yield staffRoleId
  }.recoverWith { e =>
    logger.warn(s"StaffRole Insert failed with ${e.getMessage}")
    throw new StaffRoleInsertError(e.getMessage)
  }

  override def delete(staffId: Long, teamId: Long): F[Long] = ???

}
