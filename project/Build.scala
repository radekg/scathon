import sbt._

object MarathonIntegrationBuild extends Build {

  lazy val root = Project("scathon", file(".")).aggregate(models, apiClient, testServer)

  lazy val models = Project("scathon-models", file("scathon-models"))
  lazy val testServer = Project("scathon-testServer", file("scathon-testServer")).dependsOn(models)
  lazy val apiClient = Project("scathon-client", file("scathon-client")).dependsOn(models, testServer)

}