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
import cats.implicits.*
import uk.gov.hmrc.sdecipaasstub.dto
import uk.gov.hmrc.sdecipaasstub.dto.ThreadSummary
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.ThreadDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.service.algebra.{StaffServiceAlgebra, TeamServiceAlgebra, ThreadServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.{StaffFindError, TeamFindError}

import javax.inject.Inject

class ThreadService[F[_]: Sync] @Inject() (
  threadDataAccess: ThreadDataAccessAlgebra[F],
  teamService:      TeamServiceAlgebra[F],
  staffService:     StaffServiceAlgebra[F]
) extends ThreadServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def saveThread(thread: dto.Thread): F[dto.Thread] = ???

  override def findIdByRef(threadRef: String): F[Option[Long]] =
    for {
      _        <- logger.info(s"Finding ID by the threadRef: $threadRef")
      threadId <- threadDataAccess.findIdByRef(threadRef)
    } yield threadId

  override def findThreadsForWorkspace(pid: String, teamName: String): F[List[ThreadSummary]] =
    for {
      _    <- logger.info(s"Getting workspace threads for user $pid - $teamName")
      team <- teamService.getByTeamName(teamName)
      teamId: Long <- team match {
                        case Some(value) => value.id.pure[F]
                        case None        =>
                          logger.warn(s"Team Name does not exist in database: $teamName") *>
                            Sync[F].raiseError[Long](new TeamFindError(s""))
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
}
