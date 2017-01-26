/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.testServer

import java.net.URI
import java.nio.charset.StandardCharsets

import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.logging.Logger
import com.twitter.util.Time
import play.api.libs.json.{JsValue, Json}
import uk.co.appministry.scathon.models.v2._

import scala.util.Try

class Callback(val callback: String, val event: MarathonEventBusObject) extends Runnable
  with AddHealthCheckEventParser
  with ApiPostEventParser
  with DeploymentFailedEventParser
  with DeploymentInfoEventParser
  with DeploymentSuccessEventParser
  with EventSubscriptionSubscribeEventParser
  with EventSubscriptionUnsubscribeEventParser
  with FrameworkMessageEventParser
  with GroupChangeFailedEventParser
  with GroupChangeSuccessEventParser
  with HealthStatusChangedEventParser
  with RemoveHealthCheckEventParser
  with StatusUpdateEventParser
  with UnhealthyTaskKillEventParser {

  private def withSerializedEvent( block: ( JsValue ) => Unit ): Unit = {
    Try { event match {
      case e: AddHealthCheckEvent => Json.toJson(e)
      case e: ApiPostEvent => Json.toJson(e)
      case e: DeploymentFailedEvent => Json.toJson(e)
      case e: DeploymentInfoEvent => Json.toJson(e)
      case e: DeploymentSuccessEvent => Json.toJson(e)
      case e: EventSubscriptionSubscribeEvent => Json.toJson(e)
      case e: EventSubscriptionUnsubscribeEvent => Json.toJson(e)
      case e: FrameworkMessageEvent => Json.toJson(e)
      case e: GroupChangeFailedEvent => Json.toJson(e)
      case e: GroupChangeSuccessEvent => Json.toJson(e)
      case e: HealthStatusChangedEvent => Json.toJson(e)
      case e: RemoveHealthCheckEvent => Json.toJson(e)
      case e: StatusUpdateEvent => Json.toJson(e)
      case e: UnhealthyTaskKillEvent => Json.toJson(e)
      case _ =>
        Logger.get().error("Not an event")
        throw new RuntimeException("Not an event")
    } }.map { value => block.apply(value) }
  }

  override def run(): Unit = {
    withSerializedEvent { jsonData =>
      val url  = new URI(callback)
      val dest = s"${url.getHost}:${url.getPort}"
      val client: Service[Request, Response] = Http.newService(dest)
      val request = Request(url.getPath)
      request.headerMap.add("Content-Type", "application/json; charset=utf-8")
      request.host = dest
      request.method = Method.Post
      request.write( jsonData.toString().getBytes(StandardCharsets.UTF_8) )
      client(request).onSuccess { resp =>
        client.close(Time.Zero) /* ignore failure */
      }.onFailure { ex =>
        Logger.get().warning(s"Failed to notify $callback about the event.")
        client.close(Time.Zero) /* ignore failure */
      }
    }
  }

}
