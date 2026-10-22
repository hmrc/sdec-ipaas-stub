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
import cats.syntax.all.*
import uk.gov.hmrc.sdecipaasstub.dto.FileMetadata
import uk.gov.hmrc.sdecipaasstub.hcp.algebra.FileDataAccessAlgebra
import uk.gov.hmrc.sdecipaasstub.infrastructure.ApplicationLogger
import uk.gov.hmrc.sdecipaasstub.service.algebra.{FileServiceAlgebra, ThreadServiceAlgebra}
import uk.gov.hmrc.sdecipaasstub.service.errors.FileSaveError

import javax.inject.Inject

class FileService[F[_]: Sync] @Inject() (
  fileDataAccess: FileDataAccessAlgebra[F],
  threadService:  ThreadServiceAlgebra[F]
) extends FileServiceAlgebra[F]
    with ApplicationLogger[F] {

  override def saveFile(metadata: FileMetadata, threadRef: String): F[FileMetadata] = {
    for {
      _             <- logger.info(s"Save File: $metadata")
      maybeThreadId <- threadService.findIdByRef(threadRef)
      threadId = maybeThreadId match {
                   case Some(value) => value
                   case None        => throw new FileSaveError(s"Thread Reference does not exist: $threadRef")
                 }
      fileId    <- fileDataAccess.insert(metadata, threadId)
      fmdOption <- fileDataAccess.findById(fileId)
      fmd = fmdOption match {
              case Some(value) => value
              case None        => throw new FileSaveError(s"File Save Error: $metadata")
            }
    } yield FileMetadata.fromEntity(fmd)
  }.recoverWith { e =>
    logger.warn(s"File Save Failed with ${e.getMessage}")
    throw new FileSaveError(e.getMessage)
  }

}
