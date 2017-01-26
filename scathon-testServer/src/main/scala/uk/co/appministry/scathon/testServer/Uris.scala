package uk.co.appministry.scathon.testServer

/**
  * Created by rad on 1/26/17.
  */
object Uris {

  val appsUrl = "/v2/apps/?".r
  val appUrl = "/v2/apps/(.{1,})".r
  val appRestartUrl = "/v2/apps/(.{1,})/restart".r
  val appTasksUrl = "/v2/apps/(.{1,})/tasks".r
  val appTaskUrl = "/v2/apps/(.{1,})/tasks/(.{1,})".r
  val appVersionsUrl = "/v2/apps/(.{1,})/versions".r
  val appVersionUrl = "/v2/apps/(.{1,})/versions/(.{1,})".r

  val deploymentsUrl = "/v2/deployments".r
  val deploymentUrl = "/v2/deployments/(.{1,})".r

  val groupsUrl = "/v2/groups".r
  val groupsVersionsUrl = "/v2/groups/versions".r
  val groupUrl = "/v2/groups/(.{1,})".r
  val groupVersionsUrl = "/v2/groups/(.{1,})/versions".r

  val tasksUrl = "/v2/tasks".r
  val tasksDeleteUrl = "/v2/tasks/delete".r

  val artifactsUrl = "/v2/artifacts".r
  val artifactUrl = "/v2/artifacts/(.{1,})".r

  val eventsUrl = "/v2/events".r

  val eventSubscriptionsUrl = "/v2/eventSubscriptions".r

  val serverInfoUrl = "/v2/info".r
  val leaderInfoUrl = "/v2/leader".r

  val pluginsUrl = "/v2/plugins".r
  val pluginUrl = "/v2/plugins/(.[^/]{1,})/(.{1,})".r

  val queueUrl = "/v2/queue".r
  val queueAppDelayUrl = "/v2/queue/(.{1,})/delay".r

  val pingUrl = "/ping".r
  val metricsUrl = "/metrics".r
  val loggingUrl = "/logging".r
  val helpUrl = "/help".r

}
