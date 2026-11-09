import _root_.io.swagger.v3.parser.OpenAPIV3Parser
import sbt.*
import sbt.Keys.*

import scala.jdk.CollectionConverters.asScalaBufferConverter

object Validate {
  val validate = taskKey[Unit]("Validate OpenAPI specification")
  def settings: Seq[Setting[_]] = Seq(
    validate := {
      val openApiFilePath = baseDirectory.value / "target/swagger/application.yaml"
      val parser          = new OpenAPIV3Parser()
      val result          = parser.readLocation(openApiFilePath.toString, null, null)

      // @formatter:off
      if (result.getMessages.isEmpty) {
        sys.error(s"Validation failed:\n${result.getMessages.asScala.mkString("\n")}")
      }
      // @formatter:on

    }
  )
}
