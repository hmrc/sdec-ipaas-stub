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

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.sdecipaasstub.dto.{Staff, StaffTeam, Team}
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.StaffDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecStaff, SdecTeam, StaffRole}
import uk.gov.hmrc.sdecipaasstub.model.SRSEnrollment
import uk.gov.hmrc.sdecipaasstub.service.algebra.{StaffRoleServiceAlgebra, TeamServiceAlgebra}

class StaffServiceSpec extends AnyWordSpec with Matchers {

  final class StubStaffDataAccess(
    existingStaff: Option[SdecStaff],
    insertedStaff: Ref[IO, List[Staff]]
  ) extends StaffDataAccessAlgebra[IO] {

    override def findByPID(pid: String): IO[Option[SdecStaff]] =
      IO.pure(existingStaff)

    override def insert(staff: Staff): IO[Long] =
      insertedStaff.update(_ :+ staff).as(100L)
  }

  final class StubTeamService(
    existingTeam:  Option[SdecTeam],
    insertedTeams: Ref[IO, List[(String, Boolean)]]
  ) extends TeamServiceAlgebra[IO] {

    override def getByTeamName(team: String): IO[Option[Team]] =
      IO.pure(existingTeam.map(entity => Team.fromEntity(entity)))

    override def getTeam(team: String): IO[Team] =
      IO.raiseError(UnsupportedOperationException("Not used"))

    override def insert(team: String, isTaskBased: Boolean): IO[Long] =
      insertedTeams.update(_ :+ (team, isTaskBased)).as(200L)
  }

  final class StubStaffRoleService(
    assignments: Ref[IO, List[(Long, Long, SRSEnrollment)]]
  ) extends StaffRoleServiceAlgebra[IO] {

    override def findByStaffAndTeam(staffId: Long, teamId: Long): IO[StaffRole] =
      IO.raiseError(UnsupportedOperationException("Not used"))

    override def insert(staffId: Long, teamId: Long, enrolment: SRSEnrollment): IO[Long] =
      assignments.update(_ :+ (staffId, teamId, enrolment)).as(1L)

    override def delete(staffId: Long, teamId: Long): IO[Long] =
      IO.pure(0)
  }

  "StaffService.upsert(staff)" should {

    "insert staff and team when neither exists" in {
      val staff = Staff(
        pid = "1001",
        name = "Test User",
        email = "tu@tu.gov.uk",
        srs = "sdec_vat_user"
      )

      val result = for {
        insertedStaff <- Ref.of[IO, List[Staff]](Nil)
        insertedTeams <- Ref.of[IO, List[(String, Boolean)]](Nil)
        assignments   <- Ref.of[IO, List[(Long, Long, SRSEnrollment)]](Nil)

        staffDataAccess =
          new StubStaffDataAccess(None, insertedStaff)

        teamService      = new StubTeamService(None, insertedTeams)
        staffRoleService = new StubStaffRoleService(assignments)

        service = new StaffService[IO](
                    staffDataAccess,
                    teamService,
                    staffRoleService
                  )

        enrollment = SRSEnrollment.extractFromEnrollment(staff.srs)

        response <- service.upsert(staff)

        actualStaff       <- insertedStaff.get
        actualTeams       <- insertedTeams.get
        actualAssignments <- assignments.get
      } yield (
        response,
        actualStaff,
        actualTeams,
        actualAssignments,
        enrollment
      )

      val (
        response,
        actualStaff,
        actualTeams,
        actualAssignments,
        enrollment
      ) = result.unsafeRunSync()

      response mustBe StaffTeam(100L, 200L)
      actualStaff mustBe List(staff)
      actualTeams mustBe List((enrollment.team, false))
      actualAssignments mustBe List((100L, 200L, enrollment))
    }
  }
}
