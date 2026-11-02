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
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.RecipientDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.{NewSdecRecipient, SdecRecipient}

import javax.inject.Singleton

@Singleton
class RecipientDataRepository[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends RecipientDataAccessAlgebra[F]:
  override def findById(id: Long): F[Option[SdecRecipient]] =
    sql"""
        SELECT
          id,
          internal_id,
          first_name,
          last_name,
          email,
          phone_number,
          nino
        FROM sdec_recipient
        WHERE id = $id
      """
      .query[SdecRecipient]
      .option
      .transact(xa)

  override def createAndAssignToThread(
    threadId:  Long,
    recipient: NewSdecRecipient
  ): F[SdecRecipient] = {

    val insertRecipient: ConnectionIO[Long] =
      sql"""
        INSERT INTO sdec_recipient (
          internal_id,
          first_name,
          last_name,
          email,
          phone_number,
          nino
        )
        VALUES (
          ${recipient.internalId},
          ${recipient.firstName},
          ${recipient.lastName},
          ${recipient.email},
          ${recipient.phoneNumber},
          ${recipient.nino}
        )
      """.update
        .withUniqueGeneratedKeys[Long]("id")

    val assignRecipient: Long => ConnectionIO[Int] =
      recipientId => sql"""
          UPDATE sdec_thread
          SET recipient_id = $recipientId
          WHERE id = $threadId
        """.update.run

    val findInsertedRecipient: Long => ConnectionIO[SdecRecipient] =
      recipientId => sql"""
          SELECT
            id,
            internal_id,
            first_name,
            last_name,
            email,
            phone_number,
            nino
          FROM sdec_recipient
          WHERE id = $recipientId
        """.query[SdecRecipient].unique

    val transaction: ConnectionIO[SdecRecipient] =
      for {
        recipientId <- insertRecipient
        updatedRows <- assignRecipient(recipientId)
        _           <-
          if updatedRows == 1 then ().pure[ConnectionIO]
          else
            new RuntimeException(
              s"Could not assign recipient to thread $threadId"
            ).raiseError[ConnectionIO, Unit]
        inserted <- findInsertedRecipient(recipientId)
      } yield inserted

    transaction.transact(xa)
  }
