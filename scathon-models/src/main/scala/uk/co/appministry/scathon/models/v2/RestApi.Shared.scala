/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2

import uk.co.appministry.scathon.models.util.EnumUtils
import uk.co.appministry.scathon.models.v2.util.VersionUtils
import org.joda.time.{DateTime, DateTimeZone}

trait MarathonApiObject

object EventTypes extends Enumeration {
  type EventType = Value
  val api_post_event = Value("api_post_event")
  val status_update_event = Value("status_update_event")
  val framework_message_event = Value("framework_message_event")
  val subscribe_event = Value("subscribe_event")
  val unsubscribe_event = Value("unsubscribe_event")
  val add_health_check_event = Value("add_health_check_event")
  val remove_health_check_event = Value("remove_health_check_event")
  val failed_health_check_event = Value("failed_health_check_event")
  val health_status_changed_event = Value("health_status_changed_event")
  val unhealthy_task_kill_event = Value("unhealthy_task_kill_event")
  val group_change_success = Value("group_change_success")
  val group_change_failed = Value("group_change_failed")
  val deployment_success = Value("deployment_success")
  val deployment_failed = Value("deployment_failed")
  val deployment_info = Value("deployment_info")
  val deployment_step_success = Value("deployment_step_success")
  val deployment_step_failure = Value("deployment_step_failure")
}

object StatusUpdateEventTypes extends Enumeration {
  type StatusUpdateEventType = Value
  val TASK_STAGING = Value("TASK_STAGING")
  val TASK_STARTING = Value("TASK_STARTING")
  val TASK_RUNNING = Value("TASK_RUNNING")
  val TASK_FINISHED = Value("TASK_FINISHED")
  val TASK_FAILED = Value("TASK_FAILED")
  val TASK_KILLED = Value("TASK_KILLED")
  val TASK_LOST = Value("TASK_LOST")
}

object PortMappingTypes extends Enumeration {
  type PortMappingType = Value
  val TCP = Value("tcp")
  val UDP = Value("udp")
}

object ProtocolTypes extends Enumeration {
  type ProtocolType = Value
  val HTTP = Value("HTTP")
  val HTTPS = Value("HTTPS")
}

object DockerNetworkTypes extends Enumeration {
  type DockerNetworkType = Value
  val BRIDGE = Value("BRIDGE")
  val HOST = Value("HOST")
}

object ContainerTypes extends Enumeration {
  type ContainerType = Value
  val DOCKER = Value("DOCKER")
  val MESOS = Value("MESOS")
}

object DeploymentActionTypes extends Enumeration {
  type DeploymentActionType = Value
  val START_APPLICATION = Value("StartApplication")
  val STOP_APPLICATION = Value("StopApplication")
  val SCALE_APPLICATION = Value("ScaleApplication")
  val RESTART_APPLICATION = Value("RestartApplication")
  val RESOLVE_ARTIFACTS = Value("ResolveArtifacts")
  val KILL_ALL_OLD_TASKS_OF = Value("KillAllOldTasksOf")
}

trait EnumParser {
  implicit val enumEventTypesFormat = EnumUtils.enumFormat(EventTypes)
  implicit val enumStatusUpdateEventTypesFormat = EnumUtils.enumFormat(StatusUpdateEventTypes)
  implicit val enumPortMappingTypesFormat = EnumUtils.enumFormat(PortMappingTypes)
  implicit val enumProtocolTypesFormat = EnumUtils.enumFormat(ProtocolTypes)
  implicit val enumDockerNetworkTypesFormat = EnumUtils.enumFormat(DockerNetworkTypes)
  implicit val enumContainerTypesFormat = EnumUtils.enumFormat(ContainerTypes)
  implicit val enumDeploymentActionTypesFormat = EnumUtils.enumFormat(DeploymentActionTypes)
}

object Version {
  def apply():DateTime = {
    new DateTime(DateTimeZone.UTC)
  }
  def apply(dt: DateTime):String = {
    VersionUtils.format.print(dt)
  }
  def apply(value: String): DateTime = {
    VersionUtils.format.parseDateTime(value)
  }
}

trait VersionParser {
  implicit val versionFormat = VersionUtils.dateTimeformat
}