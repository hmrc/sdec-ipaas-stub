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
import cats.effect.kernel.Sync
import cats.syntax.all.*
import uk.gov.hmrc.sdecipaasstub.dto.*
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.ThreadDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus.PENDING
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecRecipient, SdecStaff, SdecThread}
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, JwtUser}
import uk.gov.hmrc.sdecipaasstub.service.algebra.{RecipientServiceAlgebra, StaffServiceAlgebra, TeamServiceAlgebra, ThreadServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.*

import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.util.Random

class ThreadService[F[_]: Sync] @Inject() (
  threadDataAccess: ThreadDataAccessAlgebra[F],
  teamService:      TeamServiceAlgebra[F],
  staffService:     StaffServiceAlgebra[F],
  recipientService: RecipientServiceAlgebra[F]
) extends ThreadServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def insert(request: CreateThreadRequest): F[ThreadResponse] =
    for {
      _       <- logger.info(s"Insert: $request")
      staff   <- staffService.findByPid(request.pid)
      staffId <- staff match
                   case Some(value) => value.id.pure[F]
                   case None        =>
                     logger.warn(s"Could not find Staff with PID ${request.pid}") *>
                       Sync[F].raiseError[Long](new StaffFindError(s"Could not find Staff with PID ${request.pid}"))
      team      <- teamService.getTeam(request.team)
      reference <- generateReference
      now       <- Sync[F].realTimeInstant.map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))
      _         <- threadDataAccess
             .insert(reference, PENDING, staffId, team.id, now)
             .adaptError { case e => ThreadInsertError(e.getMessage) }
    } yield ThreadResponse(reference)

  override def findIdByRef(threadRef: String): F[Option[Long]] =
    threadDataAccess.findIdByRef(threadRef)

  override def findThreadsForWorkspace(pid: String, teamName: String): F[List[ThreadSummary]] =
    for {
      _    <- logger.info(s"Getting workspace threads for user $pid - $teamName")
      team <- teamService.getByTeamName(teamName)
      teamId: Long <- team match {
                        case Some(value) => value.id.pure[F]
                        case None        =>
                          logger.warn(s"Team Name does not exist in database: $teamName") *>
                            Sync[F].raiseError[Long](
                              new TeamLookupError(s"Team Name does not exist in database: $teamName")
                            )
                      }
      staff <- staffService.findByPid(pid)
      staffId: Long <- staff match {
                         case Some(value) => value.id.pure[F]
                         case None        =>
                           logger.warn(s"Staff not found for $pid") *>
                             Sync[F].raiseError[Long](StaffFindError(s"Staff with PID $pid not found"))
                       }
      threads <- threadDataAccess.findByStaffIdAndTeamId(staffId, teamId)
      _       <- logger.info(s"Found following threads in database: $threads")
    } yield threads.map(ThreadSummary.fromEntity)

  private def generateReference: F[String] =
    Sync[F].delay {
      val letters = List.fill(4)(('A' + Random.nextInt(26)).toChar).mkString
      val digits  = List.fill(5)(Random.nextInt(10)).mkString
      letters + digits
    }

  override def getThreadInfoByThreadId(
    threadRef: String,
    user:      JwtUser
  ): F[ThreadReference] =
    for {
      _ <- logger.info(
             s"Finding thread by reference: $threadRef"
           )

      thread <- findThread(threadRef)

      staff <- staffService.findStaff(thread.createdBy)

      recipientId <- Sync[F].fromOption(
                       thread.recipientId,
                       ThreadRecipientMissingError("Thread has no recipient")
                     )

      recipient <- recipientService.findRecipient(recipientId)

      createdTimestamp <- requireField(thread.createdTimestamp, "created timestamp")

      lastUpdatedTimestamp <- requireField(thread.lastUpdatedTimestamp, "last updated timestamp")

      threadExpiryDate <- requireField(thread.threadExpiryDate, "thread expiry date")

      _ <- validateUser(recipient, user)

    } yield convertToDto(
      thread,
      staff,
      recipient,
      createdTimestamp,
      lastUpdatedTimestamp,
      threadExpiryDate
    )

  private def findThread(
    threadRef: String
  ): F[SdecThread] =
    threadDataAccess
      .findByRef(threadRef)
      .flatMap(
        Sync[F].fromOption(
          _,
          ThreadNotFoundError(s"Thread $threadRef not found")
        )
      )

  private def validateUser(
    recipient: SdecRecipient,
    user:      JwtUser
  ): F[Unit] =
    if recipient.nino == user.nino then {
      Sync[F].unit
    } else {
      Sync[F].raiseError(
        ThreadAccessDeniedError("User details do not match recipient")
      )
    }

  private def requireField[A](
    value:     Option[A],
    fieldName: String
  ): F[A] =
    Sync[F].fromOption(
      value,
      ThreadFieldMissingError(s"Missing $fieldName")
    )

  private def convertToDto(
    thread:               SdecThread,
    staff:                SdecStaff,
    recipient:            SdecRecipient,
    createdTimestamp:     LocalDateTime,
    lastUpdatedTimestamp: LocalDateTime,
    threadExpiryDate:     LocalDate
  ): ThreadReference =
    ThreadReference(
      id = thread.reference,
      status = thread.status,
      createdBy = StaffDetails(
        id = staff.id,
        pid = staff.pid,
        name = staff.name
      ),
      createdTimeStamp = createdTimestamp,
      lastUpdatedTimeStamp = lastUpdatedTimestamp,
      threadExpiryDate = threadExpiryDate,
      associatedCaseReference = thread.caseReference.getOrElse(""),
      threadOwner = None,
      owningTeam = Team(id = thread.teamId, srsName = ""),
      recipientDetails = RecipientDetails(
        firstName = recipient.firstName,
        lastName = recipient.lastName,
        email = recipient.email,
        phoneNumber = recipient.phoneNumber.getOrElse("No phone number"),
        nationalInsuranceNumber = recipient.nino,
        hasRelatedCase = thread.caseReference.isDefined,
        caseReferenceNumber = thread.caseReference
      ),
      threadDetails = ThreadDetails(
        message = thread.message.getOrElse(""),
        responseDate = thread.requiredBy.getOrElse(threadExpiryDate)
      )
    )
}
