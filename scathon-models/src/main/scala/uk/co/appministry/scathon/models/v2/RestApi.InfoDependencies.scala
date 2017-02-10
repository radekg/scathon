/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class HttpConfiguration(
           val assetsPath: Option[String] = None,
           val httpPort: Int = 8080,
           val httpsPort: Int = 8443 ) extends MarathonApiObject

case class EventSubscriber(
           val `type`: String = "http_callback",
           val httpEndpoints: List[String] = List.empty[String] ) extends MarathonApiObject

case class MarathonConfiguration(
           val checkpoint:Boolean = false,
           val executor:String = "//cmd",
           val failoverTimeout: Int = 604800,
           val ha: Boolean = true,
           val hostname: String = "127.0.0.1",
           val localPortMax: Int = 49151,
           val localPortMin: Int = 32767,
           val master: String = "zk://localhost:2181/mesos",
           val mesosLeaderUiUrl: String = "http://mesos:5050",
           val mesosRole: Option[String] = None,
           val mesosUser: String = "root",
           val reconciliationInitialDelay: Int = 30000,
           val reconciliationInterval: Int = 30000,
           val taskLaunchTimeout: Int = 60000,
           val frameworkName: Option[String] = None,
           val webUri: Option[String] = None,
           val taskReservationTimeout: Option[Int] = None,
           val leaderProxyConnectionTimeoutMs: Option[Int] = None,
           val leaderProxyReadTimeoutMs: Option[Int] = None,
           val features: Option[List[String]] = None) extends MarathonApiObject

case class ZookeeperConfiguration(
           val zk:String = "zk://localhost:2181/marathon",
           val zkTimeout:Int=10000,
           val zkSessionTimeout:Int=1800000,
           val zkMaxVersions:Int=5 ) extends MarathonApiObject

trait HttpConfigurationParser {
  implicit val httpConfigurationFormat: Format[HttpConfiguration] = (
    ( __ \ "assets_path" ).formatNullable[ String ] and
    ( __ \ "http_port" ).format[Int] and
    ( __ \ "https_port" ).format[Int]
  )(HttpConfiguration.apply, unlift(HttpConfiguration.unapply))
}

trait EventSubscriberParser {
  implicit val eventSubscriberFormat: Format[EventSubscriber] = (
    ( __ \ "type" ).format[ String ] and
    ( __ \ "http_endpoints" ).format[List[String]]
  )(EventSubscriber.apply, unlift(EventSubscriber.unapply))
}

trait MarathonConfigurationParser {
  implicit val marathonConfigurationFormat: Format[MarathonConfiguration] = (
    ( __ \ "checkpoint" ).format[Boolean] and
    ( __ \ "executor" ).format[String] and
    ( __ \ "failover_timeout" ).format[Int] and
    ( __ \ "ha" ).format[Boolean] and
    ( __ \ "hostname" ).format[String] and
    ( __ \ "local_port_max" ).format[Int] and
    ( __ \ "local_port_min" ).format[Int] and
    ( __ \ "master" ).format[String] and
    ( __ \ "mesos_leader_ui_url" ).format[String] and
    ( __ \ "mesos_role" ).formatNullable[String] and
    ( __ \ "mesos_user" ).format[String] and
    ( __ \ "reconciliation_initial_delay" ).format[Int] and
    ( __ \ "reconciliation_interval" ).format[ Int ] and
    ( __ \ "task_launch_timeout" ).format[Int] and
    ( __ \ "framework_name" ).formatNullable[String] and
    ( __ \ "web_uri" ).formatNullable[String] and
    ( __ \ "task_reservation_timeout" ).formatNullable[Int] and
    ( __ \ "leader_proxy_connection_timeout_ms" ).formatNullable[Int] and
    ( __ \ "leader_proxy_read_timeout_ms" ).formatNullable[Int] and
    ( __ \ "features" ).formatNullable[List[String]]
  )(MarathonConfiguration.apply, unlift(MarathonConfiguration.unapply))
}

trait ZookeeperConfigurationParser {
  implicit val marathonZookeeperConfigFormat: Format[ZookeeperConfiguration] = (
    ( __ \ "zk" ).format[ String ] and
    ( __ \ "zk_timeout" ).format[Int] and
    ( __ \ "zk_session_timeout" ).format[Int] and
    ( __ \ "zk_max_versions" ).format[Int]
  )(ZookeeperConfiguration.apply, unlift(ZookeeperConfiguration.unapply))
}