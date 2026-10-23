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
import cats.effect.kernel.Sync
import cats.syntax.all.*
import uk.gov.hmrc.sdecipaasstub.dto.{CreateThreadRequest, ThreadResponse}
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.{StaffDataAccessAlgebra, TeamDataAccessAlgebra, ThreadDataAccessAlgebra}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus.DRAFT
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.service.algebra.ThreadServiceAlgebra
import uk.gov.hmrc.sdecipaasstub.service.errors.*

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.util.Random

class ThreadService[F[_]: Monad: Sync] @Inject() (
  threadDataAccess: ThreadDataAccessAlgebra[F],
  teamDataAccess:   TeamDataAccessAlgebra[F],
  staffDataAccess:  StaffDataAccessAlgebra[F]
) extends ThreadServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def upsert(request: CreateThreadRequest): F[ThreadResponse] =
    for {
      _ <- logger.info(s"Upsert: $request")

      staff <- staffDataAccess
                 .findByPID(request.pid)
                 .flatMap(_.liftTo[F](StaffLookupError(s"No staff found for pid ${request.pid}")))

      team <- teamDataAccess
                .getByTeamName(request.team)
                .flatMap(_.liftTo[F](TeamLookupError(s"No team found with name ${request.team}")))

      reference <- generateReference

      now <- Sync[F].realTimeInstant.map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))

      _ <- threadDataAccess
             .insert(reference, DRAFT, staff.id, team.id, now)
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
}
