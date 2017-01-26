/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2

import uk.co.appministry.scathon.models.mesos.Constraint
import uk.co.appministry.scathon.models.v2.util.ApplicationUtils
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Application(
           val id: String,
           val cmd: Option[String] = None,
           val args: Option[List[String]] = None,
           val cpus: Double = 1,
           val mem: Double = 1024,
           val disk: Double = 0,
           @deprecated("Use portDefinitions.", "v0.16.0") val ports: Option[List[Int]] = None,
           val portDefinitions: Option[List[PortDefinition]] = None,
           val requirePorts: Boolean = false,
           val instances: Int = 1,
           val executor: Option[String] = None,
           val container: Option[Container] = None,
           val env: Map[String, String] = Map.empty[String, String],
           val constraints: Option[List[Constraint]] = None,
           val acceptedResourceRoles: Option[List[String]] = None,
           val labels: Map[String, String] = Map.empty[String, String],
           val fetch: Option[List[FetchUri]] = None,
           @deprecated("Use fetch.", "v0.16.0") val uris: Option[List[String]] = None,
           val dependencies: Option[List[String]] = None,
           val healthChecks: Option[List[HealthCheck]] = None,
           val readinessChecks: Option[List[ReadinessCheck]] = None,
           val ipAddress: Option[ApplicationIpAddress] = None,
           val backoffSeconds: Int = 1,
           val backoffFactor: Double = 1.15,
           val maxLaunchDelaySeconds: Int = 3600,
           val updateStrategy: Option[UpdateStrategy] = None,
           val user: Option[String] = None,
           // Returned when requesting an app:
           val tasks: Option[List[Task]] = None,
           val tasksRunning: Option[Int] = Some(0),
           val tasksStaged: Option[Int] = Some(0),
           val version: Option[DateTime] = None,
           val lastTaskFailure: Option[LastTaskFailure] = None ) extends MarathonApiObject

case class ApplicationIpAddress(
           val groups: Option[List[String]] = None,
           val labels: Option[Map[String, String]] = None,
           val discovery: Option[ApplicationIpAddressDiscovery] = None ) extends MarathonApiObject

case class ApplicationIpAddressDiscovery(
           val ports: List[ApplicationIpAddressDiscoveryPort] = List.empty[ApplicationIpAddressDiscoveryPort] ) extends MarathonApiObject

case class ApplicationIpAddressDiscoveryPort(
           val number: Int,
           val name: String,
           val protocol: PortMappingTypes.PortMappingType ) extends MarathonApiObject

case class Container(
           val docker: Option[ContainerDocker],
           val volumes: List[ContainerVolume] = List.empty[ContainerVolume],
           val `type`: ContainerTypes.ContainerType = ContainerTypes.DOCKER ) extends MarathonApiObject

case class ContainerDocker(
           val image: String,
           val network: DockerNetworkTypes.DockerNetworkType = DockerNetworkTypes.BRIDGE,
           val forcePullImage: Boolean = false,
           val portMappings: Option[List[ContainerPortMapping]] = None ) extends MarathonApiObject

case class ContainerPortMapping(
           val containerPort: Int,
           val hostPort: Int,
           val servicePort: Option[Int] = None,
           val protocol: PortMappingTypes.PortMappingType ) extends MarathonApiObject

case class ContainerVolume(
           val containerPath:String,
           val hostPath: String,
           val mode: String = "RO" ) extends MarathonApiObject

case class FetchUri(
           val uri: String,
           val executable: Boolean = false,
           val extract: Boolean = true,
           val cache: Boolean = true ) extends MarathonApiObject

case class Group(
           val id:String,
           val apps: List[Application] = List.empty[Application],
           val groups: List[Group] = List.empty[Group],
           val dependencies: List[String] = List.empty[String],
           val version: DateTime) extends MarathonApiObject

case class LastTaskFailure(
           val appId: String,
           val host: String,
           val message: String,
           val state: StatusUpdateEventTypes.StatusUpdateEventType,
           val taskId: String,
           val timestamp: DateTime,
           val version: DateTime ) extends MarathonApiObject

case class DeploymentIdentifier(
           val deploymentId: String,
           val version: DateTime ) extends MarathonApiObject

case class Plugin(
           val id: String,
           val plugin: String,
           val implementation: String,
           val tags: Option[List[String]] = None,
           val configuration: Option[JsValue] = None,
           val info: Option[Map[String, JsValue]] = None ) extends MarathonApiObject

case class PortDefinition(
           val port: Int,
           val protocol: PortMappingTypes.PortMappingType,
           val name: String,
           val labels: Map[String, String] = Map.empty[String, String] ) extends MarathonApiObject

case class ReadinessCheck(
           val name: Option[String] = Some("readinessCheck"),
           val protocol: Option[ProtocolTypes.ProtocolType] = Some(ProtocolTypes.HTTP),
           val path: Option[String] = Some("/"),
           val portName: Option[String] = Some("http-api"),
           val intervalSeconds: Option[Int] = Some(30),
           val timeoutSeconds: Option[Int] = Some(10),
           val httpStatusCodesForReady: Option[List[Int]] = Some(List(200)),
           val preserveLastResponse: Option[Boolean] = Some(false) ) extends MarathonApiObject

case class Task(
           val id: String,
           val host: String,
           val ports: List[Int],
           val servicePorts: List[Int],
           val startedAt: Option[DateTime] = None,
           val stagedAt: Option[DateTime] = None,
           val version: DateTime,
           val appId: String,
           val slaveId: String ) extends MarathonApiObject

case class UpdateStrategy(
           val minimumHealthCapacity: Double,
           val maximumOverCapacity: Double ) extends MarathonApiObject

//
// Parsers:
//

trait ApplicationParser {
  implicit val applicationFormat: Format[Application] = ApplicationUtils.applicationFormat
}

trait ApplicationIpAddressParser extends ApplicationIpAddressDiscoveryParser {
  implicit val applicationIpAddressFormat: Format[ApplicationIpAddress] = (
    ( __ \ "groups" ).formatNullable[List[String]] and
    ( __ \ "labels" ).formatNullable[Map[String, String]] and
    ( __ \ "discovery" ).formatNullable[ApplicationIpAddressDiscovery]
  )(ApplicationIpAddress.apply, unlift(ApplicationIpAddress.unapply))
}

trait ApplicationIpAddressDiscoveryParser extends ApplicationIpAddressDiscoveryPortParser {
  implicit val applicationIpAddressDiscoveryFormat: Format[ApplicationIpAddressDiscovery] = (__ \ "ports").format[List[ApplicationIpAddressDiscoveryPort]].inmap(ApplicationIpAddressDiscovery.apply, unlift(ApplicationIpAddressDiscovery.unapply))
}

trait ApplicationIpAddressDiscoveryPortParser extends EnumParser {
  implicit val applicationIpAddressDiscoveryPortFormat: Format[ApplicationIpAddressDiscoveryPort] = (
    ( __ \ "number" ).format[Int] and
    ( __ \ "name" ).format[String] and
    ( __ \ "protocol" ).format[PortMappingTypes.PortMappingType]
  )(ApplicationIpAddressDiscoveryPort.apply, unlift(ApplicationIpAddressDiscoveryPort.unapply))
}

trait ContainerParser extends ContainerDockerParser with ContainerVolumeParser with EnumParser {
  implicit val containerFormat: Format[Container] = (
    ( __ \ "docker" ).formatNullable[ContainerDocker] and
    ( __ \ "volumes" ).format[List[ContainerVolume]] and
    ( __ \ "type" ).format[ContainerTypes.ContainerType]
  )(Container.apply, unlift(Container.unapply))
}

trait ContainerDockerParser extends ContainerPortMappingParser with EnumParser {
  implicit val containerDockerFormat: Format[ContainerDocker] = (
    ( __ \ "image" ).format[String] and
    ( __ \ "network" ).format[ DockerNetworkTypes.DockerNetworkType ] and
    ( __ \ "forcePullImage" ).format[Boolean] and
    ( __ \ "portMappings" ).formatNullable[List[ContainerPortMapping]]
  )(ContainerDocker.apply, unlift(ContainerDocker.unapply))
}

trait ContainerPortMappingParser extends EnumParser {
  implicit val containerPortMappingFormat: Format[ContainerPortMapping] = (
    ( __ \ "containerPort" ).format[Int] and
    ( __ \ "hostPort" ).format[Int] and
    ( __ \ "servicePort" ).formatNullable[Int] and
    ( __ \ "protocol" ).format[ PortMappingTypes.PortMappingType ]
  )(ContainerPortMapping.apply, unlift(ContainerPortMapping.unapply))
}

trait ContainerVolumeParser {
  implicit val containerVolumeFormat: Format[ContainerVolume] = (
    ( __ \ "containerPath" ).format[String] and
    ( __ \ "hostPath" ).format[String] and
    ( __ \ "mode" ).format[String]
  )(ContainerVolume.apply, unlift(ContainerVolume.unapply))
}

trait DeploymentIdentifierParser extends VersionParser {
  implicit val deploymentIdentifierFormat: Format[DeploymentIdentifier] = (
    ( __ \ "deploymentId" ).format[String] and
    ( __ \ "version" ).format[DateTime]
  )(DeploymentIdentifier.apply, unlift(DeploymentIdentifier.unapply))
}

trait FetchUriParser {
  implicit val fetchUriFormat: Format[FetchUri] = (
    ( __ \ "uri" ).format[String] and
    ( __ \ "executable" ).format[Boolean] and
    ( __ \ "extract" ).format[Boolean] and
    ( __ \ "cache" ).format[Boolean]
  )(FetchUri.apply, unlift(FetchUri.unapply))
}

trait GroupParser extends ApplicationParser with VersionParser {
  implicit val groupFormat: Format[Group] = (
    ( __ \ "id" ).format[String] and
    ( __ \ "apps" ).format[List[Application]] and
    ( __ \ "groups" ).lazyFormat( implicitly[Format[List[Group]]] ) and
    ( __ \ "dependencies" ).format[List[String]] and
    ( __ \ "version" ).format[DateTime]
  )(Group.apply, unlift(Group.unapply))
}

trait LastTaskFailureParser extends VersionParser with EnumParser {
  implicit val lastTaskFailureFormat: Format[LastTaskFailure] = (
    ( __ \ "appId" ).format[String] and
    ( __ \ "host" ).format[String] and
    ( __ \ "message" ).format[String] and
    ( __ \ "state" ).format[StatusUpdateEventTypes.StatusUpdateEventType] and
    ( __ \ "taskId" ).format[String] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "version" ).format[DateTime]
  )(LastTaskFailure.apply, unlift(LastTaskFailure.unapply))
}

trait PluginParser {
  implicit val pluginFormat: Format[Plugin] = (
    ( __ \ "id" ).format[String] and
    ( __ \ "plugin" ).format[String] and
    ( __ \ "implementation" ).format[String] and
    ( __ \ "tags" ).formatNullable[List[String]] and
    ( __ \ "configuration" ).formatNullable[JsValue] and
    ( __ \ "info" ).formatNullable[Map[String, JsValue]]
  )(Plugin.apply, unlift(Plugin.unapply))
}

trait PortDefinitionParser extends EnumParser {
  implicit val portDefinitionFormat: Format[PortDefinition] = (
    ( __ \ "port" ).format[Int] and
    ( __ \ "protocol" ).format[PortMappingTypes.PortMappingType] and
    ( __ \ "name" ).format[String] and
    ( __ \ "labels" ).format[Map[String, String]]
  )(PortDefinition.apply, unlift(PortDefinition.unapply))
}

trait ReadinessCheckParser extends EnumParser {
  implicit val readinessCheckFormat: Format[ReadinessCheck] = (
    ( __ \ "name" ).formatNullable[String] and
    ( __ \ "protocol" ).formatNullable[ProtocolTypes.ProtocolType] and
    ( __ \ "path" ).formatNullable[String] and
    ( __ \ "portName" ).formatNullable[String] and
    ( __ \ "intervalSeconds" ).formatNullable[Int] and
    ( __ \ "timeoutSeconds" ).formatNullable[Int] and
    ( __ \ "httpStatusCodesForReady" ).formatNullable[List[Int]] and
    ( __ \ "preserveLastResponse" ).formatNullable[Boolean]
  )(ReadinessCheck.apply, unlift(ReadinessCheck.unapply))
}

trait TaskParser extends VersionParser {
  implicit val taskFormat: Format[Task] = (
    ( __ \ "id" ).format[String] and
    ( __ \ "host" ).format[String] and
    ( __ \ "ports" ).format[List[Int]] and
    ( __ \ "servicePorts" ).format[List[Int]] and
    ( __ \ "startedAt" ).formatNullable[DateTime] and
    ( __ \ "stagedAt" ).formatNullable[DateTime] and
    ( __ \ "version" ).format[DateTime] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "slaveId" ).format[String]
  )(Task.apply, unlift(Task.unapply))
}

trait UpdateStrategyParser {
  implicit val updateStrategyFormat: Format[UpdateStrategy] = (
    ( __ \ "minimumHealthCapacity" ).format[Double] and
    ( __ \ "maximumOverCapacity" ).format[Double]
  )(UpdateStrategy.apply, unlift(UpdateStrategy.unapply))
}
