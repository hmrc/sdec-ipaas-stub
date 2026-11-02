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
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{NewSdecRecipient, SdecRecipient, SdecStaff, SdecThread, ThreadStatus}
import uk.gov.hmrc.sdecipaasstub.infrastructure.JwtUser
import uk.gov.hmrc.sdecipaasstub.service.algebra.{RecipientServiceAlgebra, StaffServiceAlgebra, TeamServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.{StaffFindError, TeamLookupError, ThreadInsertError}

import java.time.{LocalDate, LocalDateTime}

class ThreadServiceSpec extends AnyWordSpec with Matchers {

  final case class InsertedThread(reference: String, status: ThreadStatus, createdBy: Long, teamId: Long)

  final class StubThreadDataAccess(
    thread:      Option[SdecThread],
    inserted:    Ref[SyncIO, List[InsertedThread]],
    insertError: Option[Throwable]
  ) extends ThreadDataAccessAlgebra[SyncIO] {

    override def findByRef(
      threadRef: String
    ): SyncIO[Option[SdecThread]] =
      SyncIO.pure(thread.filter(_.reference == threadRef))

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

    override def findStaff(staffId: Long): SyncIO[SdecStaff] =
      existingStaff match {
        case Some(found) => SyncIO.pure(found)
        case None        =>
          SyncIO.raiseError(
            StaffFindError(s"Staff with ID $staffId not found")
          )
      }
  }

  final class StubRecipientService(
    recipient:         Option[SdecRecipient],
    createdRecipients: Ref[SyncIO, List[(Long, NewSdecRecipient)]]
  ) extends RecipientServiceAlgebra[SyncIO] {

    override def findRecipient(
      recipientId: Long
    ): SyncIO[Option[SdecRecipient]] =
      SyncIO.pure(recipient)

    override def createAndAssignToThread(
      threadId:     Long,
      newRecipient: NewSdecRecipient
    ): SyncIO[SdecRecipient] =
      createdRecipients
        .update(_ :+ (threadId -> newRecipient))
        .as(
          SdecRecipient(
            id = 99L,
            internalId = newRecipient.internalId,
            firstName = Some(newRecipient.firstName),
            lastName = Some(newRecipient.lastName),
            email = Some(newRecipient.email),
            phoneNumber = newRecipient.phoneNumber,
            nino = newRecipient.nino
          )
        )
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

  private val jwtUser =
    JwtUser(
      internalId = Some("12345"),
      firstName = Some("John"),
      lastName = Some("Smith"),
      email = Some("john.smith@example.com"),
      phoneNumber = None,
      nino = "WM111111D",
      confidenceLevel = 250
    )

  private val recipient =
    SdecRecipient(
      id = 1,
      internalId = Some("12345"),
      firstName = Some("John"),
      lastName = Some("Smith"),
      email = Some("user@test.com"),
      phoneNumber = None,
      nino = "WM111111D"
    )

  private val thread =
    SdecThread(
      id = 1,
      reference = "THREAD1000AA",
      status = ThreadStatus.ACTIVE,
      createdBy = 100L,
      createdTimestamp = Some(LocalDateTime.now),
      lastUpdatedTimestamp = Some(LocalDateTime.now),
      threadExpiryDate = Some(LocalDate.now.plusMonths(1)),
      caseReference = Some("CASE-100001"),
      recipientId = Some(1L),
      email = "Email",
      name = "Name",
      nino = None,
      message = Some("Where are the files?"),
      requiredBy = Some(LocalDate.now.plusMonths(1)),
      teamId = 1L
    )

  private def service(
    existingStaff:     Option[SdecStaff],
    existingRecipient: Option[SdecRecipient],
    existingTeam:      Option[Team],
    inserted:          Ref[SyncIO, List[InsertedThread]],
    createdRecipients: Ref[SyncIO, List[(Long, NewSdecRecipient)]],
    existingThread:    Option[SdecThread] = Some(thread),
    insertError:       Option[Throwable] = None
  ) =
    new ThreadService[SyncIO](
      new StubThreadDataAccess(existingThread, inserted, insertError),
      new StubTeamService(existingTeam),
      new StubStaffService(existingStaff),
      new StubRecipientService(existingRecipient, createdRecipients)
    )

  "insert" should {

    "insert a thread and return its reference" in {
      val (response, actualInserts) = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        response <- service(Some(staff), Some(recipient), Some(team), inserted, created).insert(request)
        actual   <- inserted.get
      } yield (response, actual)).unsafeRunSync()

      response.threadReference must fullyMatch regex "[A-Z]{4}[0-9]{5}"
      actualInserts mustBe List(InsertedThread(response.threadReference, ThreadStatus.PENDING, 100L, 200L))
    }

    "fail with StaffLookupError and insert nothing when staff is not found" in {
      val (outcome, actualInserts) = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        outcome  <- service(None, Some(recipient), Some(team), inserted, created).insert(request).attempt
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
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        outcome  <- service(Some(staff), Some(recipient), None, inserted, created).insert(request).attempt
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
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        outcome  <- service(
                     Some(staff),
                     Some(recipient),
                     Some(team),
                     inserted,
                     created,
                     insertError = Some(new RuntimeException("boom"))
                   )
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
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        id       <- service(Some(staff), Some(recipient), Some(team), inserted, created).findIdByRef("ABCD12345")
      } yield id).unsafeRunSync()

      result mustBe Some(1L)
    }
  }

  "getThreadInfoByThreadId" should {

    "return thread information when NINO matches" in {
      val result = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        result   <- service(
                    Some(staff),
                    Some(recipient),
                    Some(team),
                    inserted,
                    created
                  ).getThreadInfoByThreadId("THREAD1000AA", jwtUser)
      } yield result).unsafeRunSync()

      result.id mustBe "THREAD1000AA"
    }

    "fail when JWT NINO does not match recipient NINO" in {

      val invalidJwtUser = jwtUser.copy(nino = "AB123456A")

      val outcome = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        result   <- service(
                    Some(staff),
                    Some(recipient),
                    Some(team),
                    inserted,
                    created
                  )
                    .getThreadInfoByThreadId(
                      "THREAD1000AA",
                      invalidJwtUser
                    )
                    .attempt

      } yield result).unsafeRunSync()

      outcome match {
        case Left(error) =>
          error.getMessage mustBe
            "User details do not match recipient"

        case Right(value) =>
          fail(
            s"Expected failure but got $value"
          )
      }
    }

    "fail when thread cannot be found" in {

      val outcome = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)
        result   <- service(
                    Some(staff),
                    Some(recipient),
                    Some(team),
                    inserted,
                    created
                  )
                    .getThreadInfoByThreadId(
                      "DOES_NOT_EXIST",
                      jwtUser
                    )
                    .attempt

      } yield result).unsafeRunSync()

      outcome match {
        case Left(error) =>
          error.getMessage must include(
            "Thread DOES_NOT_EXIST not found"
          )

        case Right(value) =>
          fail(
            s"Expected failure but got $value"
          )
      }
    }

    "reuse the existing recipient when internal IDs match" in {
      val result = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)

        response <- service(
                      Some(staff),
                      Some(recipient),
                      Some(team),
                      inserted,
                      created
                    ).getThreadInfoByThreadId("THREAD1000AA", jwtUser)

        actualCreated <- created.get
      } yield (response, actualCreated)).unsafeRunSync()

      result._1.recipientDetails.email mustBe "user@test.com"
      result._2 mustBe Nil
    }

    "create a new recipient when internal IDs differ" in {
      val differentInternalIdUser =
        jwtUser.copy(internalId = Some("INT999999"))

      val result = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)

        response <- service(
                      Some(staff),
                      Some(recipient),
                      Some(team),
                      inserted,
                      created
                    ).getThreadInfoByThreadId("THREAD1000AA", differentInternalIdUser)

        actualCreated <- created.get
      } yield (response, actualCreated)).unsafeRunSync()

      result._1.recipientDetails.email mustBe "john.smith@example.com"

      result._2 must have size 1
      result._2.head._1 mustBe thread.id
      result._2.head._2.internalId mustBe Some("INT999999")
      result._2.head._2.email mustBe "john.smith@example.com"
    }

    "create and assign a recipient when the thread has no recipient" in {
      val threadWithoutRecipient =
        thread.copy(
          recipientId = None,
          nino = Some(jwtUser.nino)
        )

      val result = (for {
        inserted <- Ref.of[SyncIO, List[InsertedThread]](Nil)
        created  <- Ref.of[SyncIO, List[(Long, NewSdecRecipient)]](Nil)

        response <- service(
                      Some(staff),
                      None,
                      Some(team),
                      inserted,
                      created,
                      existingThread = Some(threadWithoutRecipient)
                    ).getThreadInfoByThreadId("THREAD1000AA", jwtUser)

        actualCreated <- created.get
      } yield (response, actualCreated)).unsafeRunSync()

      result._1.recipientDetails.email mustBe "john.smith@example.com"

      result._2 must have size 1
      result._2.head._1 mustBe thread.id
      result._2.head._2.nino mustBe jwtUser.nino
    }
  }
}
