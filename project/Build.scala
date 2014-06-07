import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object Build extends sbt.Build {

  lazy val proj = Project(
    "akka-cluster-example-inloop",
    file("."),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Dependencies.all))

  def commonSettings = Defaults.defaultSettings ++
    formatSettings ++
    Seq(
      organization := "io.inloop",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.4",
      autoCompilerPlugins := true,
      addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.4"),
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-P:continuations:enable"),
      publishTo <<= isSnapshot { isSnapshot =>
        val id = if (isSnapshot) "snapshots" else "releases"
        val uri = "http://repo.scala-sbt.org/scalasbt/sbt-plugin-" + id
        Some(Resolver.url("sbt-plugin-" + id, url(uri))(Resolver.ivyStylePatterns))
      },
      publishMavenStyle := false,
      resolvers ++= Seq(
        "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
        "spray" at "http://repo.spray.io",
        "spray nightly" at "http://nightlies.spray.io/"))
    

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test := formattingPreferences)

  import scalariform.formatter.preferences._
  def formattingPreferences =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, false)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(IndentSpaces, 2)
  
}

object Dependencies {
  val SPRAY_VERSION = "1.3.1"
  val AKKA_VERSION = "2.3.3"

  val akka_actor = "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION 
  val akka_cluster = "com.typesafe.akka" %% "akka-cluster" % AKKA_VERSION
  val spray_io = "io.spray" % "spray-io" % SPRAY_VERSION 

  val akka_testkit = "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % "test"
  val scalatest = "org.scalatest" %% "scalatest" % "2.1.3" % "test"
  val specs2 = "org.specs2" %% "specs2" % "2.3.11" % "test"

  val all = Seq(akka_actor, akka_cluster, spray_io, scalatest, akka_testkit, specs2)

}
