/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2.util

import uk.co.appministry.scathon.models.mesos.{Constraint, ConstraintParser}
import uk.co.appministry.scathon.models.v2._
import org.joda.time.DateTime
import play.api.libs.json._

case class InvalidApplicationDefinitionException(val message:String) extends Exception(message)

object ApplicationUtils extends TaskParser
                        with    ContainerParser
                        with    VersionParser
                        with    LastTaskFailureParser
                        with    PortDefinitionParser
                        with    ConstraintParser
                        with    FetchUriParser
                        with    HealthCheckParser
                        with    ReadinessCheckParser
                        with    ApplicationIpAddressParser
                        with    UpdateStrategyParser {

  def noneIfEmptyList[A]( list: Option[List[A]] ): Option[List[A]] = {
    list match {
      case None => None
      case Some(Nil) => None
      case anyOther => anyOther
    }
  }

  def applicationReads[E <: Application]: Reads[Application] = new Reads[Application] {
    def reads(json: JsValue): JsResult[Application] = {

      try {
        val obj = json.asInstanceOf[JsObject]
        (obj \ "id").toOption match {
          case Some(value) =>
            try {
              val id = value.asInstanceOf[JsString].value
              val app = Application(
                id = id,
                cmd = (obj \ "cmd").asOpt[String],
                args = noneIfEmptyList( (obj \ "args").asOpt[List[String]] ),
                cpus = (obj \ "cpus").asOpt[Double].getOrElse(ApplicationDefaults.cpus),
                mem = (obj \ "mem").asOpt[Double].getOrElse(ApplicationDefaults.mem),
                disk = (obj \ "disk").asOpt[Double].getOrElse(ApplicationDefaults.disk),
                ports = noneIfEmptyList( (obj \ "ports").asOpt[List[Int]] ),
                portDefinitions = noneIfEmptyList( (obj \ "portDefinitions").asOpt[List[PortDefinition]] ),
                requirePorts = (obj \ "requirePorts").asOpt[Boolean].getOrElse( ApplicationDefaults.requirePorts ),
                instances = (obj \ "instances").asOpt[Int].getOrElse(ApplicationDefaults.instances),
                executor = (obj \ "executor").asOpt[String],
                container = (obj \ "container").asOpt[Container],
                env = (obj \ "env").asOpt[Map[String, String]].getOrElse(Map.empty[String, String]),
                constraints = noneIfEmptyList( (obj \ "constraints").asOpt[List[Constraint]] ),
                acceptedResourceRoles = noneIfEmptyList( (obj \ "acceptedResourceRoles").asOpt[List[String]] ),
                labels = (obj \ "labels").asOpt[Map[String, String]].getOrElse(Map.empty[String, String]),
                fetch = noneIfEmptyList( (obj \ "fetch").asOpt[List[FetchUri]] ),
                uris = noneIfEmptyList( (obj \ "uris").asOpt[List[String]] ),
                dependencies = noneIfEmptyList( (obj \ "dependencies").asOpt[List[String]] ),
                healthChecks = noneIfEmptyList( (obj \ "healthChecks").asOpt[List[HealthCheck]] ),
                readinessChecks = noneIfEmptyList( (obj \ "readinessChecks").asOpt[List[ReadinessCheck]] ),
                ipAddress = (obj \ "ipAddress").asOpt[ApplicationIpAddress],
                backoffSeconds = (obj \ "backoffSeconds").asOpt[Int].getOrElse( ApplicationDefaults.backoffSeconds ),
                backoffFactor = (obj \ "backoffFactor").asOpt[Double].getOrElse( ApplicationDefaults.backoffFactor ),
                maxLaunchDelaySeconds = (obj \ "maxLaunchDelaySeconds").asOpt[Int].getOrElse( ApplicationDefaults.maxLaunchDelaySeconds ),
                updateStrategy = (obj \ "updateStrategy").asOpt[UpdateStrategy],
                user = (obj \ "user").asOpt[String],
                tasks = noneIfEmptyList( (obj \ "tasks").asOpt[List[Task]] ),
                tasksRunning = (obj \ "tasksRunning").asOpt[Int] match {
                  case Some(v) => Some(v)
                  case None => Some(0)
                },
                tasksStaged = (obj \ "tasksStaged").asOpt[Int] match {
                  case Some(v) => Some(v)
                  case None => Some(0)
                },
                version = (obj \ "version").asOpt[DateTime],
                lastTaskFailure = (obj \ "lastTaskFailure").asOpt[LastTaskFailure] )
              JsSuccess(app)
            } catch {
              case _: ClassCastException => JsError(s"Expected id to be a string but value '$value' does not look like one.")
            }
          case None =>
            JsError("Expected id property in JSON but none found.")
        }
      } catch {
        case _: ClassCastException => JsError(s"Expected an object but value '$json' does not look like one.")
      }


    }
  }

  implicit def applicationWrites[ E <: Application ]: Writes[Application] = new Writes[Application] {
    def writes(v: Application): JsValue = {

      val portsStateError = ( v.ports, v.portDefinitions ) match {
        case ( Some(_), Some(_) ) => Some(s"Application definition contains both ports and portDefinitions. This is not a valid state.")
        case _ => None
      }
      val urisStateError = ( v.fetch, v.uris ) match {
        case ( Some(_), Some(_) ) => Some(s"Application definition contains both uris and fetch. This is not a valid state.")
        case _ => None
      }

      ( portsStateError, urisStateError ) match {
        case (None, None) =>
          JsObject(List(
            Some(("id", JsString(v.id))),
            v.cmd match {
              case Some(cmd) => Some("cmd", JsString(cmd))
              case None => None
            },
            v.args match {
              case Some(args) => Some("args", JsArray(args.map { arg => JsString(arg) }))
              case None => None
            },
            Some(("cpus", JsNumber(v.cpus))),
            Some(("mem", JsNumber(v.mem))),
            Some(("disk", JsNumber(v.disk))),
            v.ports match {
              case Some(ports) => Some("ports", JsArray(ports.map { arg => JsNumber(arg) }))
              case None => None
            },
            v.portDefinitions match {
              case Some(portDefinitions) => Some("portDefinitions", JsArray(portDefinitions.map { arg => portDefinitionFormat.writes(arg) }))
              case None => None
            },
            Some(("requirePorts", JsBoolean(v.requirePorts))),
            Some(("instances", JsNumber(v.instances))),
            v.executor match {
              case Some(executor) => Some("executor", JsString(executor))
              case None => None
            },
            v.container match {
              case Some(container) => Some("container", containerFormat.writes(container))
              case None => None
            },
            Some(("env", JsObject( v.env.map { case (k, v) => ( k, JsString(v) ) } ))),
            v.constraints match {
              case Some(constraints) => Some("constraints", JsArray( constraints.map { arg => mesosAppConstraintFormat.writes(arg) } ))
              case None => None
            },
            v.acceptedResourceRoles match {
              case Some(acceptedResourceRoles) => Some("acceptedResourceRoles", JsArray( acceptedResourceRoles.map { arg => JsString(arg) } ))
              case None => None
            },
            Some(("labels", JsObject( v.labels.map { case (k, v) => ( k, JsString(v) ) } ))),
            v.fetch match {
              case Some(fetch) => Some("fetch", JsArray( fetch.map { arg => fetchUriFormat.writes(arg) } ))
              case None => None
            },
            v.uris match {
              case Some(uris) => Some("uris", JsArray( uris.map { arg => JsString(arg) } ))
              case None => None
            },
            v.dependencies match {
              case Some(dependencies) => Some("dependencies", JsArray( dependencies.map { arg => JsString(arg) } ))
              case None => None
            },
            v.healthChecks match {
              case Some(healthChecks) => Some("healthChecks", JsArray( healthChecks.map { arg => healthCheckFormat.writes(arg) } ))
              case None => None
            },
            v.readinessChecks match {
              case Some(readinessChecks) => Some("readinessChecks", JsArray( readinessChecks.map { arg => readinessCheckFormat.writes(arg) } ))
              case None => None
            },
            v.ipAddress match {
              case Some(ipAddress) => Some("ipAddress", applicationIpAddressFormat.writes(ipAddress) )
              case None => None
            },
            Some(("backoffSeconds", JsNumber(v.backoffSeconds))),
            Some(("backoffFactor", JsNumber(v.backoffFactor))),
            Some(("maxLaunchDelaySeconds", JsNumber(v.maxLaunchDelaySeconds))),
            v.updateStrategy match {
              case Some(updateStrategy) => Some("updateStrategy", updateStrategyFormat.writes(updateStrategy))
              case None => None
            },
            v.user match {
              case Some(user) => Some("user", JsString(user))
              case None => None
            },
            v.tasks match {
              case Some(tasks) => Some("tasks", JsArray( tasks.map { arg => taskFormat.writes(arg) } ))
              case None => None
            },
            v.tasksRunning match {
              case Some(tasksRunning) => Some("tasksRunning", JsNumber(tasksRunning))
              case None => None
            },
            v.tasksStaged match {
              case Some(tasksStaged) => Some("tasksStaged", JsNumber(tasksStaged))
              case None => None
            },
            v.version match {
              case Some(version) => Some("version", versionFormat.writes(version))
              case None => None
            },
            v.lastTaskFailure match {
              case Some(lastTaskFailure) => Some("lastTaskFailure", lastTaskFailureFormat.writes(lastTaskFailure))
              case None => None
            }
          ).flatten)
        case (Some(err1), _) =>
          throw new InvalidApplicationDefinitionException(err1)
        case (_, Some(err2)) =>
          throw new InvalidApplicationDefinitionException(err2)
      }

    }
  }

  implicit def applicationFormat: Format[Application] = {
    Format(ApplicationUtils.applicationReads, ApplicationUtils.applicationWrites)
  }

}
