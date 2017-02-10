/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

import uk.co.appministry.scathon.models.v2._
import uk.co.appministry.scathon.models.v2.util.VersionUtils
import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.finagle.http._
import com.twitter.finagle.{Http, Service}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future => TFuture}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class Client( val host: String = "localhost",
              val port: Int = 8080,
              val version: String = "v2",
              val requestTimeout: Int = 5000,
              val username: Option[String] = None,
              val password: Option[String] = None )
                  extends GetAppsResponseParser
                  with    GetAppResponseParser
                  with    ApplicationParser
                  with    GetApplicationVersionsResponseParser
                  with    DeploymentIdentifierParser
                  with    DeploymentParser
                  with    GroupParser
                  with    GetEventSubscriptionsResponseParser
                  with    EventSubscriptionSubscribeEventParser
                  with    EventSubscriptionUnsubscribeEventParser
                  with    GetInfoResponseParser
                  with    GetLeaderResponseParser
                  with    GetPluginsResponseParser
                  with    GetQueueResponseParser
                  with    GetTasksResponseParser
                  with    VersionParser {

  private val clientHost = s"$host:$port"
  private val cli: Service[Request, Response] = Http.client.withRequestTimeout(requestTimeout.milliseconds).newService(clientHost)
  private var authorization: Option[String] = ( username, password ) match {
    case (Some(u), Some(p)) => Some(s"Basic ${ Base64.getEncoder.encodeToString(s"$u:$p".getBytes(StandardCharsets.UTF_8)) }")
    case _ => None
  }

  def constructRequestUri( endpoint: String): String = {
    s"/$version/$endpoint"
  }

  def request( method: Method, endpoint: String, headers: Map[String, String] = Map.empty[String, String] ): TFuture[Response] = {
    request(method, endpoint, headers, None)
  }

  def request( method: Method, endpoint: String, content: Array[Byte] ): TFuture[Response] = {
    request(method, endpoint, Map.empty[String, String], Some(content))
  }

  def request( method: Method, endpoint: String, headers: Map[String, String], content: Array[Byte] ): TFuture[Response] = {
    request(method, endpoint, headers, Some(content))
  }

  def request( method: Method, endpoint: String, params: Tuple2[String, String]* ): TFuture[Response] = {
    request(method, endpoint, Map.empty[String, String], None, params: _*)
  }

  def request( method: Method, endpoint: String, headers: Map[String, String], params: Tuple2[String, String]* ): TFuture[Response] = {
    request(method, endpoint, headers, None, params: _*)
  }

  def request( method: Method, endpoint: String, headers: Map[String, String], content: Option[Array[Byte]], params: Tuple2[String, String]* ): TFuture[Response] = {
    cli( buildRequest( method, endpoint, Map.empty[String, String], content, params: _* ) )
  }

  def buildRequest(method: Method, endpoint: String, headers: Map[String, String], content: Option[Array[Byte]], params: Tuple2[String, String]*): Request = {
    val requestUri = if (endpoint.startsWith("/")) {
      endpoint
    } else {
      constructRequestUri(endpoint)
    }
    val request = Request(requestUri, params: _*)
    request.method = method
    content.map { c => request.write(c) }
    headers.foreach { pair => request.headerMap.put( pair._1, pair._2 ) }
    if (!request.headerMap.contains(Fields.ContentType)) {
      request.headerMap.add(Fields.ContentType, "application/json; charset=utf-8" )
    }
    authorization.foreach { auth => request.headerMap.add(Fields.Authorization, auth) }
    request.host = clientHost
    request
  }

  def handle( f: TFuture[Response], successCodes: List[Int]=List(200, 201) ): Future[Response] = {
    val p = Promise[Response]()
    f.onSuccess { resp =>
      if ( resp.statusCode == 401 ) {
        p.failure(Unauthorized(resp.status, "Invalid username or password."))
      } else if ( resp.statusCode == 403 ) {
        p.failure(Forbidden(resp.status, "Not Authorized to perform this action!"))
      } else if ( resp.statusCode == 404 ) {
        p.failure(NotFound(resp.status, "Requested resource could not be found."))
      } else {
        if (resp.statusCode / 100 == 2) {
          p.completeWith(Future { resp })
        } else {
          p.failure(UnknownResponse(resp.status, resp.contentString))
        }
      }
    }.onFailure { ex =>
      p.failure(ex)
    }
    p.future
  }

  def handleBinary( f: TFuture[Response] ): Future[(Status, Buf)] = {
    val p = Promise[(Status, Buf)]()
    f.onSuccess { resp =>
      if ( resp.statusCode == 401 ) {
        p.failure(Unauthorized(resp.status, "Invalid username or password."))
      } else if ( resp.statusCode == 403 ) {
        p.failure(Forbidden(resp.status, "Not Authorized to perform this action!"))
      } else if ( resp.statusCode == 404 ) {
        p.failure(NotFound(resp.status, "Requested resource could not be found."))
      } else {
        if (resp.statusCode / 100 == 2) {
          p.completeWith(Future { (resp.status, resp.content) })
        } else {
          p.failure(UnknownBinaryResponse(resp.status))
        }
      }
    }.onFailure { ex =>
      p.failure(ex)
    }
    p.future
  }

  //
  // Apps
  //

  /**
    * Get the list of running applications. Several filters can be applied via the following
    * query parameters.
    *
    * @param embed Embeds nested resources that match the supplied path. You can specify this
    *              parameter multiple times with different values.
    *              - apps.tasks embed all tasks of each application
    *                Note: if this embed is definded, it automatically sets apps.deployments but
    *                this will change in a future release. Please define all embeds explicit.
    *              - apps.counts embed all task counts (tasksStaged, tasksRunning, tasksHealthy,
    *                tasksUnhealthy)
    *                Note: currently embedded by default but this will change in a future release.
    *                Please define all embeds explicit.
    *              - apps.deployments embed all deployment identifier, if the related app currently
    *                is in deployment.
    *              - apps.lastTaskFailure embeds the lastTaskFailure for the application
    *                if there is one.
    *              - apps.failures Shorthand for apps.lastTaskFailure, apps.tasks, apps.counts
    *                and apps.deployments.
    *                Note: deprecated and will be removed in future versions Please define
    *                all embeds explicit.
    *              - apps.taskStats exposes task statistics in the JSON.
    * @param labelSelector A label selector query contains one or more label selectors, which are
    *                      comma separated. Marathon supports three types of selectors
    *                      existence-based, equality-based and set-based. In the case of multiple
    *                      selectors, all must be satisfied so comma separator acts
    *                      as an AND logical operator. Labels and values must consist
    *                      of alphanumeric characters plus - _ and . -A-Za-z0-9_.. Any other
    *                      character is possible, but must be escaped with a backslash character.
    * @param idSelector Filter the result to only return apps whose id is or contains the
    *                   given value.
    * @param cmdSelector Filter the result to only return apps whose cmd field contains the
    *                    given value.
    * @return list of applications
    */
  def getApps( embed: List[ApplicationEmbedTypes.ApplicationEmbedType] = List.empty[ApplicationEmbedTypes.ApplicationEmbedType],
               labelSelector: Option[String] = None,
               idSelector: Option[String] = None,
               cmdSelector: Option[String] = None ): Future[List[Application]] = {
    val p = Promise[List[Application]]()

    val embeds = embed.map { e => ("embed", s"apps.${e.toString}") }
    val labels = labelSelector match {
      case None => List.empty[Tuple2[String, String]]
      case Some(selector) => List(("label", selector))
    }
    val ids = idSelector match {
      case None => List.empty[Tuple2[String, String]]
      case Some(selector) => List(("id", selector))
    }
    val cmds = cmdSelector match {
      case None => List.empty[Tuple2[String, String]]
      case Some(selector) => List(("cmd", selector))
    }

    handle( request( Method.Get, "apps", embeds ++ labels ++ ids ++ cmds:_* ) ).onComplete {
      case Success(response) =>
        getAppsResponseFormat.reads(Json.parse( response.contentString )).asOpt match {
          case Some(data) => p.completeWith( Future { data.apps } )
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Get the application with id app_id. The response includes some status information besides
    * the current configuration of the app. You can specify optional embed arguments, to get more
    * embedded information.
    *
    * @param id application ID
    * @param embed Embeds nested resources that match the supplied path. You can specify this
    *              parameter multiple times with different values.
    * @return an application
    */
  def getApp( id:String, embed: List[ApplicationEmbedTypes.ApplicationEmbedType] = List.empty[ApplicationEmbedTypes.ApplicationEmbedType] ): Future[Application] = {
    val p = Promise[Application]()
    handle( request( Method.Get, s"apps/$id", embed.map { e => ("embed", s"apps.${e.toString}") }:_* ) ).onComplete {
      case Success(response) =>
        getAppResponseFormat.reads(Json.parse( response.contentString )).asOpt match {
          case Some(data) => p.completeWith( Future { data.app } )
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Create and start a new application. Note: This operation will create a deployment.
    * The operation finishes, if the deployment succeeds. You can query the deployments endoint
    * to see the status of the deployment.
    *
    * @param app an application to create
    * @return an application created
    */
  def createApp(app:Application): Future[Application] = {
    val p = Promise[Application]()
    val content = applicationFormat.writes(app).toString().getBytes(StandardCharsets.UTF_8)
    handle( request( Method.Post, "apps", content ) ).onComplete {
      case Success(response) =>
        applicationFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith( Future { data } )
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Change multiple applications either by upgrading existing ones or creating new ones. If there
    * is an update to an already running application, the application gets upgraded. All instances
    * of this application get replaced by the new version. The order of dependencies will be applied
    * correctly. The upgradeStrategy defines the behaviour of the upgrade. If the id of
    * the application is not known, the application gets started. The order of dependencies will be
    * applied correctly. It is possible to mix upgrades and installs. If you have more complex
    * scenarios with upgrades, use the groups endpoint.
    * Note: This operation will create a deployment. The operation finishes, if the deployment
    * succeeds. You can query the deployments endpoint to see the status of the deployment.
    *
    * @param apps applications to update
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return deployment ID
    */
  def updateApps(apps:List[Application], force: Boolean = false): Future[DeploymentIdentifier] = {
    val p = Promise[DeploymentIdentifier]()
    val content = Json.toJson(apps).toString().getBytes(StandardCharsets.UTF_8)
    handle( request( Method.Put, "apps", Map.empty[String, String], Some(content), ("force", force.toString) ) ).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith( Future { data } )
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Replaces parameters of a running application. If no application with the given id exists,
    * it will be created. If there is an application with this id, all running instances get
    * upgraded to the new definition.
    * Note: This operation will create a deployment. The operation finishes, if the deployment
    * succeeds. You can query the deployments endoint to see the status of the deployment.
    *
    * @param app Application to update
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return deployment ID
    */
  def updateApp(app:Application, force: Boolean = false): Future[DeploymentIdentifier] = {
    val p = Promise[DeploymentIdentifier]()
    val content = applicationFormat.writes(app).toString().getBytes(StandardCharsets.UTF_8)
    handle( request( Method.Put, s"apps/${app.id}", Map.empty[String, String], Some(content), ("force", force.toString) ) ).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith( Future { data } )
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Destroy an application. All data about that application will be deleted.
    * Note: This operation will create a deployment. The operation finishes, if the deployment
    * succeeds. You can query the deployments endoint to see the status of the deployment.
    *
    * @param appId application ID of an app to destroy
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return deployment ID
    */
  def deleteApp(appId:String, force:Boolean = false): Future[DeploymentIdentifier] = {
    val p = Promise[DeploymentIdentifier]()
    handle( request( Method.Delete, s"apps/$appId", ("force", force.toString) ) ).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Restart all tasks of this application.
    *
    * @param appId application ID of which the tasks should be restarted
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return deployment ID
    */
  def restartApp(appId:String, force:Boolean = false): Future[DeploymentIdentifier] = {
    val p = Promise[DeploymentIdentifier]()
    handle( request( Method.Post, s"apps/$appId/restart", ("force", force.toString) ) ).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * List all running tasks for application app_id.
    *
    * @param appId application ID
    * @return list of tasks
    */
  def getAppTasks(appId:String): Future[List[Task]] = {
    val p = Promise[List[Task]]()
    handle( request( Method.Get, s"apps/$appId/tasks" ) ).onComplete {
      case Success(response) =>
        getTasksResponseFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data.tasks })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Kill tasks that belong to the application app_id.
    *
    * @param appId application ID
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return If scale=false, all tasks that were killed are returned.
    *         If scale=true, then a deployment is triggered and the deployment is returned.
    */
  def deleteAppTasks( appId: String, force: Boolean = false ): Future[List[MarathonApiObject]] = {
    val p = Promise[List[MarathonApiObject]]()
    handle( request( Method.Delete, s"apps/$appId/tasks", ("force", force.toString)) ).onComplete {
      case Success(response) =>
        if (force) {
          deploymentIdentifierFormat.reads(Json.parse( response.contentString )).asOpt match {
            case Some(data) => p.completeWith( Future { List(data) } )
            case None => p.failure(InvalidResponse(response.status, response.contentString))
          }
        } else {
          getTasksResponseFormat.reads(Json.parse( response.contentString )).asOpt match {
            case Some(data) => p.completeWith( Future { data.tasks } )
            case None => p.failure(InvalidResponse(response.status, response.contentString))
          }
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Kill the task with ID task_id that belongs to the application app_id.
    *
    * @param appId application ID
    * @param taskId task ID
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return If scale=false, all tasks that were killed are returned.
    *         If scale=true, then a deployment is triggered and the deployment is returned.
    */
  def deleteAppTask( appId: String, taskId: String, force: Boolean = false ): Future[List[MarathonApiObject]] = {
    val p = Promise[List[MarathonApiObject]]()
    handle( request( Method.Delete, s"apps/$appId/tasks/$taskId", ("force", force.toString)) ).onComplete {
      case Success(response) =>
        if (force) {
          deploymentIdentifierFormat.reads(Json.parse( response.contentString )).asOpt match {
            case Some(data) => p.completeWith( Future { List(data) } )
            case None => p.failure(InvalidResponse(response.status, response.contentString))
          }
        } else {
          getTasksResponseFormat.reads(Json.parse( response.contentString )).asOpt match {
            case Some(data) => p.completeWith( Future { data.tasks } )
            case None => p.failure(InvalidResponse(response.status, response.contentString))
          }
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * List the versions of the application with id app_id.
    *
    * @param appId application ID
    * @return list of versions
    */
  def getAppVersions(appId:String): Future[List[DateTime]] = {
    val p = Promise[List[DateTime]]()
    handle( request( Method.Get, s"apps/$appId/versions" ) ).onComplete {
      case Success(response) =>
        getAppVersionsResponseFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data.versions })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * List the configuration of the application with id app_id at version version.
    *
    * @param appId application ID
    * @param version versions
    * @return an application configuration
    */
  def getAppVersion(appId:String, version: DateTime): Future[Application] = {
    val p = Promise[Application]()
    handle( request( Method.Get, s"apps/$appId/versions/${version.toString(VersionUtils.format)}" ) ).onComplete {
      case Success(response) =>
        applicationFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Deployments
  //

  /**
    * List all running deployments. A deployment is a change in the service setup. A deployment is
    * identified by an id, affects a set of applications and is composed of deployment steps. Every
    * step contains a list of actions with following types:
    * - <pre>StartApplication</pre> starts an application, which is currently not running.
    * - <pre>StopApplication</pre> stops an already running application.
    * - <pre>ScaleApplication</pre> changes the number of instances of an application and allows to
    *                               kill specified instances while scaling.
    * - <pre>RestartApplication</pre> upgrades an already deployed application with a new version.
    * - <pre>ResolveArtifacts</pre> Resolve all artifacts of an application
    *
    * @return list of deployments
    */
  def getDeployments(): Future[List[Deployment]] = {
    val p = Promise[List[Deployment]]()
    handle( request( Method.Get, "deployments" ) ).onComplete {
      case Success(response) =>
        Json.parse(response.contentString).asOpt[List[Deployment]] match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Revert the deployment with deployment_id by creating a new deployment which reverses
    * all changes.
    *
    * @param id deployment ID
    * @param force If set to false (the default) then the deployment is canceled and a new
    *              deployment is created to revert the changes of this deployment. Without
    *              concurrent deployments, this restores the configuration before this deployment.
    *              If set to true, then the deployment is still canceled but no rollback deployment
    *              is created.
    * @return a deployment ID
    */
  def deleteDeployment(id: String, force: Boolean = false): Future[DeploymentIdentifier] = {
    val p = Promise[DeploymentIdentifier]()
    handle( request( Method.Delete, s"deployments/$id", ("force", force.toString) ) ).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Groups
  //

  /**
    * Get the group with all applications and all transitive child groups.
    *
    * @param id group ID
    * @return a group
    */
  def getGroups(id: Option[String]): Future[ Group ] = {
    val p = Promise[Group]()
    val endpoint = id match {
      case Some(value) => s"groups/$value"
      case None => "groups"
    }
    handle(request( Method.Get, endpoint )).onComplete {
      case Success(response) =>
        groupFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Create and start a new application group. Application groups can contain other application groups.
    *
    * @param group a group to create
    * @param id optional group ID
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return a deployment ID
    */
  def createGroup(group: Group, id: Option[String], force: Boolean = false): Future[ DeploymentIdentifier ] = {
    val p = Promise[DeploymentIdentifier]()
    val endpoint = id match {
      case Some(value) => s"groups/$value"
      case None => "groups"
    }
    val content = groupFormat.writes(group).toString().getBytes(StandardCharsets.UTF_8)
    handle(request( Method.Post, endpoint, Map.empty[String, String], Some(content), ("force", force.toString) )).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Change parameters of a deployed application group. The new group parameters get applied.
    *
    * @param group a group to change
    * @param id optional group ID
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return a deployment ID
    */
  def updateGroup(group: Group, id: Option[String], force: Boolean = false): Future[ DeploymentIdentifier ] = {
    val p = Promise[DeploymentIdentifier]()
    val endpoint = id match {
      case Some(value) => s"groups/$value"
      case None => "groups"
    }
    val content = groupFormat.writes(group).toString().getBytes(StandardCharsets.UTF_8)
    handle(request( Method.Put, endpoint, Map.empty[String, String], Some(content), ("force", force.toString) )).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Destroy a group. All data about that group and all associated applications will be deleted.
    * The failure or success of the action is signalled via events. There is a group_change_success
    * and group_change_failed with the given version.
    *
    * @param id ID of a group to destroy
    * @param force Only one deployment can be applied to one application at the same time.
    *              If the existing deployment should be canceled by this change, you can
    *              set force=true. Caution: setting force=true will cancel the current deployment.
    *              This parameter should be used only, if the current deployment is unsuccessful!.
    * @return a deployment ID
    */
  def deleteGroup(id: Option[String], force: Boolean = false): Future[ DeploymentIdentifier ] = {
    val p = Promise[DeploymentIdentifier]()
    val endpoint = id match {
      case Some(value) => s"groups/$value"
      case None => "groups"
    }
    handle(request( Method.Delete, endpoint, ("force", force.toString) )).onComplete {
      case Success(response) =>
        deploymentIdentifierFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * List all versions the group with the specified ID.
    *
    * @param id group ID
    * @return list of versions
    */
  def getGroupVersions(id: Option[String]): Future[List[DateTime]] = {
    val p = Promise[List[DateTime]]()
    val endpoint = id match {
      case Some(value) => s"groups/$value/versions"
      case None => "groups/versions"
    }
    handle(request( Method.Get, endpoint )).onComplete {
      case Success(response) =>
        Json.parse(response.contentString).asOpt[List[DateTime]] match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Tasks
  //

  /**
    * List all running tasks.
    *
    * @return list of tasks
    */
  def getTasks(status: String*): Future[List[Task]] = {
    val p = Promise[List[Task]]()
    handle( request( Method.Get, "tasks", status.map { v => ("status", v) }: _*) ).onComplete {
      case Success(response) =>
        getTasksResponseFormat.reads(Json.parse( response.contentString )).asOpt match {
          case Some(data) => p.completeWith( Future { data.tasks } )
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Kill a list of running tasks.
    *
    * @param force Only one deployment can be applied to one application at the same time. If
    *              the existing deployment should be canceled by this change, you can set
    *              force=true. Caution: setting force=true will cancel the current deployment.
    *              This paramter should be used only, if the current deployment is unsuccessful!
    * @param scale If scale=true is specified, then the related application is scaled down by
    *              the number of killed tasks. Only possible if wipe=false or not specified.
    * @param wipe  If wipe=true is specified and the app uses local persistent volumes,
    *              associated dynamic reservations will be unreserved, and persistent volumes
    *              will be destroyed. Only possible if scale=false or not specified.
    * @return If scale=false, all tasks that were killed are returned.
    *         If scale=true, than a deployment is triggered and the deployment is returned.
    */
  def deleteTasks( force: Boolean = false, scale: Boolean = false, wipe: Boolean = false ): Future[List[MarathonApiObject]] = {
    val p = Promise[List[MarathonApiObject]]()
    handle( request( Method.Post, "tasks/delete", ("force", force.toString), ("scale", scale.toString), ("wipe", wipe.toString)) ).onComplete {
      case Success(response) =>
        if (force) {
          deploymentIdentifierFormat.reads(Json.parse( response.contentString )).asOpt match {
            case Some(data) => p.completeWith( Future { List(data) } )
            case None => p.failure(InvalidResponse(response.status, response.contentString))
          }
        } else {
          getTasksResponseFormat.reads(Json.parse( response.contentString )).asOpt match {
            case Some(data) => p.completeWith( Future { data.tasks } )
            case None => p.failure(InvalidResponse(response.status, response.contentString))
          }
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Artifacts
  //

  /**
    * Upload an artifact to the artifact store. A multipart form upload request has to be performed.
    * The form parameter name has to be file. The filename used in the artifact store, is the same
    * as given by the form parameter. The response holds the URL of the artifact in the artifact
    * store in the Location Header.
    *
    * @param content file content
    * @param fileName name of the file as in the file field
    * @param path path to upload the file to, the value of / will be used if None
    * @param secure is this a https upload?
    * @return the value of the Location header, if upload was successful
    */
  def uploadArtifact(content: Array[Byte], fileName: String, path:Option[String]=None, secure: Boolean = false): Future[(Status, Option[String])] = {
    val p = Promise[(Status, Option[String])]()
    val protocol = if (secure) "https://" else "http://"
    val pathElement = path match {
      case Some(path) => if (path.startsWith("/")) path else s"/$path"
      case None       => ""
    }
    val request = RequestBuilder().url(s"$protocol$clientHost/$version/artifacts$pathElement")
                    .add(FileElement(("file"), Buf.ByteArray.Owned(content), Some("application/octet-stream"), Some(fileName)))
                    .buildFormPost(multipart = true)
    cli(request).onSuccess { resp =>
      p.completeWith(Future{ (resp.status, resp.headerMap.get(Fields.Location)) })
    }.onFailure { ex =>
      p.failure(ex)
    }
    p.future
  }

  /**
    * Delete an artifact from the artifact store. The path is the relative path in the artifact store.
    *
    * @param path full path of the file to remove
    * @param secure is this a https delete?
    * @return HTTP status of the operation
    */
  def deleteArtifact( path: String, secure: Boolean = false ): Future[Status] = {
    val p = Promise[Status]()
    val pathElement = if (path == "") "" else { if (path.startsWith("/")) path else s"/$path" }
    handle( request( Method.Delete, s"artifacts$pathElement") ).onComplete {
      case Success(response) => p.completeWith(Future{ response.status })
      case Failure(ex) =>
      p.failure(ex)
    }
    p.future
  }

  /**
    * Download an artifact from the artifact store. The path is the relative path in the artifact
    * store.
    *
    * @param path full path of the file to download
    * @param secure is this a secure download?
    * @return file contents
    */
  def getArtifact( path: String, secure: Boolean = false ): Future[Array[Byte]] = {
    val p = Promise[Array[Byte]]()
    val pathElement = if (path == "") "" else { if (path.startsWith("/")) path else s"/$path" }
    handleBinary( request( Method.Get, s"artifacts$pathElement") ).onComplete {
      case Success((_, body)) =>
        var buffer = new Array[Byte](body.length)
        body.write(buffer, 0)
        p.completeWith(Future{ buffer })
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Events
  //

  private def fromReader(reader: Reader): AsyncStream[Buf] =
    AsyncStream.fromFuture(reader.read(Int.MaxValue)).flatMap {
      case None => AsyncStream.empty
      case Some(a) => a +:: fromReader(reader)
    }

  /**
    * Attach to the marathon event stream. To use this endpoint, the client has to accept
    * the text/event-stream content type. Please note a request to this endpoint will not be
    * closed by the server. If an event happens on the server side, this event will be propagated
    * to the client immediately.
    *
    * @param limit Maximum number of messages to receive
    * @param onMessage message receive callback function
    */
  def streamEvents( limit: Int = 0 )( onMessage: ( String ) => Unit ): Unit = {
    var received = 0
    val streamingCli: Service[Request, Response] = Http.client.withStreaming(enabled = true).withRequestTimeout(requestTimeout.milliseconds).newService(clientHost)
    val headers = Map(
      Fields.Accept -> "text/event-stream",
      Fields.AcceptEncoding -> "gzip, deflate")
    streamingCli( buildRequest( Method.Get, "events", headers, None ) ).onSuccess { response =>
      fromReader(response.reader).foreach {
        case Buf.Utf8(buf) if limit == 0 || ( limit > 0 && received < limit ) =>
          onMessage.apply( new String(buf.getBytes, StandardCharsets.UTF_8) )
          received += 1
        case _ =>
          streamingCli.close()
      }
    }
  }

  //
  // Event subscriptions
  //

  /**
    * List all event subscriber callback URLs. NOTE To activate this endpoint, you need to startup
    * a Marathon instance with --event_subscriber http_callback
    *
    * @return list of callback URIs
    */
  def getEventSubscriptions(): Future[List[String]] = {
    val p = Promise[List[String]]()
    handle( request( Method.Get, "eventSubscriptions" ) ).onComplete {
      case Success(response) =>
        getEventSubscriptionsResponseFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future { data.callbackUrls })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Subscribe to the event callback mechanism with the specified callback URL.
    *
    * @param callbackUri callback URI
    * @return event in case of successful subscription, error otherwise
    */
  def createEventSubscription(callbackUri: URI): Future[EventSubscriptionSubscribeEvent] = {
    val p = Promise[EventSubscriptionSubscribeEvent]()
    handle( request( Method.Post, "eventSubscriptions", ("callbackUrl", callbackUri.toString) ) ).onComplete {
      case Success(response) =>
        eventSubscriptionSubscribeEventFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(event) => p.completeWith(Future{ event })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Unregister a callback URL from the event subscribers list.
    *
    * @param callbackUri callback URI
    * @return HTTP status of the operation
    */
  def deleteEventSubscription(callbackUri: URI): Future[Status] = {
    val p = Promise[Status]()
    handle(request(Method.Delete, "eventSubscriptions", ("callbackUrl", callbackUri.toString))).onComplete {
      case Success(response) => p.completeWith(Future{ response.status })
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Info
  //

  /**
    * Get info about the Marathon Instance
    *
    * @return Marathon information.
    */
  def getInfo(): Future[GetInfoResponse] = {
    val p = Promise[GetInfoResponse]()
    handle( request( Method.Get, "info" ) ).onComplete {
      case Success(response) =>
        getInfoResponseFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Leader
  //

  /**
    * Returns the current leader.
    *
    * @return leader information
    */
  def getLeader(): Future[Option[GetLeaderResponse]] = {
    val p = Promise[Option[GetLeaderResponse]]()
    handle( request( Method.Get, "leader") ).onComplete {
      case Success(response) =>
        p.completeWith( Future{ getLeaderInfoFormat.reads(Json.parse( response.contentString )).asOpt } )
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Causes the current leader to abdicate, triggering a new election.
    *
    * @return HTTP status of the operation
    */
  def deleteLeader(): Future[Status] = {
    val p = Promise[Status]()
    handle( request( Method.Delete, "leader" ) ).onComplete {
      case Success(response) => p.completeWith( Future { response.status } )
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // Plugins
  //

  /**
    * Returns the list of all loaded plugins.
    *
    * @return list of plugins
    */
  def getPlugins(): Future[List[Plugin]] = {
    val p = Promise[List[Plugin]]()
    handle( request( Method.Get, "plugins" ) ).onComplete {
      case Success(response) =>
        getPluginsResponseFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data.plugins })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Request is handled by the plugin.
    *
    * @param method HTTP method
    * @param pluginId ID of the plugin
    * @param path request path
    * @param content data to send with the request
    * @param headers request headers
    *
    * @return response
    */
  def pluginExecuteRequest(method: Method, pluginId: String, path: Option[String]=None, content: Option[Array[Byte]]=None, headers: Option[Map[String, String]]=None): Future[Response] = {
    val p = Promise[Response]()
    if (List(Method.Get, Method.Put, Method.Post, Method.Delete).contains( method ) ) {
      val pathElement = path match {
        case Some(value) => if (value.startsWith("/")) value else s"/$value"
        case None => ""
      }
      handle( request( method,
                       s"plugins/$pluginId$pathElement",
                       headers.getOrElse( Map.empty[String, String] ),
                       content ) ).onComplete {
        case Success(response) => p.completeWith(Future{ response })
        case Failure(ex) => p.failure(ex)
      }
    } else {
      p.failure(NotAllowed(Status.MethodNotAllowed, s"Marathon does not support ${method} requests for plugins."))
    }
    p.future
  }

  //
  // Queue
  //

  /**
    * List all the tasks queued up or waiting to be scheduled. This is mainly used for
    * troubleshooting and occurs when scaling changes are requested and the volume of scaling
    * changes out paces the ability to schedule those tasks. In addition to the application
    * in the queue, you see also the task count that needs to be started. If the task has
    * a rate limit, then a delay to the start gets applied. You can see this delay for every
    * application with the seconds to wait before the next launch will be tried.
    *
    * @return Queue items.
    */
  def getQueue(): Future[List[QueueItem]] = {
    val p = Promise[List[QueueItem]]()
    handle( request( Method.Get, "queue" ) ).onComplete {
      case Success(response) =>
        getQueueResponseFormat.reads(Json.parse(response.contentString)).asOpt match {
          case Some(data) => p.completeWith(Future{ data.queue })
          case None => p.failure(InvalidResponse(response.status, response.contentString))
        }
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * If an application fails too often in a specified amount of time (according to the application
    * definition), the task launch will be delayed. This delay can be removed by calling this
    * endpoint. The effect is, that the tasks of this application will be launched immediately.
    *
    * @param appId Application ID
    * @return Status, Ok if success, NotFound if application with given ID not found
    */
  def deleteQueue(appId: String): Future[Status] = {
    val p = Promise[Status]()
    handle(request(Method.Delete, s"queue/$appId/delay")).onComplete {
      case Success(response) =>
        p.completeWith(Future{ response.status })
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  //
  // General
  //

  /**
    * Ping this Marathon instance.
    *
    * @return Status of the request. Ok means everything is fine.
    */
  def ping(): Future[Status] = {
    val p = Promise[Status]()
    handle( request( Method.Get, "/ping") ).onComplete {
      case Success(response) =>
        p.completeWith( Future { response.status } )
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

  /**
    * Get metrics data from this Marathon instance.
    *
    * @return All aggregated runtime metrics for a Marathon instance.
    */
  def metrics(): Future[JsValue] = {
    val p = Promise[JsValue]()
    handle( request( Method.Get, "/metrics") ).onComplete {
      case Success(response) =>
        p.completeWith( Future { Json.parse(response.contentString) } )
      case Failure(ex) => p.failure(ex)
    }
    p.future
  }

}
