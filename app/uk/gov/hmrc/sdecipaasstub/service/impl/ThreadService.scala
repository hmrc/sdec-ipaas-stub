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
import uk.gov.hmrc.sdecipaasstub.dto
import uk.gov.hmrc.sdecipaasstub.dto.{CreateThreadRequest, ThreadResponse, ThreadSummary, ThreadUpdate}
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.ThreadDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.hcp.entity.ThreadStatus.PENDING
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.service.algebra.{StaffServiceAlgebra, TeamServiceAlgebra, ThreadServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.*

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.util.Random

class ThreadService[F[_]: Sync] @Inject() (
  threadDataAccess: ThreadDataAccessAlgebra[F],
  teamService:      TeamServiceAlgebra[F],
  staffService:     StaffServiceAlgebra[F]
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

  override def updateThreadAndRecipient(threadUpdate: ThreadUpdate, threadRef: String): F[ThreadUpdate] =
    for {
      _         <- logger.info(s"Update: $threadUpdate")
      now       <- Sync[F].realTimeInstant.map(LocalDateTime.ofInstant(_, ZoneOffset.UTC))
      isUpdated <- threadDataAccess.updateThreadAndRecipientDetails(threadUpdate, threadRef, now)
      result    <-
        if isUpdated then { Sync[F].pure(threadUpdate) }
        else {
          Sync[F].raiseError[ThreadUpdate](
            new ThreadUpdateError(s"failed to update thread details: $threadRef")
          )
        }
    } yield result

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
}
