/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json

class EventTests extends WordSpec
  with Matchers
  with EventParser
  with StatusUpdateEventParser
  with ApiPostEventParser {

  "EventParser" should {
    "parse a valid message looking like a Marathon Event Bus message" in {
      val eventType = EventTypes.status_update_event
      val timestamp = ""+System.currentTimeMillis()
      val data =
        s"""
          {
            "eventType": "$eventType",
            "timestamp": "$timestamp"
          }
         """
      val message = Json.parse(data).as[Event]
      message shouldBe Event(
        eventType = eventType,
        timestamp = timestamp
      )
    }
  }

  "StatusUpdateEventParser" should {
    "parse a valid message looking like a Marathon Event Bus message" in {
      val eventType = EventTypes.status_update_event
      val timestamp = Version()
      val slaveId = s"slave.$timestamp"
      val taskId = s"test.task.$timestamp"
      val taskStatus = StatusUpdateEventTypes.TASK_RUNNING
      val appId = "/test/task"
      val ports = List(31245)
      val host = "slave-2.mesos.test"
      val version = Version()
      val data =
        s"""
          {
            "eventType": "$eventType",
            "timestamp": "${Version(timestamp)}",
            "slaveId": "$slaveId",
            "taskId": "$taskId",
            "taskStatus": "$taskStatus",
            "appId": "$appId",
            "ports": [${ports.mkString(",")}],
            "host": "$host",
            "version": "${Version(version)}"
          }
         """
      val message = Json.parse(data).as[StatusUpdateEvent]
      message shouldBe StatusUpdateEvent(
        timestamp = timestamp,
        slaveId   = slaveId,
        taskId    = taskId,
        taskStatus = taskStatus,
        appId = appId,
        ports = ports,
        host = host,
        version = version
      )
    }
  }

  "ApiPostEventParser" should {
    "parse a valid message looking like an event" in {
      val eventType = EventTypes.api_post_event
      val timestamp = Version()
      val clientIp = "0:0:0:0:0:0:0:1"
      val uri = "/v2/apps/test/application"
      val data =
        s"""
          {
            "eventType": "$eventType",
            "timestamp": "${Version(timestamp)}",
            "clientIp": "$clientIp",
            "uri": "$uri",
            "appDefinition": {
              "id": "/test/application",
              "instances": 1,
              "cpus": 1,
              "mem": 1024,
              "disk": 0,
              "env": {},
              "labels": {},
              "ports": [],
              "container": null,
              "uris": [],
              "requirePorts": false,
              "tasksStaged": 0,
              "tasksRunning": 0
            }
          }
         """
      val message = Json.parse(data).as[ApiPostEvent]
      message shouldBe ApiPostEvent(
        timestamp = timestamp,
        clientIp  = clientIp,
        uri       = uri,
        appDefinition = Application(
          id = "/test/application"
        )
      )
    }
  }

}
