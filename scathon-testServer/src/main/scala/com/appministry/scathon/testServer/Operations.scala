/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.testServer

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.appministry.scathon.models.v2.{Version => MVersion}
import com.appministry.scathon.models.v2._
import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.finagle.http.exp.Multipart
import com.twitter.finagle.http._
import com.twitter.io.{Buf, Reader}
import com.twitter.logging.Logger
import com.twitter.util.{JavaTimer, Future}
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json

import scala.collection.JavaConversions._
import scala.util.Try

class Operations(val marathon: TestMarathon ) extends
  GetAppsResponseParser
  with GetAppResponseParser
  with GetTasksResponseParser
  with GetApplicationVersionsResponseParser
  with ApplicationParser
  with DeploymentParser
  with DeploymentIdentifierParser
  with GetEventSubscriptionsResponseParser
  with EventSubscriptionSubscribeEventParser
  with EventSubscriptionUnsubscribeEventParser
  with GetLeaderResponseParser
  with GetPluginsResponseParser
  with GetQueueResponseParser
  with GetInfoResponseParser {

  private val fileField: String = "file"
  private val apiVersion = Try { marathon.config.getString("testMarathon.api-version") }.getOrElse( "v2" )

  //
  // APPS
  // TODO: implement: putApps, putApp

  def getApps(request: Request): Response = {
    val resp = GetAppsResponse( apps = marathon.apps.values().toList )
    Response(request.version, Status.Ok, Reader.fromBuf(
      Buf.Utf8( getAppsResponseFormat.writes(resp).toString() )
    ))
  }

  def getApp(request: Request, appId: String): Response = {
    if (marathon.apps.containsKey(appId)) {
      val app = marathon.apps.get(appId).copy(
        tasks = Some( marathon.appsTasks.getOrDefault( appId, new ConcurrentHashMap[String, Task]() ).values().toList ) )
      Response(request.version, Status.Ok, Reader.fromBuf(
        Buf.Utf8( getAppResponseFormat.writes( GetAppResponse(app) ).toString() )
      ))
    } else {
      Response(request.version, Status.NotFound)
    }
  }

  def getAppTasks(request: Request, appId: String): Response = {
    if (marathon.apps.containsKey(appId)) {
      val tasks = marathon.appsTasks.getOrDefault( appId, new ConcurrentHashMap[String, Task]() ).values().toList
      Response(request.version, Status.Ok, Reader.fromBuf(
        Buf.Utf8( getTasksResponseFormat.writes( GetTasksResponse(tasks) ).toString() )
      ))
    } else {
      Response(request.version, Status.NotFound)
    }
  }

  def getAppVersions(request: Request, appId: String): Response = {
    if (marathon.apps.containsKey(appId)) {
      Response(request.version, Status.Ok, Reader.fromBuf(
        Buf.Utf8( getAppVersionsResponseFormat.writes( GetVersionsResponse(List(marathon.apps.get(appId).version.get)) ).toString() )
      ))
    } else {
      Response(request.version, Status.NotFound)
    }
  }

  def getAppVersion(request: Request, appId: String, version: String): Response = {
    if (marathon.apps.containsKey(appId)) {
      if ( marathon.apps.get(appId).version.get.compareTo( MVersion(version) ) == 0 ) {
        Response(request.version, Status.Ok, Reader.fromBuf(
          Buf.Utf8( applicationFormat.writes( marathon.apps.get(appId) ).toString() )
        ))
      } else {
        Response(request.version, Status.NotFound)
      }
    } else {
      Response(request.version, Status.NotFound)
    }
  }

  def createApp(request: Request): Response = {
    applicationFormat.reads(Json.parse(request.contentString)).asOpt match {
      case Some(app) =>
        if (marathon.apps.containsKey(app.id)) {
          Response(Status.Conflict)
        } else {
          val storedApp = app.copy(version = Some(MVersion()))
          marathon.apps.put(app.id, storedApp)
          marathon.callbackUrls.foreach { callbackUrl =>
            val event = ApiPostEvent(
              eventType = EventTypes.api_post_event,
              timestamp = MVersion(),
              clientIp = request.remoteAddress.getHostAddress,
              uri = "/v2/apps/" + storedApp.id,
              appDefinition = storedApp )
            marathon.executor.execute( new Callback( callbackUrl, event ) )
          }
          marathon.queue.offer(QueueItem( storedApp, storedApp.instances, QueueDelay(0, false) ))
          Response(request.version, Status.Created, Reader.fromBuf(
            Buf.Utf8(applicationFormat.writes(app).toString())
          ))
        }
      case None =>
        Response(request.version, Status.BadRequest)
    }
  }

  def deleteApp(request: Request, appId: String): Response = {
    Option(marathon.apps.get(appId)) match {
      case Some(app) =>
        val deployment = Deployment( id = UUID.randomUUID().toString,
                                     version = MVersion(),
                                     affectedApps = List(app.id),
                                     steps = List(List(DeploymentStep(
                                      action = DeploymentActionTypes.STOP_APPLICATION,
                                      app = appId
                                     ))),
                                     currentActions = List(DeploymentCurrentAction(
                                      action = DeploymentActionTypes.STOP_APPLICATION,
                                      app = appId
                                     )),
                                     currentStep = 1,
                                     totalSteps = 1 )

        marathon.callbackUrls.foreach { callbackUrl =>
          val event = ApiPostEvent(
            eventType = EventTypes.api_post_event,
            timestamp = MVersion(),
            clientIp = request.remoteAddress.getHostAddress,
            uri = "/v2/apps/" + app.id,
            appDefinition = app )
          marathon.executor.execute( new Callback( callbackUrl, event ) )
        }

        Response(request.version, Status.Ok, Reader.fromBuf(
          Buf.Utf8( deploymentIdentifierFormat.writes( marathon.addDeployment(deployment) ).toString() )
        ))
      case None => Response(Status.NotFound)
    }
  }

  def deleteAppTasks(request: Request, appId: String): Response = {
    Option(marathon.apps.get(appId)) match {
      case Some(app) =>
        val removedTasks = marathon.appsTasks.remove(app.id)
        if (request.params.getBooleanOrElse("force", false)) {
          val deployment = Deployment( id = UUID.randomUUID().toString,
                                       version = MVersion(),
                                       affectedApps = List.empty[String],
                                       steps = List(List.empty[DeploymentStep]),
                                       currentActions = List.empty[DeploymentCurrentAction],
                                       currentStep = 1,
                                       totalSteps = 1 )
          // we do not process this deployment, only satisfy responses
          Response(request.version, Status.Ok, Reader.fromBuf(
            Buf.Utf8( deploymentIdentifierFormat.writes( DeploymentIdentifier(deployment.id, deployment.version) ).toString() )
          ))
        } else {
          Response(request.version, Status.Ok, Reader.fromBuf(
            Buf.Utf8( getTasksResponseFormat.writes(GetTasksResponse(removedTasks.values().toList)).toString() )
          ))
        }
      case None => Response(Status.NotFound)
    }
  }

  def deleteAppTask(request: Request, appId: String, taskId: String): Response = {
    Option(marathon.apps.get(appId)) match {
      case Some(app) =>
        val tasks = marathon.appsTasks.getOrDefault(app.id, new ConcurrentHashMap[String, Task]())
        Option( tasks.remove(taskId) ) match {
          case Some(task) =>
            if (request.params.getBooleanOrElse("force", false)) {
              val deployment = Deployment( id = UUID.randomUUID().toString,
                                           version = MVersion(),
                                           affectedApps = List.empty[String],
                                           steps = List(List.empty[DeploymentStep]),
                                           currentActions = List.empty[DeploymentCurrentAction],
                                           currentStep = 0,
                                           totalSteps = 0 )
              // we do not process this deployment, only satisfy responses
              Response(request.version, Status.Ok, Reader.fromBuf(
                Buf.Utf8( deploymentIdentifierFormat.writes( DeploymentIdentifier(deployment.id, deployment.version) ).toString() )
              ))
            } else {
              Response(request.version, Status.Ok, Reader.fromBuf(
                Buf.Utf8( getTasksResponseFormat.writes(GetTasksResponse(List(task)) ).toString )
              ))
            }
          case None => Response(Status.NotFound)
        }
      case None => Response(Status.NotFound)
    }
  }

  def restartApp(request: Request, appId: String): Response = {
    Option(marathon.apps.get(appId)) match {
      case Some(app) =>
        val deployment = Deployment( id = UUID.randomUUID().toString,
                                     version = MVersion(),
                                     affectedApps = List(app.id),
                                     steps = List(List(DeploymentStep(
                                      action = DeploymentActionTypes.RESTART_APPLICATION,
                                      app = appId
                                     ))),
                                     currentActions = List(DeploymentCurrentAction(
                                      action = DeploymentActionTypes.RESTART_APPLICATION,
                                      app = appId
                                     )),
                                     currentStep = 1,
                                     totalSteps = 1 )

        marathon.callbackUrls.foreach { callbackUrl =>
          val event = ApiPostEvent(
            eventType = EventTypes.api_post_event,
            timestamp = MVersion(),
            clientIp = request.remoteAddress.getHostAddress,
            uri = "/v2/apps/" + app.id,
            appDefinition = app )
          marathon.executor.execute( new Callback( callbackUrl, event ) )
        }

        Response(request.version, Status.Ok, Reader.fromBuf(
          Buf.Utf8( deploymentIdentifierFormat.writes( marathon.addDeployment(deployment) ).toString() )
        ))
      case None => Response(Status.NotFound)
    }
  }

  //
  // DEPLOYMENTS
  //

  def getDeployments(request: Request): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf(
      Buf.Utf8( Json.toJson(marathon.deployments.values().toList).toString() )
    ))
  }

  def deleteDeployment(request: Request, deploymentId: String): Response = {
    marathon.removeDeployment(deploymentId) match {
      case None => Response(Status.NotFound)
      case Some(oldDeployment) =>
        if (request.params.getBooleanOrElse("force", false)) {
          Response(Status.Accepted)
        } else {
          val newDeployment = oldDeployment.copy(id = UUID.randomUUID().toString, version = MVersion())
          marathon.addDeployment(newDeployment)
          Response(request.version, Status.Ok, Reader.fromBuf(
            Buf.Utf8( deploymentFormat.writes(newDeployment).toString() )
          ))
        }
    }
  }

  //
  // GROUPS
  // TODO: implement: getGroups, updateGroups, createGroups, deleteGroups, getGroupsVersions
  // TODO: implement: getGroup, updateGroup, createGroup, deleteGroup, getGroupVersions

  //
  // TASKS
  //

  def getTasks(request: Request): Response = {
    val tasks = marathon.appsTasks.values().toList.foldLeft(List.empty[Task])( (aggr, tasks) => aggr ++ tasks.values().toList )
    Response(request.version, Status.Ok, Reader.fromBuf(
      Buf.Utf8( getTasksResponseFormat.writes( GetTasksResponse(tasks) ).toString() )
    ))
  }

  def deleteTasks(request: Request): Response = {
    val tasks = marathon.appsTasks.values().toList.foldLeft(List.empty[Task])( (aggr, tasks) => aggr ++ tasks.values().toList )
    marathon.appsTasks.clear()
    if (request.params.getBooleanOrElse("force", false)) {
      val deployment = Deployment( id = UUID.randomUUID().toString,
                                   version = MVersion(),
                                   affectedApps = List.empty[String],
                                   steps = List(List.empty[DeploymentStep]),
                                   currentActions = List.empty[DeploymentCurrentAction],
                                   currentStep = 0,
                                   totalSteps = 0 )
      // we do not process this deployment, only satisfy responses
      Response(request.version, Status.Ok, Reader.fromBuf(
        Buf.Utf8( deploymentIdentifierFormat.writes( DeploymentIdentifier(deployment.id, deployment.version) ).toString() )
      ))
    } else {
      Response(request.version, Status.Ok, Reader.fromBuf(
        Buf.Utf8( getTasksResponseFormat.writes( GetTasksResponse(tasks) ).toString() )
      ))
    }
  }

  //
  // ARTIFACTS
  //

  private def resolveDir(path: Option[String], fileName: String): File = {
    path match {
      case Some(p) =>
        val resolved = marathon.temp.get.resolve(p)
        if (resolved.toAbsolutePath.toString.endsWith(fileName)) {
          new File(resolved.toFile.getParent)
        } else {
          resolved.toFile
        }
      case None => marathon.temp.get.toFile
    }
  }

  def withArtifactLocationHeader(response: Response, path: String): Response = {
    response.headerMap.put(Fields.Location, s"http://${marathon.host.get}:${marathon.port.get}/$apiVersion/artifacts$path")
    response
  }

  def uploadArtifact(request: Request, path: Option[String]=None): Response = {
    request.multipart match {
      case Some(multipart) =>
        if ( multipart.files.contains(fileField) ) {
          multipart.files(fileField).head match {
            case Multipart.OnDiskFileUpload(file, contentType, fileName, contentTransferEncoding) =>
              val targetDir = resolveDir(path, fileName)
              FileUtils.forceMkdir( targetDir )
              val targetFile = new File(targetDir.getAbsolutePath + "/" + fileName)
              FileUtils.copyFile(file, targetFile)
              withArtifactLocationHeader( Response(Status.Created), targetFile.getAbsolutePath.replaceFirst(marathon.temp.get.toFile.getAbsolutePath, "") )
            case Multipart.InMemoryFileUpload(buf, contentType, fileName, contentTransferEncoding) =>
              val targetDir = resolveDir(path, fileName)
              FileUtils.forceMkdir( targetDir )
              val targetFile = new File(targetDir.getAbsolutePath + "/" + fileName)
              var buffer = new Array[Byte](buf.length)
              buf.write(buffer, 0)
              FileUtils.writeByteArrayToFile(targetFile, buffer)
              withArtifactLocationHeader( Response(Status.Created), targetFile.getAbsolutePath.replaceFirst(marathon.temp.get.toFile.getAbsolutePath, "") )
          }
        } else {
          Response(Status.Ok)
        }
      case None =>
        Response(Status.Ok)
    }
  }

  def getArtifact(request: Request, path:String): Response = {
    val resolved = marathon.temp.get.resolve(path).toFile
    if (resolved.exists()) {
      val data = Files.readAllBytes(Paths.get(resolved.toURI))
      Response(request.version, Status.Ok, Reader.fromBuf( Buf.ByteArray.Owned(data) ))
    } else {
      Response(Status.NotFound)
    }
  }

  def deleteArtifact(request: Request, path:String): Response = {
    val resolved = marathon.temp.get.resolve(path).toFile
    if (resolved.exists()) {
      resolved.delete()
      Response(Status.Ok)
    } else {
      Response(Status.NotFound)
    }
  }

  //
  // EVENTS
  // We are emulating anything, the content not important at this stage.
  implicit val timer = new JavaTimer
  def events(): AsyncStream[String] =
    System.currentTimeMillis().toString +:: AsyncStream.fromFuture(Future.sleep(1000.millis)).flatMap(_ => events())
  @volatile private[this] var eventsStream: AsyncStream[Buf] = events().map(n => Buf.Utf8(n))
  eventsStream.foreach(_ => eventsStream = eventsStream.drop(1))
  def getEvents(request: Request): Response = {
    val writable = Reader.writable()
    eventsStream.foreachF(writable.write)
    Response(request.version, Status.Ok, writable)
  }

  //
  // EVENT SUBSCRIPTIONS
  //

  def getCallbackUrls(request: Request): Response = {
    val resp = GetEventSubscriptionsResponse( callbackUrls = marathon.callbackUrls.toList )
    Response(request.version, Status.Ok, Reader.fromBuf(
      Buf.Utf8( getEventSubscriptionsResponseFormat.writes( resp ).toString() )
    ))
  }

  def createEventSubscription(request: Request): Response = {
    request.params.get("callbackUrl") match {
      case Some(url) =>
        val event = EventSubscriptionSubscribeEvent(
          callbackUrl = url,
          clientIp = "0:0:0:0:0:0:0:1",
          timestamp = MVersion() )
        marathon.callbackUrls.foreach { callbackUrl =>
          marathon.executor.execute( new Callback(callbackUrl, event) )
        }
        marathon.callbackUrls.add(event.callbackUrl)
        Logger.get().info(s"${event.callbackUrl} registered as event listener")
        Response(request.version, Status.Created, Reader.fromBuf(
          Buf.Utf8( eventSubscriptionSubscribeEventFormat.writes( event ).toString() )
        ))
      case None => Response(Status.BadRequest)
    }
  }

  def deleteEventSubscription(request: Request): Response = {
    request.params.get("callbackUrl") match {
      case Some(url) =>
        val event = EventSubscriptionUnsubscribeEvent(
          callbackUrl = url,
          clientIp = "0:0:0:0:0:0:0:1",
          timestamp = MVersion() )
        marathon.callbackUrls.remove(event.callbackUrl)
        marathon.callbackUrls.foreach { callbackUrl =>
          marathon.executor.execute( new Callback(callbackUrl, event) )
        }
        Logger.get().info(s"${event.callbackUrl} de-registered as event listener")
        Response(request.version, Status.Ok, Reader.fromBuf(
          Buf.Utf8( eventSubscriptionUnsubscribeEventFormat.writes( event ).toString() )
        ))
      case None => Response(Status.BadRequest)
    }
  }

  //
  // SERVER INFO
  //

  def getInfo(req: Request): Response = {
    val info = GetInfoResponse(
      frameworkId = marathon.frameworkId,
      leader = marathon.leader.map { l => l.leader },
      httpConfig = HttpConfiguration(),
      eventSubscriber = EventSubscriber(
        httpEndpoints = marathon.callbackUrls.toList
      ),
      marathonConfig = MarathonConfiguration(),
      zookeeperConfig = ZookeeperConfiguration() )
    Response(req.version, Status.Ok, Reader.fromBuf(
      Buf.Utf8( getInfoResponseFormat.writes( info ).toString() )
    ))
  }

  //
  // LEADER
  //

  def getLeader(req: Request): Response = {
    marathon.leader match {
      case Some(l) =>
        Response(req.version, Status.Ok, Reader.fromBuf(
          Buf.Utf8( getLeaderInfoFormat.writes( l ).toString() )
        ))
      case None => Response(req.version, Status.NotFound)
    }
  }

  def deleteLeader(req: Request): Response = {
    marathon.leader match {
      case Some(_) =>
        marathon.leader = None
        Response(req.version, Status.Ok, Reader.fromBuf(
          Buf.Utf8( """{ "message": "Leadership abdicated" }""" )
        ))
      case None => Response(Status.NotFound)
    }
  }

  //
  // PLUGINS
  //

  def getPlugins(request: Request): Response = {
    val resp = GetPluginsResponse(plugins = marathon.plugins.values().toList.map { tplugin => tplugin.info })
    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(
      getPluginsResponseFormat.writes(resp).toString()
    )))
  }

  def handlePluginsRequest(request: Request, pluginId: String, path: String): Response = {
    if (marathon.plugins.containsKey(pluginId)) {
      if (request.method == Method.Get) {
        marathon.plugins.get(pluginId).get(request, path)
      } else if (request.method == Method.Put) {
        marathon.plugins.get(pluginId).put(request, path)
      } else if (request.method == Method.Post) {
        marathon.plugins.get(pluginId).post(request, path)
      } else if (request.method == Method.Delete) {
        marathon.plugins.get(pluginId).delete(request, path)
      } else {
        Response(Status.MethodNotAllowed)
      }
    } else {
      Response(Status.NotFound)
    }
  }

  //
  // QUEUE
  //

  def getQueue(request: Request): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf(
      Buf.Utf8( getQueueResponseFormat.writes(GetQueueResponse(marathon.queue.toList) ).toString() )
    ))
  }

  def deleteQueueAppDelay(request: Request, appId: String): Response = {
    Option(marathon.apps.get(appId)) match {
      case None => Response(Status.NotFound)
      case Some(_) => Response(Status.Ok)
    }
  }

  //
  // MISC
  //

  def ping(request: Request): Response = {
    val resp = Response(request.version, Status.Ok, Reader.fromBuf( Buf.Utf8( "pong" ) ))
    resp.headerMap.add(Fields.ContentType, "text/plain")
    resp
  }

  def logging(request: Request): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf( Buf.Utf8( """<html><head><title>Logging</title></head><body>HTML mock endpoint</body></html>""" ) ))
  }

  def help(request: Request): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf( Buf.Utf8( """<html><head><title>Help</title></head><body>HTML mock endpoint</body></html>""" ) ))
  }

  def metrics(request: Request): Response = {
    val data =
      """
        |{
        |  "counters": {
        |    "name.of.counter": {
        |      "count": 1
        |    }
        |  },
        |  "gauges": {
        |    "name.of.gauge": {
        |      "value": 7248
        |    }
        |  },
        |  "histograms": {
        |    "name.of.histogram": {
        |      "count": 0,
        |      "max": 0,
        |      "mean": 0.0,
        |      "min": 0,
        |      "p50": 0.0,
        |      "p75": 0.0,
        |      "p95": 0.0,
        |      "p98": 0.0,
        |      "p99": 0.0,
        |      "p999": 0.0,
        |      "stddev": 0.0
        |    }
        |  },
        |  "meters": {
        |    "name.of.meter": {
        |      "count": 0,
        |      "m15_rate": 0.0,
        |      "m1_rate": 0.0,
        |      "m5_rate": 0.0,
        |      "mean_rate": 0.0,
        |      "units": "events/second"
        |    }
        |  },
        |  "timers": {
        |    "name.of.timer": {
        |      "count": 1,
        |      "duration_units": "seconds",
        |      "m15_rate": 0.2,
        |      "m1_rate": 0.2,
        |      "m5_rate": 0.2,
        |      "max": 0.0021718640000000003,
        |      "mean": 0.0021718640000000003,
        |      "mean_rate": 0.13897812037014803,
        |      "min": 0.0021718640000000003,
        |      "p50": 0.0021718640000000003,
        |      "p75": 0.0021718640000000003,
        |      "p95": 0.0021718640000000003,
        |      "p98": 0.0021718640000000003,
        |      "p99": 0.0021718640000000003,
        |      "p999": 0.0021718640000000003,
        |      "rate_units": "calls/second",
        |      "stddev": 0.0
        |    }
        |  },
        |  "version": "3.0.0"
        |}
      """.stripMargin
    val resp = Response(request.version, Status.Ok, Reader.fromBuf( Buf.Utf8( data ) ))
    resp.headerMap.add(Fields.ContentType, "application/json")
    resp
  }

}
