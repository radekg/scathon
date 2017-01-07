import BuildDefaults._

scalaVersion := BuildDefaults.buildScalaVersion
version := BuildDefaults.buildVersion
organization := BuildDefaults.buildOrganization

name := "scathon-testServer"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.twitter" %% "finagle-http" % "6.41.0",
  "commons-io" % "commons-io" % "2.4",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

coverageEnabled := false

crossScalaVersions := Seq("2.11.8", "2.12.1")