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

coverageEnabled in Test := true

crossScalaVersions := Seq("2.11.8", "2.12.1")

publishMavenStyle := true

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/AppMinistry/scathon</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:git://github.com/AppMinistry/scathon.git</connection>
      <developerConnection>scm:git:git://github.com/AppMinistry/scathon.git</developerConnection>
      <url>https://github.com/AppMinistry/scathon/tree/master</url>
    </scm>
    <developers>
      <developer>
        <id>radekg</id>
        <name>Radoslaw Gruchalski</name>
        <url>http://gruchalski.com</url>
      </developer>
    </developers>
  )