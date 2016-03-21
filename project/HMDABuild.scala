import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scoverage.ScoverageSbtPlugin
import spray.revolver.RevolverPlugin.autoImport.Revolver

object BuildSettings {
  val buildOrganization = "cfpb"
  val buildVersion      = "1.0.0"
  val buildScalaVersion = "2.11.7"

  val buildSettings = Defaults.coreDefaultSettings ++
    Seq(
      organization := buildOrganization,
      version      := buildVersion,
      scalaVersion := buildScalaVersion,
      scalacOptions ++= Seq(
        "-Xlint",
        "-deprecation",
        "-unchecked",
        "-feature"),
      aggregate in assembly := false
    )

}


object HMDABuild extends Build {
  import Dependencies._
  import BuildSettings._

  val commonDeps = Seq(logback, scalaTest, scalaCheck)

  val akkaDeps = commonDeps ++ Seq(akka, akkaSlf4J, akkaStream)

  val httpDeps = akkaDeps ++ Seq(akkaHttp, akkaHttpJson, akkaHttpTestkit)

  val scalazDeps = scalaz

  lazy val hmda = (project in file("."))
    .settings(buildSettings:_*)
    .settings(Revolver.settings:_*)
    .settings(
      Seq(
        assemblyJarName in assembly := {s"${name.value}.jar"},
        mainClass in assembly := Some("hmda.api.HmdaApi"),
        assemblyMergeStrategy in assembly := {
          case "application.conf" => MergeStrategy.concat
          case "JS_DEPENDENCIES" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        }
      )
    ).dependsOn(api)
    .aggregate(
      parserJVM,
      parserJS,
      api,
      platformTest,
      validation)

  lazy val model = (crossProject in file("model"))
    .settings(buildSettings: _*)
    .enablePlugins(ScalaJSPlugin)
    .disablePlugins(ScoverageSbtPlugin)
    .jsSettings(

    )
    .jvmSettings(
      libraryDependencies ++= commonDeps ++ Seq(
        "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
      )
    )

  lazy val modelJS = model.js.disablePlugins(ScoverageSbtPlugin)
  lazy val modelJVM = model.jvm

  lazy val parser = (crossProject in file("parser"))
    .settings(buildSettings: _*)
    .jsSettings(
      scalaJSUseRhino in Global := false,
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % Version.scalaTest % "test",
        "org.scalacheck" %%% "scalacheck" % Version.scalaCheck % "test"
      )
    )
    .jvmSettings(
      libraryDependencies ++= commonDeps ++ Seq(
        "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
      )
    )
    .dependsOn(model)


  lazy val parserJVM = parser.jvm
  lazy val parserJS = parser.js.disablePlugins(ScoverageSbtPlugin)

  lazy val validation = (project in file("validation"))
    .settings(buildSettings: _*)
    .settings(
      libraryDependencies ++= commonDeps ++ Seq(scalazDeps)
    ).dependsOn(parserJVM % "compile->compile;test->test")


  lazy val api = (project in file("api"))
    .settings(buildSettings: _*)
    .settings(Revolver.settings:_*)
    .settings(
      Seq(
        scoverage.ScoverageKeys.coverageExcludedPackages := "hmda.api.HmdaApi",
        assemblyJarName in assembly := {s"${name.value}.jar"},
        mainClass in assembly := Some("hmda.api.HmdaApi"),
        assemblyMergeStrategy in assembly := {
          case "application.conf" => MergeStrategy.concat
          case x =>
            val oldStrategy = (assemblyMergeStrategy in assembly).value
            oldStrategy(x)
        },
        libraryDependencies ++= httpDeps
      )
    ).dependsOn(parserJVM)


  lazy val platformTest = (project in file("platform-test"))
    .settings(buildSettings: _*)
      .settings(
        Seq(
          libraryDependencies ++= akkaDeps
        )
      )
    .dependsOn(parserJVM)


}
