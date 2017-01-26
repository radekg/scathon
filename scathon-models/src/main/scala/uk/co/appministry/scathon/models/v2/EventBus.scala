/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait MarathonEventBusObject extends MarathonApiObject

case class Event(
           val eventType: EventTypes.EventType,
           val timestamp:String ) extends MarathonEventBusObject

case class ApiPostEvent(
           val eventType: EventTypes.EventType = EventTypes.api_post_event,
           val timestamp: DateTime,
           val clientIp: String,
           val uri: String,
           val appDefinition: Application ) extends MarathonEventBusObject

case class StatusUpdateEvent(
           val eventType: EventTypes.EventType = EventTypes.status_update_event,
           val timestamp: DateTime,
           val slaveId:String,
           val taskId:String,
           val taskStatus: StatusUpdateEventTypes.StatusUpdateEventType,
           val appId:String,
           val host:String,
           val ports:List[Int],
           val version: DateTime ) extends MarathonEventBusObject

case class FrameworkMessageEvent(
           val eventType: EventTypes.EventType = EventTypes.framework_message_event,
           val timestamp: DateTime,
           val slaveId: String,
           val executorId: String,
           val message: String )

case class EventSubscriptionSubscribeEvent(
           val eventType:EventTypes.EventType = EventTypes.subscribe_event,
           val callbackUrl:String,
           val clientIp:String,
           val timestamp: DateTime ) extends MarathonEventBusObject

case class EventSubscriptionUnsubscribeEvent(
           val eventType:EventTypes.EventType = EventTypes.unsubscribe_event,
           val callbackUrl:String,
           val clientIp:String,
           val timestamp: DateTime ) extends MarathonEventBusObject

case class AddHealthCheckEvent(
           val eventType: EventTypes.EventType = EventTypes.add_health_check_event,
           val timestamp: DateTime,
           val appId: String,
           val healthCheck: HealthCheck)

case class RemoveHealthCheckEvent(
           val eventType: EventTypes.EventType = EventTypes.remove_health_check_event,
           val timestamp: DateTime,
           val appId: String,
           val healthCheck: HealthCheck)

case class FailedHealthCheckEvent(
           val eventType: EventTypes.EventType = EventTypes.failed_health_check_event,
           val timestamp: DateTime,
           val appId: String,
           val taskId: String,
           val healthCheck: HealthCheck)

case class HealthStatusChangedEvent(
           val eventType: EventTypes.EventType = EventTypes.health_status_changed_event,
           val timestamp: DateTime,
           val appId: String,
           val taskId: String,
           val version: DateTime,
           val alive: Boolean)

case class UnhealthyTaskKillEvent(
           val eventType: EventTypes.EventType = EventTypes.unhealthy_task_kill_event,
           val timestamp: DateTime,
           val appId: String,
           val taskId: String,
           val version: DateTime,
           val reason: String,
           val host: String,
           val slaveId: String)

case class GroupChangeSuccessEvent(
           val eventType: EventTypes.EventType = EventTypes.group_change_success,
           val timestamp: DateTime,
           val groupId: String,
           val version: DateTime )

case class GroupChangeFailedEvent(
           val eventType: EventTypes.EventType = EventTypes.group_change_failed,
           val timestamp: DateTime,
           val groupId: String,
           val version: DateTime,
           val reason: String )

case class DeploymentSuccessEvent(
           val eventType: EventTypes.EventType = EventTypes.deployment_success,
           val timestamp: DateTime,
           val id: String )

case class DeploymentFailedEvent(
           val eventType: EventTypes.EventType = EventTypes.deployment_failed,
           val timestamp: DateTime,
           val id: String )

case class DeploymentEventPlan(
           val id: String,
           val original: Deployment,
           val target: Deployment,
           val steps: List[DeploymentStep] )

case class DeploymentCurrentStep(
           val actions: List[DeploymentStep] )

case class DeploymentInfoEvent(
           val eventType: EventTypes.EventType = EventTypes.deployment_info,
           val timestamp: DateTime,
           val plan: DeploymentEventPlan,
           val currentStep: DeploymentCurrentStep )

case class DeploymentStepSuccessEvent(
           val eventType: EventTypes.EventType = EventTypes.deployment_step_success,
           val timestamp: DateTime,
           val plan: DeploymentEventPlan,
           val currentStep: DeploymentCurrentStep )

case class DeploymentStepFailureEvent(
           val eventType: EventTypes.EventType = EventTypes.deployment_step_failure,
           val timestamp: DateTime,
           val plan: DeploymentEventPlan,
           val currentStep: DeploymentCurrentStep )

trait EventParser extends EnumParser {
  implicit val eventFormat: Format[Event] = (
    ( __ \ "eventType" ).format[ EventTypes.EventType ] and
    ( __ \ "timestamp" ).format[String]
  )(Event.apply, unlift(Event.unapply))
}

trait ApiPostEventParser extends EventParser with ApplicationParser with EnumParser with VersionParser {
  implicit val apiPostEventFormat: Format[ApiPostEvent] = (
    ( __ \ "eventType" ).format[ EventTypes.EventType ] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "clientIp" ).format[String] and
    ( __ \ "uri" ).format[ String ] and
    ( __ \ "appDefinition" ).format[ Application ]
  )(ApiPostEvent.apply, unlift(ApiPostEvent.unapply))
}

trait StatusUpdateEventParser extends EventParser with EnumParser with VersionParser {
  implicit val statusUpdateEventFormat: Format[StatusUpdateEvent] = (
    ( __ \ "eventType" ).format[ EventTypes.EventType ] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "slaveId" ).format[String] and
    ( __ \ "taskId" ).format[String] and
    ( __ \ "taskStatus" ).format[ StatusUpdateEventTypes.StatusUpdateEventType ] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "host" ).format[String] and
    ( __ \ "ports" ).format[List[Int]] and
    ( __ \ "version" ).format[DateTime]
  )(StatusUpdateEvent.apply, unlift(StatusUpdateEvent.unapply))
}

trait FrameworkMessageEventParser extends EventParser with VersionParser {
  implicit val frameworkMessageEventFormat: Format[FrameworkMessageEvent] = (
    ( __ \ "eventType" ).format[ EventTypes.EventType ] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "slaveId" ).format[String] and
    ( __ \ "executorId" ).format[String] and
    ( __ \ "message" ).format[String]
  )(FrameworkMessageEvent.apply, unlift(FrameworkMessageEvent.unapply))
}

trait EventSubscriptionSubscribeEventParser extends EventParser with VersionParser {
  implicit val eventSubscriptionSubscribeEventFormat: Format[EventSubscriptionSubscribeEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "callbackUrl" ).format[String] and
    ( __ \ "clientIp" ).format[String] and
    ( __ \ "timestamp" ).format[DateTime]
  )(EventSubscriptionSubscribeEvent.apply, unlift(EventSubscriptionSubscribeEvent.unapply))
}

trait EventSubscriptionUnsubscribeEventParser extends EventParser with VersionParser {
  implicit val eventSubscriptionUnsubscribeEventFormat: Format[EventSubscriptionUnsubscribeEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "callbackUrl" ).format[String] and
    ( __ \ "clientIp" ).format[String] and
    ( __ \ "timestamp" ).format[DateTime]
  )(EventSubscriptionUnsubscribeEvent.apply, unlift(EventSubscriptionUnsubscribeEvent.unapply))
}

trait AddHealthCheckEventParser extends EventParser with VersionParser with HealthCheckParser {
  implicit val addHealthCheckEventFormat: Format[AddHealthCheckEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "healthCheck" ).format[HealthCheck]
  )(AddHealthCheckEvent.apply, unlift(AddHealthCheckEvent.unapply))
}

trait RemoveHealthCheckEventParser extends EventParser with VersionParser with HealthCheckParser {
  implicit val removeHealthCheckEventFormat: Format[RemoveHealthCheckEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "healthCheck" ).format[HealthCheck]
  )(RemoveHealthCheckEvent.apply, unlift(RemoveHealthCheckEvent.unapply))
}

trait FailedHealthCheckEventParser extends EventParser with VersionParser with HealthCheckParser {
  implicit val failedHealthCheckEventFormat: Format[FailedHealthCheckEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "taskId" ).format[String] and
    ( __ \ "healthCheck" ).format[HealthCheck]
  )(FailedHealthCheckEvent.apply, unlift(FailedHealthCheckEvent.unapply))
}

trait HealthStatusChangedEventParser extends EventParser with VersionParser {
  implicit val healthStatusChangedEventFormat: Format[HealthStatusChangedEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "taskId" ).format[String] and
    ( __ \ "version" ).format[DateTime] and
    ( __ \ "alive" ).format[Boolean]
  )(HealthStatusChangedEvent.apply, unlift(HealthStatusChangedEvent.unapply))
}

trait UnhealthyTaskKillEventParser extends EventParser with VersionParser {
  implicit val unhealthyTaskKillEventFormat: Format[UnhealthyTaskKillEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "appId" ).format[String] and
    ( __ \ "taskId" ).format[String] and
    ( __ \ "version" ).format[DateTime] and
    ( __ \ "reason" ).format[String] and
    ( __ \ "host" ).format[String] and
    ( __ \ "slaveId" ).format[String]
  )(UnhealthyTaskKillEvent.apply, unlift(UnhealthyTaskKillEvent.unapply))
}

trait GroupChangeSuccessEventParser extends EventParser with VersionParser {
  implicit val groupChangeSuccessEventFormat: Format[GroupChangeSuccessEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "groupId" ).format[String] and
    ( __ \ "version" ).format[DateTime]
  )(GroupChangeSuccessEvent.apply, unlift(GroupChangeSuccessEvent.unapply))
}

trait GroupChangeFailedEventParser extends EventParser with VersionParser {
  implicit val groupChangeFailedEventFormat: Format[GroupChangeFailedEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "groupId" ).format[String] and
    ( __ \ "version" ).format[DateTime] and
    ( __ \ "reason" ).format[String]
  )(GroupChangeFailedEvent.apply, unlift(GroupChangeFailedEvent.unapply))
}

trait DeploymentSuccessEventParser extends EventParser with VersionParser {
  implicit val deploymentSuccessEventFormat: Format[DeploymentSuccessEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "id" ).format[String]
  )(DeploymentSuccessEvent.apply, unlift(DeploymentSuccessEvent.unapply))
}

trait DeploymentFailedEventParser extends EventParser with VersionParser {
  implicit val deploymentFailedEventFormat: Format[DeploymentFailedEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "id" ).format[String]
  )(DeploymentFailedEvent.apply, unlift(DeploymentFailedEvent.unapply))
}

trait DeploymentEventPlanParser extends DeploymentParser with DeploymentStepParser {
  implicit val deploymentEventPlanFormat: Format[DeploymentEventPlan] = (
    ( __ \ "id" ).format[String] and
    ( __ \ "original" ).format[Deployment] and
    ( __ \ "target" ).format[Deployment] and
    ( __ \ "steps" ).format[List[DeploymentStep]]
  )(DeploymentEventPlan.apply, unlift(DeploymentEventPlan.unapply))
}

trait DeploymentCurrentStepParser extends DeploymentParser with DeploymentStepParser {
  implicit val deploymentCurrentStepFormat: Format[DeploymentCurrentStep] = ( __ \ "actions" ).format[List[DeploymentStep]].inmap(DeploymentCurrentStep.apply, unlift(DeploymentCurrentStep.unapply))
}

trait DeploymentInfoEventParser extends EventParser with VersionParser with DeploymentEventPlanParser with DeploymentCurrentStepParser {
  implicit val deploymentInfoEventFormat: Format[DeploymentInfoEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "plan" ).format[DeploymentEventPlan] and
    ( __ \ "currentStep" ).format[DeploymentCurrentStep]
  )(DeploymentInfoEvent.apply, unlift(DeploymentInfoEvent.unapply))
}

trait DeploymentStepSuccessEventParser extends EventParser with VersionParser with DeploymentEventPlanParser with DeploymentCurrentStepParser {
  implicit val deploymentStepSuccessEventFormat: Format[DeploymentStepSuccessEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "plan" ).format[DeploymentEventPlan] and
    ( __ \ "currentStep" ).format[DeploymentCurrentStep]
  )(DeploymentStepSuccessEvent.apply, unlift(DeploymentStepSuccessEvent.unapply))
}

trait DeploymentStepFailureEventParser extends EventParser with VersionParser with DeploymentEventPlanParser with DeploymentCurrentStepParser {
  implicit val deploymentStepFailureEventFormat: Format[DeploymentStepFailureEvent] = (
    ( __ \ "eventType" ).format[EventTypes.EventType] and
    ( __ \ "timestamp" ).format[DateTime] and
    ( __ \ "plan" ).format[DeploymentEventPlan] and
    ( __ \ "currentStep" ).format[DeploymentCurrentStep]
  )(DeploymentStepFailureEvent.apply, unlift(DeploymentStepFailureEvent.unapply))
}

