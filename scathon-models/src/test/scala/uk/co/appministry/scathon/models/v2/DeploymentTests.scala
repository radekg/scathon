/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2

import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class DeploymentTests extends WordSpec
  with Matchers
  with Inside
  with DeploymentParser {

  "DeploymentParser" should {

    val deploymentId = "97c136bf-5a28-4821-9d94-480d9fbb01c8"
    val affectedApp  = "/foo"
    val currentStep = 2
    val version = Version("2015-09-30T09:09:17.614Z")

    "parse deployment JSON" in {

      val data =
        s"""
           {
             "id": "$deploymentId",
             "version": "${Version(version)}",
             "affectedApps": [ "$affectedApp" ],
             "steps": [
               [
                 {
                   "action": "StartApplication",
                   "app": "$affectedApp"
                 }
               ],
               [
                 {
                   "action": "ScaleApplication",
                   "app": "$affectedApp"
                 }
               ]
             ],
             "currentActions": [
               {
                 "action": "ScaleApplication",
                 "app": "$affectedApp",
                 "readinessChecks": [
                   {
                     "lastResponse": {
                       "body": "{}",
                       "contentType": "application/json",
                       "status": 500
                     },
                     "name": "myReadyCheck",
                     "ready": false,
                     "taskId": "foo.c9de6033"
                   }
                 ]
               }
             ],
             "currentStep": $currentStep,
             "totalSteps": 2
           }
        """

      val deployment = Json.parse(data).as[Deployment]
      deployment.id shouldBe( deploymentId )
      deployment.affectedApps shouldBe( List(affectedApp) )
      deployment.steps.length shouldBe (deployment.totalSteps)
      deployment.currentActions.length shouldBe (1)
      deployment.currentStep shouldBe (currentStep)

      inside(deployment.steps) {
        case steps =>
          steps.head shouldBe(List(DeploymentStep(
            action = DeploymentActionTypes.START_APPLICATION,
            app = affectedApp
          )))
      }

      inside(deployment.currentActions) {
        case actions =>
          actions.length shouldBe(1)
          inside(actions.head) {
            case action =>
              action.app shouldBe(affectedApp)
              action.action shouldBe(DeploymentActionTypes.SCALE_APPLICATION)
              action.readinessChecks shouldBe(Some(List(DeploymentReadinessCheck(
                name = "myReadyCheck",
                ready = false,
                taskId = "foo.c9de6033",
                lastResponse = Some(DeploymentReadinessCheckLastResponse(
                  body = "{}",
                  contentType = "application/json",
                  status = 500
                ))
              ))))
          }
      }

    }

    "parse a list of deployments from JSON" in {
      val data =
        s"""
          [
             {
               "id": "$deploymentId",
               "version": "${Version(version)}",
               "affectedApps": [ "$affectedApp" ],
               "steps": [
                 [
                   {
                     "action": "StartApplication",
                     "app": "$affectedApp"
                   }
                 ],
                 [
                   {
                     "action": "ScaleApplication",
                     "app": "$affectedApp"
                   }
                 ]
               ],
               "currentActions": [
                 {
                   "action": "ScaleApplication",
                   "app": "$affectedApp",
                   "readinessChecks": [
                     {
                       "lastResponse": {
                         "body": "{}",
                         "contentType": "application/json",
                         "status": 500
                       },
                       "name": "myReadyCheck",
                       "ready": false,
                       "taskId": "foo.c9de6033"
                     }
                   ]
                 }
               ],
               "currentStep": $currentStep,
               "totalSteps": 2
             }
           ]
        """

      val deployments = Json.parse(data).as[List[Deployment]]
      deployments.length shouldBe(1)
      inside(deployments.head) {
        case deployment =>
          deployment.id shouldBe( deploymentId )
      }
    }

  }

}
