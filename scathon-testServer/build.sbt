import BuildDefaults._

organization := "com.appministry"

name := "scathon-testServer"

version := "0.1.1"

scalaVersion := BuildDefaults.buildScalaVersion

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.34.0",
  "commons-io" % "commons-io" % "2.4",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

coverageEnabled := false
