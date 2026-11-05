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

package uk.gov.hmrc.sdecipaasstub.controllers.mft

import jakarta.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.Files.TemporaryFile
import play.api.mvc.*

import java.nio.file.Files
import java.security.MessageDigest
import scala.concurrent.ExecutionContext

@Singleton
class FileUploader @Inject() (
  cc: ControllerComponents
)(using ExecutionContext)
    extends AbstractController(cc)
    with Logging {
  private val Redirect_Key = "success_action_redirect"
  private val File_Key     = "file"

  def postUpload(bucket: String): Action[MultipartFormData[TemporaryFile]] = {
    logger.info(s"Uploading file to bucket $bucket")
    Action(parse.multipartFormData) { request =>
      given Request[MultipartFormData[TemporaryFile]] = request
      val redirectUrl                                 = request.body.dataParts.get(Redirect_Key).flatMap(_.headOption)

      request.body.file(File_Key) match
        case Some(file) =>
          redirectUrl match {
            case Some(redirectStr) => redirectAfterUpload(redirectStr, file, bucket)
            case None              => BadRequest("No Redirect URL supplied")
          }
        case None => BadRequest("No file supplied")
    }
  }

  private def redirectAfterUpload(url: String, file: MultipartFormData.FilePart[TemporaryFile], bucket: String)(using
    request: Request[MultipartFormData[TemporaryFile]]
  ) = {
    val key =
      request.body.dataParts.get("key").flatMap(_.headOption).map(_.split("/").head).getOrElse("No Object Key Defined")
    val etag         = calculateETag(file.ref.path)
    val fileMetaData = Map(
      "key"         -> Seq(key),
      "fileName"    -> Seq(file.filename),
      "contentType" -> Seq(file.contentType.getOrElse("")),
      "fileSize"    -> Seq(file.fileSize.toString),
      "etag"        -> Seq(etag),
      "bucket"      -> Seq(bucket)
    )
    Redirect(url, fileMetaData, SEE_OTHER)
  }

  private def calculateETag(path: java.nio.file.Path): String =
    val digest      = MessageDigest.getInstance("MD5")
    val inputStream = Files.newInputStream(path)
    try {
      val buffer = new Array[Byte](8192)

      def updateDigest(): Unit =
        inputStream.read(buffer) match
          case -1        => ()
          case bytesRead =>
            digest.update(buffer, 0, bytesRead)
            updateDigest()

      updateDigest()
      digest
        .digest()
        .map(b => f"${b & 0xff}%02x")
        .mkString
    } finally inputStream.close()
}
