/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Deployment(
           val id: String,
           val version: DateTime,
           val affectedApps: List[String],
           val steps: List[List[DeploymentStep]],
           val currentActions: List[DeploymentCurrentAction],
           val currentStep: Int,
           val totalSteps: Int ) extends MarathonApiObject

case class DeploymentStep(
           val action: DeploymentActionTypes.DeploymentActionType,
           val app: String ) extends MarathonApiObject

case class DeploymentCurrentAction(
           val action: DeploymentActionTypes.DeploymentActionType,
           val app: String,
           val readinessChecks: Option[List[DeploymentReadinessCheck]] = None ) extends MarathonApiObject

case class DeploymentReadinessCheck(
           val lastResponse: Option[DeploymentReadinessCheckLastResponse] = None,
           val name: String,
           val ready: Boolean,
           val taskId: String ) extends MarathonApiObject

case class DeploymentReadinessCheckLastResponse(
           val body: String,
           val contentType: String,
           val status: Int ) extends MarathonApiObject

trait DeploymentParser extends DeploymentStepParser with DeploymentCurrentActionParser with VersionParser {
  implicit val deploymentFormat: Format[Deployment] = (
    ( __ \ "id" ).format[String] and
    ( __ \ "version" ).format[DateTime] and
    ( __ \ "affectedApps" ).format[List[String]] and
    ( __ \ "steps" ).format[List[List[DeploymentStep]]] and
    ( __ \ "currentActions" ).format[List[DeploymentCurrentAction]] and
    ( __ \ "currentStep" ).format[Int] and
    ( __ \ "totalSteps" ).format[Int]
  )(Deployment.apply, unlift(Deployment.unapply))
}

trait DeploymentStepParser extends EnumParser {
  implicit val deploymentStepFormat: Format[DeploymentStep] = (
    ( __ \ "action" ).format[DeploymentActionTypes.DeploymentActionType] and
    ( __ \ "app" ).format[String]
  )(DeploymentStep.apply, unlift(DeploymentStep.unapply))
}

trait DeploymentCurrentActionParser extends DeploymentReadinessCheckParser with EnumParser {
  implicit val deploymentCurrentActionFormat: Format[DeploymentCurrentAction] = (
    ( __ \ "action" ).format[DeploymentActionTypes.DeploymentActionType] and
    ( __ \ "app" ).format[String] and
    ( __ \ "readinessChecks" ).formatNullable[List[DeploymentReadinessCheck]]
  )(DeploymentCurrentAction.apply, unlift(DeploymentCurrentAction.unapply))
}

trait DeploymentReadinessCheckParser extends DeploymentReadinessCheckLastResponseParser {
  implicit val deploymentReadinessCheckFormat: Format[DeploymentReadinessCheck] = (
    ( __ \ "lastResponse" ).formatNullable[DeploymentReadinessCheckLastResponse] and
    ( __ \ "name" ).format[String] and
    ( __ \ "ready" ).format[Boolean] and
    ( __ \ "taskId" ).format[String]
  )(DeploymentReadinessCheck.apply, unlift(DeploymentReadinessCheck.unapply))
}

trait DeploymentReadinessCheckLastResponseParser {
  implicit val deploymentReadinessCheckLastResponseParserFormat: Format[DeploymentReadinessCheckLastResponse] = (
    ( __ \ "body" ).format[String] and
    ( __ \ "contentType" ).format[String] and
    ( __ \ "status" ).format[Int]
  )(DeploymentReadinessCheckLastResponse.apply, unlift(DeploymentReadinessCheckLastResponse.unapply))
}
