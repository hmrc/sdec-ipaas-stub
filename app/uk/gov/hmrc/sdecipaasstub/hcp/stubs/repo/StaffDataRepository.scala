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
import uk.gov.hmrc.sdecipaasstub.dto.Staff
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.StaffDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.SdecStaff

import javax.inject.Singleton

@Singleton
class StaffDataRepository[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends StaffDataAccessAlgebra[F]:

  override def findByPID(pid: String): F[Option[SdecStaff]] =
    sql"""
      SELECT
        id,
        pid,
        name,
        email
      FROM sdec_staff
      WHERE pid = $pid
    """
      .query[SdecStaff]
      .option
      .transact(xa)

  override def insert(staff: Staff): F[Long] =
    sql"""
        INSERT INTO sdec_staff (
          pid,
          name,
          email
        )
        VALUES (
          ${staff.pid},
          ${staff.name},
          ${staff.email}
        )
      """
      .update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)
