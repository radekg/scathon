import BuildDefaults._

name := "scathon-client"

version := "0.1.0"

scalaVersion := BuildDefaults.buildScalaVersion

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.34.0",
  "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

coverageEnabled := true