import BuildDefaults._

organization := "com.appministry"

name := "scathon-models"

version := "0.1.1"

scalaVersion := BuildDefaults.buildScalaVersion

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.5.3",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

coverageEnabled := false