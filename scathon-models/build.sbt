import BuildDefaults._

scalaVersion := BuildDefaults.buildScalaVersion
version := BuildDefaults.buildVersion
organization := BuildDefaults.buildOrganization

name := "scathon-models"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "2.9.7",
  "com.typesafe.play" %% "play-json" % "2.6.0-M1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

coverageEnabled := false

crossScalaVersions := Seq("2.11.8", "2.12.1")