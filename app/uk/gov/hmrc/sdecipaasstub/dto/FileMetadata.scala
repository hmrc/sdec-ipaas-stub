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

package uk.gov.hmrc.sdecipaasstub.dto

import play.api.libs.json.{JsResult, JsValue, Json, OFormat}
import uk.gov.hmrc.sdecipaasstub.hcp.entity.SdecFileMetadata

final case class FileMetadata(
  filename:  String,
  s3Bucket:  String,
  filesize:  Long,
  filetype:  String,
  objectKey: String,
  etag:      String
)

object FileMetadata {
  given format: OFormat[FileMetadata] = Json.format[FileMetadata]

  def fromJson(json: JsValue): JsResult[FileMetadata] = json.validate[FileMetadata]

  def fromEntity(entity: SdecFileMetadata): FileMetadata =
    FileMetadata(
      filename = entity.filename,
      s3Bucket = entity.s3Bucket,
      filesize = entity.fileSize,
      filetype = entity.fileType,
      objectKey = entity.fileKey,
      etag = entity.fileEtag
    )
}
