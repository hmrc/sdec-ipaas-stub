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

import cats.effect.MonadCancelThrow
import cats.syntax.applicative.catsSyntaxApplicativeId
import doobie.*
import doobie.implicits.*
import doobie.implicits.javatimedrivernative.{JavaLocalDateMeta, JavaLocalDateTimeMeta}
import uk.gov.hmrc.sdecipaasstub.dto.ThreadUpdate
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.ThreadDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{SdecThread, ThreadStatus}

import java.time.LocalDateTime

class ThreadDataRepository[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends ThreadDataAccessAlgebra[F]:

  override def findByRef(threadRef: String): F[Option[SdecThread]] =
    sql"""SELECT * FROM sdec_thread
          WHERE reference = $threadRef
          """
      .query[SdecThread]
      .option
      .transact(xa)

  override def findIdByRef(threadRef: String): F[Option[Long]] =
    sql"""
          SELECT 
          id
          FROM sdec_thread
          WHERE reference = $threadRef
        """
      .query[Long]
      .option
      .transact(xa)

  override def updateThreadAndRecipientDetails(
    thread:    ThreadUpdate,
    threadRef: String,
    now:       LocalDateTime
  ): F[Boolean] = {

    val updateThreadQuery = sql"""
          UPDATE sdec_thread
          SET
          last_updated_timestamp = $now,
          status = ThreadStatus.ACTIVE,
          email = COALESCE(${thread.email}, email),
          nino = COALESCE(${thread.nino},nino),
          case_reference = COALESCE(${thread.caseReference}, case_reference),
          message = COALESCE(${thread.message}, message),
          required_by = COALESCE(${thread.requestedBy.asLocalDate}, required_by)
          WHERE reference = $threadRef
          RETURNING recipient_id
      """
      .query[Long]
      .option

    def updateRecipientQuery(recipientId: Long) = sql"""
           UPDATE sdec_recipient
           SET
           first_name = COALESCE(${thread.firstName}, first_name),
           last_name = COALESCE(${thread.surname}, last_name),
           email = COALESCE(${thread.email}, email),
           phone_number = COALESCE(${thread.mobile}, phone_number),
           nino = COALESCE(${thread.nino}, nino)
           WHERE id = $recipientId
           RETURNING id
       """.query[Long].option

    val result = for {
      maybeRecipientId <- updateThreadQuery
      success          <- maybeRecipientId match {
                   case Some(recipientId) =>
                     updateRecipientQuery(recipientId).map {
                       case Some(_) => true
                       case None    => false
                     }
                   case None => false.pure[ConnectionIO]
                 }
    } yield success

    result.transact(xa)
  }

  override def findByStaffIdAndTeamId(staffId: Long, teamId: Long): F[List[SdecThread]] =
    sql"""
              SELECT
              id,
              reference,
              status,
              created_by,
              created_timestamp,
              last_updated_timestamp,
              thread_expiry_date,
              case_reference,
              recipient_id,
              email,
              name,
              nino,
              message,
              required_by,
              team_id
              FROM sdec_thread
              WHERE created_by = $staffId
              AND team_id = $teamId
              """
      .query[SdecThread]
      .to[List]
      .transact(xa)

  override def insert(
    reference: String,
    status:    ThreadStatus,
    createdBy: Long,
    teamId:    Long,
    now:       LocalDateTime
  ): F[Long] =
    sql"""
      INSERT INTO sdec_thread (reference, status, created_by, created_timestamp, last_updated_timestamp, team_id)
      VALUES ($reference, $status, $createdBy, $now, $now, $teamId)
    """.update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(xa)
