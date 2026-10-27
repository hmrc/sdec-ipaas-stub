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
import uk.gov.hmrc.sdecipaasstub.dto.{CreateThreadRequest, RecipientDetails, StaffDetails, Team, ThreadDetails, ThreadReference, ThreadResponse}
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.{RecipientDataAccessAlgebra, RecipientDetails, StaffDataAccessAlgebra, StaffDetails, Team, ThreadDataAccessAlgebra, ThreadDetails}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus.PENDING
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecRecipient, SdecStaff, SdecThread}
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.service.algebra.{StaffServiceAlgebra, TeamServiceAlgebra, ThreadServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.*

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.util.Random

class ThreadService[F[_]: Monad: Sync] @Inject() (
  threadDataAccess: ThreadDataAccessAlgebra[F],
  staffDataAccess: StaffDataAccessAlgebra[F],
  recipientDataAccess: RecipientDataAccessAlgebra[F],
  teamService:      TeamServiceAlgebra[F],
  staffService:     StaffServiceAlgebra[F]
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
                                        threadRef: String
                                      ): F[ThreadReference] =
    for {
      _ <- logger.info(
        s"Finding thread by reference: $threadRef"
      )

      threadOpt <- threadDataAccess.findByRef(threadRef)

      thread <- Sync[F].fromOption(
        threadOpt,
        new RuntimeException(
          s"Thread $threadRef not found"
        )
      )

      staffOpt <- staffDataAccess.findById(
        thread.createdBy
      )

      staff <- Sync[F].fromOption(
        staffOpt,
        new RuntimeException(
          s"Staff ${thread.createdBy} not found"
        )
      )

      recipientId <- Sync[F].fromOption(
        thread.recipientId,
        new RuntimeException(
          "Thread has no recipient"
        )
      )

      recipientOpt <- recipientDataAccess.findById(
        recipientId
      )

      recipient <- Sync[F].fromOption(
        recipientOpt,
        new RuntimeException(
          s"Recipient $recipientId not found"
        )
      )

    } yield convertToDto(
      thread,
      staff,
      recipient
    )

  private def convertToDto(
                            thread: SdecThread,
                            staff: SdecStaff,
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
      createdTimeStamp =
        thread.createdTimestamp.getOrElse(
          throw new RuntimeException("Missing created timestamp")
        ),
      lastUpdatedTimeStamp =
        thread.lastUpdatedTimestamp.getOrElse(
          throw new RuntimeException("Missing last updated timestamp")
        ),
      threadExpiryDate =
        thread.threadExpiryDate.getOrElse(
          throw new RuntimeException("Missing thread expiry data")
        ),
      associatedCaseReference =
        thread.caseReference.getOrElse(""),
      threadOwner = None,
      owningTeam = Team(
        id = thread.teamId,
        srsName = ""),
      recipientDetails = RecipientDetails(
        firstName = recipient.firstName,
        lastName = recipient.lastName,
        email = recipient.email,
        phoneNumber =
          recipient.phoneNumber.getOrElse("No phone number"),
        nationalInsuranceNumber = recipient.nino,
        hasRelatedCase =
          thread.caseReference.isDefined,
        caseReferenceNumber =
          thread.caseReference
      ),
      threadDetails = ThreadDetails(
        message = thread.message.getOrElse(""),
        responseDate =
          thread.requiredBy.getOrElse(thread.threadExpiryDate.get)
      )
    )
}

      recipientOpt <- recipientDataAccess.findById(
        recipientId
      )

      recipient <- Sync[F].fromOption(
        recipientOpt,
        new RuntimeException(
          s"Recipient $recipientId not found"
        )
      )

    } yield convertToDto(
      thread,
      staff,
      recipient
    )

  private def convertToDto(
                            thread: SdecThread,
                            staff: SdecStaff,
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
      createdTimeStamp =
        thread.createdTimestamp.getOrElse(
          throw new RuntimeException("Missing created timestamp")
        ),
      lastUpdatedTimeStamp =
        thread.lastUpdatedTimestamp.getOrElse(
          throw new RuntimeException("Missing last updated timestamp")
        ),
      threadExpiryDate =
        thread.threadExpiryDate.getOrElse(
          throw new RuntimeException("Missing thread expiry data")
        ),
      associatedCaseReference =
        thread.caseReference.getOrElse(""),
      threadOwner = None,
      owningTeam = Team(
        id = thread.teamId,
        srsName = ""),
      recipientDetails = RecipientDetails(
        firstName = recipient.firstName,
        lastName = recipient.lastName,
        email = recipient.email,
        phoneNumber =
          recipient.phoneNumber.getOrElse("No phone number"),
        nationalInsuranceNumber = recipient.nino,
        hasRelatedCase =
          thread.caseReference.isDefined,
        caseReferenceNumber =
          thread.caseReference
      ),
      threadDetails = ThreadDetails(
        message = thread.message.getOrElse(""),
        responseDate =
          thread.requiredBy.getOrElse(thread.threadExpiryDate.get)
      )
    )
}