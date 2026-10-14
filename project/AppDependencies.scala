import sbt.Keys.libraryDependencies
import sbt.*

object AppDependencies {

  private val bootstrapVersion  = "10.8.0"
  private val scalaTestVersion  = "3.2.20"
  private val scaffeineVersion  = "5.3.0"
  private val catsEffectVersion = "3.7.0"
  private val log4catsVersion   = "2.6.0"
  private val logbackVersion    = "1.4.11"
  private val playTestVersion   = "7.0.2"
  private val h2Version         = "2.5.250"
  private val doobieVersion     = "1.0.0-RC10"
  private val flywayVersion     = "10.17.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-effect"               % catsEffectVersion,
    "ch.qos.logback" % "logback-classic"           % logbackVersion,
    "org.typelevel" %% "log4cats-core"             % log4catsVersion,
    "org.typelevel" %% "log4cats-slf4j"            % log4catsVersion,
    "com.h2database" % "h2"                        % h2Version,
    "org.tpolecat"  %% "doobie-core"               % doobieVersion,
    "org.tpolecat"  %% "doobie-h2"                 % doobieVersion,
    "org.tpolecat"  %% "doobie-hikari"             % doobieVersion,
    "org.flywaydb"   % "flyway-core"               % flywayVersion
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"     % playTestVersion  % Test,
    "org.scalatest"          %% "scalatest"              % scalaTestVersion % Test
  )

  val it = Seq.empty
}
