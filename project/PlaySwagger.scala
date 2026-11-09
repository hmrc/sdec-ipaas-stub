import com.iheart.sbtPlaySwagger.SwaggerPlugin.autoImport.*
import sbt.Def

object PlaySwagger {
  lazy val settings: Seq[Def.Setting[_]] = Seq(
    swaggerDomainNameSpaces := Seq(
      "uk.gov.hmrc.sdecipaasstub.dto",
      "uk.gov.hmrc.sdecipaasstub.model"
    ),
    swaggerRoutesFile := "app.routes",
    swaggerV3 := true,
    swaggerPrettyJson := true
  )
}
