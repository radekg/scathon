/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class GetAppsResponse(
           val apps: List[Application] )

case class GetAppResponse(
           val app: Application )

case class GetTasksResponse(
           val tasks: List[Task] )

case class GetVersionsResponse(
           val versions: List[DateTime] )

case class GetEventSubscriptionsResponse(
           val callbackUrls: List[String] )

case class GetInfoResponse(
           val frameworkId: String,
           val httpConfig: HttpConfiguration,
           val eventSubscriber: EventSubscriber,
           val marathonConfig: MarathonConfiguration,
           val zookeeperConfig: ZookeeperConfiguration,
           val leader: Option[String] = None,
           val version: String = "0.15.3",
           val name: String = "marathon" )

case class GetLeaderResponse(
           val leader:String = "127.0.0.1:8080" )

case class GetPluginsResponse(
           val plugins: List[Plugin] = List.empty[Plugin] )

case class GetQueueResponse(
           val queue: List[QueueItem] = List.empty[QueueItem] )

trait GetAppsResponseParser extends ApplicationParser {
  implicit val getAppsResponseFormat: Format[GetAppsResponse] = ( __ \ "apps" ).format[List[Application]].inmap(GetAppsResponse.apply, unlift(GetAppsResponse.unapply))
}

trait GetAppResponseParser extends ApplicationParser {
  implicit val getAppResponseFormat: Format[GetAppResponse] = ( __ \ "app" ).format[Application].inmap(GetAppResponse.apply, unlift(GetAppResponse.unapply))
}

trait GetApplicationVersionsResponseParser extends EnumParser with VersionParser {
  implicit val getAppVersionsResponseFormat: Format[GetVersionsResponse] = ( __ \ "versions" ).format[List[DateTime]].inmap(GetVersionsResponse.apply, unlift(GetVersionsResponse.unapply))
}

trait GetApplicationTasksResponseParser extends GetTasksResponseParser

trait GetTasksResponseParser extends TaskParser {
  implicit val getTasksResponseFormat: Format[GetTasksResponse] = ( __ \ "tasks" ).format[List[Task]].inmap(GetTasksResponse.apply, unlift(GetTasksResponse.unapply))
}

trait GetEventSubscriptionsResponseParser {
  implicit val getEventSubscriptionsResponseFormat: Format[GetEventSubscriptionsResponse] = ( __ \ "callbackUrls" ).format[List[String]].inmap(GetEventSubscriptionsResponse.apply, unlift(GetEventSubscriptionsResponse.unapply))
}

trait GetInfoResponseParser extends HttpConfigurationParser
                            with    EventSubscriberParser
                            with    MarathonConfigurationParser
                            with    ZookeeperConfigurationParser {
  implicit val getInfoResponseFormat: Format[GetInfoResponse] = (
    ( __ \ "frameworkId" ).format[ String ] and
    ( __ \ "http_config" ).format[HttpConfiguration] and
    ( __ \ "event_subscriber" ).format[EventSubscriber] and
    ( __ \ "marathon_config" ).format[MarathonConfiguration] and
    ( __ \ "zookeeper_config" ).format[ZookeeperConfiguration] and
    ( __ \ "leader" ).formatNullable[String] and
    ( __ \ "version" ).format[String] and
    ( __ \ "name" ).format[String]
  )(GetInfoResponse.apply, unlift(GetInfoResponse.unapply))
}

trait GetLeaderResponseParser {
  implicit val getLeaderInfoFormat: Format[GetLeaderResponse] = ( __ \ "leader" ).format[ String ].inmap(GetLeaderResponse.apply, unlift(GetLeaderResponse.unapply))
}

trait GetPluginsResponseParser extends PluginParser {
  implicit val getPluginsResponseFormat: Format[GetPluginsResponse] = ( __ \ "plugins" ).format[List[Plugin]].inmap(GetPluginsResponse.apply, unlift(GetPluginsResponse.unapply))
}

trait GetQueueResponseParser extends QueueItemParser {
  implicit val getQueueResponseFormat: Format[GetQueueResponse] = ( __ \ "queue" ).format[List[QueueItem]].inmap(GetQueueResponse.apply, unlift(GetQueueResponse.unapply))
}
