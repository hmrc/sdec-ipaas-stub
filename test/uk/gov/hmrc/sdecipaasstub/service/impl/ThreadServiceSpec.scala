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

import cats.effect.{Ref, SyncIO}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.sdecipaasstub.dto.*
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.ThreadDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecStaff, SdecThread, ThreadStatus}
import uk.gov.hmrc.sdecipaasstub.service.algebra.{StaffServiceAlgebra, TeamServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.{StaffFindError, TeamLookupError, ThreadInsertError}

import java.time.LocalDateTime

class ThreadServiceSpec extends AnyWordSpec with Matchers {

  final case class InsertedThread(reference: String, status: ThreadStatus, createdBy: Long, teamId: Long)

  final class StubThreadDataAccess(
    inserted:    Ref[SyncIO, List[InsertedThread]],
    insertError: Option[Throwable]
  ) extends ThreadDataAccessAlgebra[SyncIO] {

    override def findByRef(threadRef: String): SyncIO[Option[SdecThread]] =
      SyncIO.pure(None)

    override def findIdByRef(threadRef: String): SyncIO[Option[Long]] =
      SyncIO.pure(Some(1L))

    override def insert(
      reference: String,
      status:    ThreadStatus,
      createdBy: Long,
      teamId:    Long,
      now:       LocalDateTime
    ): SyncIO[Long] =
      insertError match {
        case Some(error) => SyncIO.raiseError(error)
        case None        => inserted.update(_ :+ InsertedThread(reference, status, createdBy, teamId)).as(1L)
      }

    override def findByStaffIdAndTeamId(staffId: Long, teamId: Long): SyncIO[List[SdecThread]] = ???
  }

  final class StubStaffService(existingStaff: Option[SdecStaff]) extends StaffServiceAlgebra[SyncIO] {

    override def upsert(staff: Staff): SyncIO[StaffTeam] =
      SyncIO.raiseError(UnsupportedOperationException("Not used"))

    override def upsert(staffUpdate: StaffUpdate): SyncIO[StaffTeams] =
      SyncIO.raiseError(UnsupportedOperationException("Not used"))

    override def findByPid(pid: String): SyncIO[Option[SdecStaff]] =
      SyncIO.pure(existingStaff)
  }

  final class StubTeamService(existingTeam: Option[Team]) extends TeamServiceAlgebra[SyncIO] {

    override def getByTeamName(team: String): SyncIO[Option[Team]] =
      SyncIO.pure(existingTeam)

    override def getTeam(team: String): SyncIO[Team] =
      existingTeam match {
        case Some(found) => SyncIO.pure(found)
        case None        => SyncIO.raiseError(TeamLookupError(s"No team found with name $team"))
      }

    override def insert(team: String, isTaskBased: Boolean): SyncIO[Long] =
      SyncIO.raiseError(UnsupportedOperationException("Not used"))
  }

  private val request = CreateThreadRequest(pid = "1001", team = "Child_Benefit")
  private val staff   = SdecStaff(100L, "1001", "Test User", None)
  private val team    = Team(200L, "Child_Benefit")

  private def service(
    existingStaff: Option[SdecStaff],
    existingTeam:  Option[Team],
    inserted:      Ref[SyncIO, List[InsertedThread]],
    insertError:   Option[Throwable] = None
  ) =
    new ThreadService[SyncIO](
      new StubThreadDataAccess(inserted, insertError),
      new StubTeamService(existingTeam),
      new StubStaffService(existingStaff)
    )

  "insert" should {

    "insert a thread and return its reference" in {
      val (response, actualInserts) = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        response <- service(Some(staff), Some(team), inserted).insert(request)
        actual   <- inserted.get
      } yield (response, actual)).unsafeRunSync()

      response.threadReference must fullyMatch regex "[A-Z]{4}[0-9]{5}"
      actualInserts mustBe List(InsertedThread(response.threadReference, ThreadStatus.PENDING, 100L, 200L))
    }

    "fail with StaffLookupError and insert nothing when staff is not found" in {
      val (outcome, actualInserts) = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        outcome  <- service(None, Some(team), inserted).insert(request).attempt
        actual   <- inserted.get
      } yield (outcome, actual)).unsafeRunSync()

      outcome match {
        case Left(_: StaffFindError) => succeed
        case other                   => fail(s"Expected StaffLookupError but got $other")
      }
      actualInserts mustBe Nil
    }

    "fail with TeamLookupError and insert nothing when team is not found" in {
      val (outcome, actualInserts) = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        outcome  <- service(Some(staff), None, inserted).insert(request).attempt
        actual   <- inserted.get
      } yield (outcome, actual)).unsafeRunSync()

      outcome match {
        case Left(_: TeamLookupError) => succeed
        case other                    => fail(s"Expected TeamLookupError but got $other")
      }
      actualInserts mustBe Nil
    }

    "wrap database failures in ThreadInsertError" in {
      val outcome = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        outcome  <- service(Some(staff), Some(team), inserted, Some(new RuntimeException("boom")))
                     .insert(request)
                     .attempt
      } yield outcome).unsafeRunSync()

      outcome match {
        case Left(error: ThreadInsertError) => error.getMessage mustBe "boom"
        case other                          => fail(s"Expected ThreadInsertError but got $other")
      }
    }
  }

  "findIdByRef" should {

    "return the id from the data access layer" in {
      val result = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        id       <- service(Some(staff), Some(team), inserted).findIdByRef("ABCD12345")
      } yield id).unsafeRunSync()

      result mustBe Some(1L)
    }
  }
}
