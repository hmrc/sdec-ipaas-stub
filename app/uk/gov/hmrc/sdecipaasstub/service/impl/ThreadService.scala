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
import cats.effect.kernel.Sync
import cats.syntax.all.*
import uk.gov.hmrc.sdecipaasstub.dto.*
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.{RecipientDataAccessAlgebra, StaffDataAccessAlgebra, ThreadDataAccessAlgebra}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus.PENDING
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecRecipient, SdecStaff, SdecThread}
import uk.gov.hmrc.sdecipaasstub.infrastructure.{ApplicationLogger, JwtUser}
import uk.gov.hmrc.sdecipaasstub.service.algebra.{StaffServiceAlgebra, TeamServiceAlgebra, ThreadServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.*

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.util.Random

class ThreadService[F[_]: Monad: Sync] @Inject() (
  threadDataAccess:    ThreadDataAccessAlgebra[F],
  staffDataAccess:     StaffDataAccessAlgebra[F],
  recipientDataAccess: RecipientDataAccessAlgebra[F],
  teamService:         TeamServiceAlgebra[F],
  staffService:        StaffServiceAlgebra[F]
) extends ThreadServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def insert(request: CreateThreadRequest): F[ThreadResponse] =
    for {
      _ <- logger.info(s"Insert: $request")

      staff     <- staffService.findByPid(request.pid)
      team      <- teamService.getTeam(request.team)
      reference <- generateReference
      now       <- Sync[F].realTimeInstant.map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))

      _ <- threadDataAccess
             .insert(reference, PENDING, staff.id, team.id, now)
             .adaptError { case e => ThreadInsertError(e.getMessage) }
    } yield ThreadResponse(reference)

  override def findIdByRef(threadRef: String): F[Option[Long]] =
    threadDataAccess.findIdByRef(threadRef)

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

      staff <- findStaff(
                 thread.createdBy
               )

      recipientId <- Sync[F].fromOption(
                       thread.recipientId,
                       new RuntimeException(
                         "Thread has no recipient"
                       )
                     )

      recipient <- findRecipient(
                     recipientId
                   )

      _ <- validateUser(
             recipient,
             user
           )

    } yield convertToDto(
      thread,
      staff,
      recipient
    )

  private def findThread(
    threadRef: String
  ): F[SdecThread] =
    threadDataAccess
      .findByRef(threadRef)
      .flatMap(
        Sync[F].fromOption(
          _,
          new RuntimeException(
            s"Thread $threadRef not found"
          )
        )
      )

  private def findStaff(
    staffId: Long
  ): F[SdecStaff] =
    staffDataAccess
      .findById(staffId)
      .flatMap(
        Sync[F].fromOption(
          _,
          new RuntimeException(
            s"Staff $staffId not found"
          )
        )
      )

  private def findRecipient(
    recipientId: Long
  ): F[SdecRecipient] =
    recipientDataAccess
      .findById(recipientId)
      .flatMap(
        Sync[F].fromOption(
          _,
          new RuntimeException(
            s"Recipient $recipientId not found"
          )
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
        new RuntimeException(
          "User details do not match recipient"
        )
      )
    }

  private def convertToDto(
    thread:    SdecThread,
    staff:     SdecStaff,
    recipient: SdecRecipient
  ): ThreadReference =
    ThreadReference(
      id = thread.reference,
      status = thread.status,
      createdBy = StaffDetails(
        id = staff.id,
        pid = staff.pid,
        name = staff.name
      ),
      createdTimeStamp = thread.createdTimestamp.getOrElse(
        throw new RuntimeException("Missing created timestamp")
      ),
      lastUpdatedTimeStamp = thread.lastUpdatedTimestamp.getOrElse(
        throw new RuntimeException("Missing last updated timestamp")
      ),
      threadExpiryDate = thread.threadExpiryDate.getOrElse(
        throw new RuntimeException("Missing thread expiry data")
      ),
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
        responseDate = thread.requiredBy.getOrElse(thread.threadExpiryDate.get)
      )
    )
}
