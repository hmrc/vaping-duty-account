import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"

lazy val microservice = Project("vaping-duty-account", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    PlayKeys.playDefaultPort := 8144,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
    RoutesKeys.routesImport += "uk.gov.hmrc.vapingdutyaccount.models.VpdId",
    RoutesKeys.routesImport += "uk.gov.hmrc.vapingdutyaccount.models.InternalId",
    RoutesKeys.routesImport += "uk.gov.hmrc.vapingdutyaccount.models.CredentialId"
  )
  .settings(CodeCoverageSettings.settings *)

commands ++= Seq(
  Command.command("runAllChecks") { state => "clean" :: "compile" :: "scalafixAll --check" :: "coverage" :: "test" :: "it/test" :: "coverageReport" :: state },

  Command.command("runLocalChecks") { state => "clean" :: "compile" :: "scalafixAll --check" :: "coverage" :: "test" :: "coverageReport" :: state },

  Command.command("lint") { state => "clean" :: "compile" :: "scalafixAll" :: state }
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
