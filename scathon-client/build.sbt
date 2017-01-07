import BuildDefaults._

scalaVersion := BuildDefaults.buildScalaVersion
version := BuildDefaults.buildVersion
organization := BuildDefaults.buildOrganization

name := "scathon-client"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.41.0",
  "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

coverageEnabled := true

crossScalaVersions := Seq("2.11.8", "2.12.1")