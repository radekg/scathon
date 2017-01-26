/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.testServer

import java.net.ServerSocket
import java.nio.file.{Files, Path}
import java.util.UUID
import java.util.concurrent._

import uk.co.appministry.scathon.testServer.plugins.{TPlugin, TestPlugin}
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.io.{Buf, Reader}
import com.twitter.logging.Logger
import com.twitter.util.{Future, Time}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import uk.co.appministry.scathon.models.v2._

import scala.util.Try

class TestMarathon(val cfg: Option[Config]=None) {

  //
  // SERVER INTERNALS
  //

  private var _server: Option[ListeningServer] = None
  private def getFreePort(): Int = {
    val ss = new ServerSocket(0)
    val port = ss.getLocalPort
    ss.close()
    port
  }
  private var ops: Operations = _

  //
  // CONFIGURATION
  //

  private var _host: Option[String] = None
  private var _port: Option[Int] = None
  private var _config: Config = cfg.getOrElse(ConfigFactory.empty())
  private var _tempDirectory: Option[Path] = None

  def config: Config = _config
  def host: Option[String] = _host
  def port: Option[Int] = _port
  def temp: Option[Path] = _tempDirectory

  //
  // OPERATIONAL
  //

  private[testServer] lazy val apps: ConcurrentHashMap[String, Application] = new ConcurrentHashMap[String, Application]
  private[testServer] lazy val appsTasks: ConcurrentHashMap[String, ConcurrentHashMap[String, Task]] = new ConcurrentHashMap[String, ConcurrentHashMap[String, Task]]
  private[testServer] lazy val deployments: ConcurrentHashMap[String, Deployment] = new ConcurrentHashMap[String, Deployment]
  private[testServer] lazy val callbackUrls: ConcurrentSkipListSet[String] = new ConcurrentSkipListSet[String]

  private[testServer] lazy val events: ConcurrentLinkedDeque[MarathonEventBusObject] = new ConcurrentLinkedDeque[MarathonEventBusObject]
  private[testServer] lazy val queue: ConcurrentLinkedDeque[QueueItem] = new ConcurrentLinkedDeque[QueueItem]
  private[testServer] lazy val deploymentQueue: ConcurrentLinkedDeque[Deployment] = new ConcurrentLinkedDeque[Deployment]

  private[testServer] def addDeployment(deployment: Deployment): DeploymentIdentifier = {
    deployments.put(deployment.id, deployment)
    deploymentQueue.add(deployment)
    DeploymentIdentifier(deployment.id, deployment.version)
  }
  private[testServer] def removeDeployment(deploymentId: String): Option[Deployment] = {
    Option(deployments.remove(deploymentId)) match {
      case Some(deployment) =>
        deploymentQueue.remove(deployment)
        Some(deployment)
      case None => None
    }
  }

  private[testServer] var leader: Option[GetLeaderResponse] = Some(GetLeaderResponse())
  private[testServer] lazy val frameworkId = UUID.randomUUID().toString
  private[testServer] val plugins: ConcurrentHashMap[String, TPlugin] = new ConcurrentHashMap[String, TPlugin]()
  private[testServer] val executor: ScheduledExecutorService = Executors.newScheduledThreadPool( Try { _config.getInt("testMarathon.executor-capacity") }.getOrElse(10) )
  private val workerQueue: QueueWorker = new QueueWorker(this)
  private val workerDeployments: DeploymentWorker = new DeploymentWorker(this)

  def start(): Unit = {
    _server match {
      case None =>

        val isUnitTested = Try { _config.getBoolean("testMarathon.for-unit-tests") }.getOrElse(false)
        _host = Try { Some(_config.getString("testMarathon.bind-host")) }.getOrElse( Some("localhost") )
        _port = Try { Some(_config.getInt("testMarathon.http-port")) }.getOrElse( Some(getFreePort()) )
        _tempDirectory = Some( Files.createTempDirectory("test-marathon") )

        ops = new Operations(this)
        val plugin = new TestPlugin
        plugins.put(plugin.info.id, plugin)

        workerQueue.startWork()
        workerDeployments.startWork()

        val service = new Service[Request, Response] {
          def apply(request: Request): Future[Response] = Future.value(
            (request.method, request.path) match {
              case ( Method.Get, path ) => path match {
                case Uris.appsUrl() => if (isUnitTested){
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:apps")))
                  } else {
                    ops.getApps(request)
                  }
                case Uris.appVersionUrl(appId, version) => if (isUnitTested) {
                  Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:appVersion:$appId:$version")))
                  } else {
                    ops.getAppVersion(request, appId, version)
                  }
                case Uris.appVersionsUrl(appId) => if (isUnitTested) {
                  Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:appVersions:$appId")))
                  } else {
                    ops.getAppVersions(request, appId)
                  }
                case Uris.appTasksUrl(appId) => if (isUnitTested) {
                  Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:appTasks:$appId")))
                  } else {
                    ops.getAppTasks(request, appId)
                  }
                case Uris.appUrl(appId) => if (isUnitTested) {
                  Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:app:$appId")))
                  } else {
                    ops.getApp(request, appId)
                  }
                case Uris.deploymentsUrl() => if (isUnitTested) {
                  Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:deployments")))
                  } else {
                    ops.getDeployments(request)
                  }
                case Uris.groupVersionsUrl(groupId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:groupVersions:$groupId")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupUrl(groupId) if groupId != "versions" => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:group:$groupId")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupsUrl() => if (isUnitTested) {
                  Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:groups")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupsVersionsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:groupVersions")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.tasksUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:tasks")))
                  } else {
                    ops.getTasks(request)
                  }
                case Uris.artifactUrl(path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:artifact:$path")))
                  } else {
                    ops.getArtifact(request, path)
                  }
                case Uris.eventsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:events")))
                  } else {
                    ops.getEvents(request)
                  }
                case Uris.eventSubscriptionsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:eventSubscriptions")))
                  } else {
                    ops.getCallbackUrls(request)
                  }
                case Uris.serverInfoUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:info")))
                  } else {
                    ops.getInfo(request)
                  }
                case Uris.leaderInfoUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:leader")))
                  } else {
                    ops.getLeader(request)
                  }
                case Uris.pluginsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:plugins")))
                  } else {
                    ops.getPlugins(request)
                  }
                case Uris.pluginUrl(pluginId, path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"GET:plugin:$pluginId:$path")))
                  } else {
                    ops.handlePluginsRequest(request, pluginId, path)
                  }
                case Uris.queueUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:queue")))
                  } else {
                    ops.getQueue(request)
                  }
                case Uris.pingUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:ping")))
                  } else {
                    ops.ping(request)
                  }
                case Uris.metricsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:metrics")))
                  } else {
                    ops.metrics(request)
                  }
                case Uris.loggingUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:logging")))
                  } else {
                    ops.logging(request)
                  }
                case Uris.helpUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET:help")))
                  } else {
                    ops.help(request)
                  }
                case _ => Response(Status.NotFound)
              }
              case ( Method.Post, path ) => path match {
                case Uris.appsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("POST:apps")))
                  } else {
                    ops.createApp(request)
                  }
                case Uris.appRestartUrl(appId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"POST:appRestart:$appId")))
                  } else {
                    ops.restartApp(request, appId)
                  }
                case Uris.groupsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("POST:groups")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupUrl(groupId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"POST:group:$groupId")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.tasksDeleteUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("POST:tasksDelete")))
                  } else {
                    ops.deleteTasks(request)
                  }
                case Uris.artifactsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("POST:artifacts")))
                  } else {
                    ops.uploadArtifact(request)
                  }
                case Uris.artifactUrl(path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"POST:artifact:$path")))
                  } else {
                    ops.uploadArtifact(request, Some(path))
                  }
                case Uris.eventSubscriptionsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("POST:eventSubscriptions")))
                  } else {
                    ops.createEventSubscription(request)
                  }
                case Uris.pluginUrl(pluginId, path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"POST:plugin:$pluginId:$path")))
                  } else {
                    ops.handlePluginsRequest(request, pluginId, path)
                  }
                case _ => Response(Status.NotFound)
              }
              case ( Method.Put, path ) => path match {
                case Uris.appsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("PUT:apps")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.appUrl(appId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"PUT:app:$appId")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("PUT:groups")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupUrl(groupId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"PUT:group:$groupId")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.pluginUrl(pluginId, path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"PUT:plugin:$pluginId:$path")))
                  } else {
                    ops.handlePluginsRequest(request, pluginId, path)
                  }
                case _ => Response(Status.NotFound)
              }
              case ( Method.Delete, path ) => path match {
                case Uris.appTaskUrl(appId, taskId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:appTask:$appId:$taskId")))
                  } else {
                    ops.deleteAppTask(request, appId, taskId)
                  }
                case Uris.appTasksUrl(appId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:appTasks:$appId")))
                  } else {
                    ops.deleteAppTasks(request, appId)
                  }
                case Uris.appUrl(appId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:app:$appId")))
                  } else {
                    ops.deleteApp(request, appId)
                  }
                case Uris.deploymentUrl(deploymentId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:deployment:$deploymentId")))
                  } else {
                    ops.deleteDeployment(request, deploymentId)
                  }
                case Uris.groupsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("DELETE:groups")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.groupUrl(groupId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:group:$groupId")))
                  } else {
                    Response(Status.Ok) // TODO
                  }
                case Uris.artifactUrl(path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:artifact:$path")))
                  } else {
                    ops.deleteArtifact(request, path)
                  }
                case Uris.eventSubscriptionsUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("DELETE:eventSubscriptions")))
                  } else {
                    ops.deleteEventSubscription(request)
                  }
                case Uris.leaderInfoUrl() => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("DELETE:leader")))
                  } else {
                    ops.deleteLeader(request)
                  }
                case Uris.pluginUrl(pluginId, path) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:plugin:$pluginId:$path")))
                  } else {
                    ops.handlePluginsRequest(request, pluginId, path)
                  }
                case Uris.queueAppDelayUrl(appId) => if (isUnitTested) {
                    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8(s"DELETE:queueAppDelay:$appId")))
                  } else {
                    ops.deleteQueueAppDelay(request, appId)
                  }
                case _ => Response(Status.NotFound)
              }
              case _ => Response(Status.MethodNotAllowed)
            }
          )
        }

        _server = Some(Http.server.withStreaming(enabled = true).serve(s"${_host.get}:${_port.get}", service))

      case Some(_) =>
        Logger.get().warning("TestMarathon already running.")
    }
  }

  def stop(): Unit = {
    _server.map { s => s.close(Time.Zero) }
    _server = None
    workerQueue.stopWork()
    workerDeployments.stopWork()
    _tempDirectory.map { d => FileUtils.deleteDirectory( d.toFile ) }
    _tempDirectory = None
  }

}
