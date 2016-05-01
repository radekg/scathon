/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import java.util.UUID

import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json._

class RestApiTests extends WordSpec
  with Matchers
  with Inside
  with ApplicationParser
  with GetPluginsResponseParser {

  val appId = "/test/application"

  val taskId = "test_application.8639d6e5-55e4-11e5-9730-0242b5b223a1"
  val taskHost = "slave-4.mesos.private"
  val taskPorts = List(31025)

  val volumeContainerPath = "/etc/hosts"
  val volumeHostPath = "/etc/destination"
  val volumeMode = "RO"

  val slaveId = UUID.randomUUID().toString

  val taskVersion = Version("2015-09-08T04:46:08.736Z")
  val taskStartedAt = Version("2015-09-08T04:46:54.248Z")
  val taskStagedAt = Version("2015-09-08T04:46:09.845Z")
  val appVersion = Version("2015-09-08T04:46:08.736Z")

  "parse a valid message looking like a Marathon Mesos application data with Docker container" in {

    val cmd = "/bin/sleep 10"

    val containerPort1 = 9000
    val containerPort2 = 9001
    val hostPort = 0
    val servicePort1 = 10001
    val servicePort2 = 10002

    val taskFailureReason = "unit test failure"

    val data =
      s"""
        {
          "id": "$appId",
          "cmd": "$cmd",
          "args": null,
          "user": null,
          "env": {
            "JAVA_HOME": "/usr/lib/jvm/java-openjdk-1.8-amd64"
          },
          "instances": 1,
          "cpus": 1,
          "mem": 256,
          "disk": 0,
          "executor": "",
          "constraints": [],
          "uris": [
            "hdfs:///docker-registry/docker.tar.gz"
          ],
          "storeUrls": [],
          "ports": [
            10001,
            10002
          ],
          "requirePorts": false,
          "backoffSeconds": 1,
          "backoffFactor": 1.15,
          "maxLaunchDelaySeconds": 3600,
          "container": {
            "type": "DOCKER",
            "volumes": [
              {
                "containerPath": "$volumeContainerPath",
                "hostPath": "$volumeHostPath",
                "mode": "$volumeMode"
              }
            ],
            "docker": {
              "image": "docker-registry.example.com/test-application:latest",
              "network": "BRIDGE",
              "portMappings": [
                {
                  "containerPort": $containerPort1,
                  "hostPort": $hostPort,
                  "servicePort": $servicePort1,
                  "protocol": "tcp"
                },
                {
                  "containerPort": $containerPort2,
                  "hostPort": $hostPort,
                  "servicePort": $servicePort2,
                  "protocol": "tcp"
                }
              ],
              "privileged": false,
              "parameters": [],
              "forcePullImage": false
            }
          },
          "healthChecks": [],
          "dependencies": [],
          "upgradeStrategy": {
            "minimumHealthCapacity": 1,
            "maximumOverCapacity": 1
          },
          "labels": {
            "consul.service.id": "service.1",
            "consul.service.name": "service"
          },
          "acceptedResourceRoles": null,
          "version": "${Version(appVersion)}",
          "tasksStaged": 0,
          "tasksRunning": 1,
          "tasksHealthy": 0,
          "tasksUnhealthy": 0,
          "deployments": [],
          "tasks": [
            {
              "id": "$taskId",
              "host": "$taskHost",
              "ports": [${taskPorts.mkString(",")}],
              "servicePorts": [${taskPorts.mkString(",")}],
              "startedAt": "${Version(taskStartedAt)}",
              "stagedAt": "${Version(taskStagedAt)}",
              "version": "${Version(taskVersion)}",
              "appId": "$appId",
              "slaveId": "$slaveId"
            }
          ],
          "lastTaskFailure": {
            "appId": "$appId",
            "slaveId": "$slaveId",
            "host": "$taskHost",
            "message": "$taskFailureReason",
            "state": "TASK_FAILED",
            "taskId": "$taskId",
            "timestamp": "${Version(taskVersion)}",
            "version": "${Version(taskVersion)}"
          },
          "healthChecks": [
            {
              "protocol": "TCP",
              "portIndex": 2
            }, {
              "protocol": "HTTP",
              "path": "/unit/test",
              "portIndex": 2
            }, {
              "protocol": "COMMAND",
              "command": "echo 1"
            }
          ]
        }
       """

    val message = Json.parse(data).as[Application]
    message.id shouldBe (appId)
    message.instances shouldBe (1)
    message.cpus shouldBe (1)
    message.mem shouldBe (256)
    inside(message.cmd) {
      case Some(str) => str shouldBe (cmd)
    }
    message.env shouldBe Map(
      "JAVA_HOME" -> "/usr/lib/jvm/java-openjdk-1.8-amd64"
    )
    inside(message.lastTaskFailure) {
      case Some(failure) =>
        failure shouldBe LastTaskFailure(
          appId = appId,
          host = taskHost,
          message = taskFailureReason,
          state = StatusUpdateEventTypes.TASK_FAILED,
          taskId = taskId,
          timestamp = taskVersion,
          version = taskVersion
        )
    }
    inside(message.tasks) {
      case Some(tasks) =>
        tasks.head shouldBe Task(
          id = taskId,
          host = taskHost,
          ports = taskPorts,
          servicePorts = taskPorts,
          startedAt = Some(taskStartedAt),
          stagedAt = Some(taskStagedAt),
          version = taskVersion,
          appId = appId,
          slaveId = slaveId )
    }
    inside(message.labels) {
      case labels =>
        labels shouldBe (Map(
          "consul.service.id" -> "service.1",
          "consul.service.name" -> "service"
        ))
    }
    inside(message.uris) {
      case uris =>
        uris shouldBe (Some(List(
          "hdfs:///docker-registry/docker.tar.gz"
        )))
    }
    inside(message.container) {
      case Some(container) =>
        inside(container.docker) {
          case Some(docker) =>
            docker.portMappings shouldBe Some(List(
              ContainerPortMapping(
                containerPort = containerPort1,
                hostPort = hostPort,
                servicePort = Some(servicePort1),
                protocol = PortMappingTypes.TCP
              ),
              ContainerPortMapping(
                containerPort = containerPort2,
                hostPort = hostPort,
                servicePort = Some(servicePort2),
                protocol = PortMappingTypes.TCP
              )
            ))
        }
        inside (container.volumes) {
          case volumes =>
            volumes shouldBe (List(
              ContainerVolume(
                containerPath = volumeContainerPath,
                hostPath = volumeHostPath,
                mode = volumeMode
              )
            ))
        }
    }
    inside(message.healthChecks) {
      case Some(checks) =>
        checks should be( List(
          TcpHealthCheck( portIndex = Some(2) ),
          HttpHealthCheck( "/unit/test", portIndex = Some(2) ),
          CommandHealthCheck( "echo 1" ) ) )
    }
  }

  "parse a valid message looking like a Marathon Mesos application data with unknown container" in {

    val data =
      s"""
        {
          "id": "$appId",
          "cmd": null,
          "args": null,
          "user": null,
          "env": {
            "JAVA_HOME": "/usr/lib/jvm/java-openjdk-1.8-amd64"
          },
          "instances": 1,
          "cpus": 1,
          "mem": 256,
          "disk": 0,
          "executor": "",
          "constraints": [],
          "uris": [
            "hdfs:///docker-registry/docker.tar.gz"
          ],
          "storeUrls": [],
          "ports": [
            10001,
            10002
          ],
          "requirePorts": false,
          "backoffSeconds": 1,
          "backoffFactor": 1.15,
          "maxLaunchDelaySeconds": 3600,
          "container": {
            "type": "MESOS",
            "volumes": [
              {
                "containerPath": "$volumeContainerPath",
                "hostPath": "$volumeHostPath",
                "mode": "$volumeMode"
              }
            ]
          },
          "healthChecks": [],
          "dependencies": [],
          "upgradeStrategy": {
            "minimumHealthCapacity": 1,
            "maximumOverCapacity": 1
          },
          "labels": {},
          "acceptedResourceRoles": null,
          "version": "${Version(appVersion)}",
          "tasksStaged": 0,
          "tasksRunning": 1,
          "tasksHealthy": 0,
          "tasksUnhealthy": 0,
          "deployments": [],
          "tasks": [
            {
              "id": "$taskId",
              "host": "$taskHost",
              "ports": [${taskPorts.mkString(",")}],
              "servicePorts": [${taskPorts.mkString(",")}],
              "startedAt": "${Version(taskStartedAt)}",
              "stagedAt": "${Version(taskStagedAt)}",
              "version": "${Version(taskVersion)}",
              "appId": "$appId",
              "slaveId": "$slaveId"
            }
          ],
          "lastTaskFailure": null
        }
       """

    val message = Json.parse(data).as[Application]
    message.id shouldBe (appId)
    message.instances shouldBe (1)
    message.cpus shouldBe (1)
    message.mem shouldBe (256)
    message.cmd should matchPattern {
      case None =>
    }
    inside(message.tasks) {
      case Some(tasks) =>
        tasks shouldBe List(Task(
          id = taskId,
          host = taskHost,
          ports = taskPorts,
          servicePorts = taskPorts,
          startedAt = Some(taskStartedAt),
          stagedAt = Some(taskStagedAt),
          version = taskVersion,
          appId = appId,
          slaveId = slaveId ))
    }

    inside(message.container) {
      case Some(container) =>
        container.docker should matchPattern {
          case None =>
        }
        inside (container.volumes) {
          case volumes =>
            volumes shouldBe (List(
              ContainerVolume(
                containerPath = volumeContainerPath,
                hostPath = volumeHostPath,
                mode = volumeMode
              )
            ))
        }
    }
  }

  "parse a valid message looking like a Marathon Mesos application data with no container" in {

    val data =
      s"""
        {
          "id": "$appId",
          "cmd": null,
          "args": null,
          "user": null,
          "env": {
            "JAVA_HOME": "/usr/lib/jvm/java-openjdk-1.8-amd64"
          },
          "instances": 1,
          "cpus": 1,
          "mem": 256,
          "disk": 0,
          "executor": "",
          "constraints": [],
          "uris": [
            "hdfs:///docker-registry/docker.tar.gz"
          ],
          "storeUrls": [],
          "ports": [
            10001,
            10002
          ],
          "requirePorts": false,
          "backoffSeconds": 1,
          "backoffFactor": 1.15,
          "maxLaunchDelaySeconds": 3600,
          "container": null,
          "healthChecks": [],
          "dependencies": [],
          "upgradeStrategy": {
            "minimumHealthCapacity": 1,
            "maximumOverCapacity": 1
          },
          "labels": {},
          "acceptedResourceRoles": null,
          "version": "${Version(appVersion)}",
          "tasksStaged": 0,
          "tasksRunning": 1,
          "tasksHealthy": 0,
          "tasksUnhealthy": 0,
          "deployments": [],
          "tasks": [
            {
              "id": "$taskId",
              "host": "$taskHost",
              "ports": [${taskPorts.mkString(",")}],
              "servicePorts": [${taskPorts.mkString(",")}],
              "startedAt": "${Version(taskStartedAt)}",
              "stagedAt": "${Version(taskStagedAt)}",
              "version": "${Version(taskVersion)}",
              "appId": "$appId",
              "slaveId": "$slaveId"
            }
          ],
          "lastTaskFailure": null
        }
       """

    val message = Json.parse(data).as[Application]
    message.id shouldBe (appId)
    inside(message.tasks) {
      case Some(tasks) =>
        tasks shouldBe List(Task(
          id = taskId,
          host = taskHost,
          ports = taskPorts,
          servicePorts = taskPorts,
          startedAt = Some(taskStartedAt),
          stagedAt = Some(taskStagedAt),
          version = taskVersion,
          appId = appId,
          slaveId = slaveId ))
    }
    message.container should matchPattern { case None => }
  }

  "can build JSON from a representation and convert back to a representation (pre v0.16.0)" in {

    val app = Application(
      id = "/test/application",
      env = Map(
        "JAVA_HOME" -> "/usr/lib/jvm/java-openjdk-1.8-amd64"
      ),
      ports = Some(List(
        9000,
        10000
      )),
      labels = Map(
        "consul.service.id" -> "service.1",
        "consul.service.name" -> "service"
      ),
      uris = Some(List(
        "hdfs:///docker-registry/docker.tar.gz"
      )),
      container = Some(Container(
        docker = Some(ContainerDocker(
          image = "docker-registry.example.com/test-application:latest",
          network = DockerNetworkTypes.BRIDGE,
          portMappings = Some(List(
            ContainerPortMapping(
              containerPort = 9000,
              hostPort = 0,
              servicePort = None,
              protocol = PortMappingTypes.TCP
            )
          ))
        )),
        volumes = List(ContainerVolume(
          containerPath = "/etc/containerPath",
          hostPath = "/etc/hostPath",
          mode = "RW"
        ))
      )),
      tasks = Some(List(
        Task(
          id = "marathon.task.1",
          host = "marathon.host.vm",
          ports = List(12345),
          servicePorts = List(12345),
          startedAt = Some(taskStartedAt),
          stagedAt = Some(taskStagedAt),
          version = taskVersion,
          appId = appId,
          slaveId = slaveId
        )
      ))
    )

    val jsonString = applicationFormat.writes(app).toString()
    val message = Json.parse(jsonString).as[Application]
    message shouldBe app
  }

  "can build JSON from a representation and convert back to a representation (v0.16.0+)" in {

    val app = Application(
      id = "/test/application",
      env = Map(
        "JAVA_HOME" -> "/usr/lib/jvm/java-openjdk-1.8-amd64"
      ),
      portDefinitions = Some(List(
        PortDefinition(9000, PortMappingTypes.TCP, "User interface port"),
        PortDefinition(10000, PortMappingTypes.TCP, "Additional port", Map("label1" -> "value 1"))
      )),
      labels = Map(
        "consul.service.id" -> "service.1",
        "consul.service.name" -> "service"
      ),
      fetch = Some(List(
        FetchUri("http://example.com/example-resource.md"),
        FetchUri(uri = "http://example.com/example-resource.zip", executable = false, extract = false, cache = false)
      )),
      container = Some(Container(
        docker = Some(ContainerDocker(
          image = "docker-registry.example.com/test-application:latest",
          network = DockerNetworkTypes.BRIDGE,
          portMappings = Some(List(
            ContainerPortMapping(
              containerPort = 9000,
              hostPort = 0,
              servicePort = None,
              protocol = PortMappingTypes.TCP
            )
          ))
        )),
        volumes = List(ContainerVolume(
          containerPath = "/etc/containerPath",
          hostPath = "/etc/hostPath",
          mode = "RW"
        ))
      )),
      tasks = Some(List(
        Task(
          id = "marathon.task.1",
          host = "marathon.host.vm",
          ports = List(12345),
          servicePorts = List(12345),
          startedAt = Some(taskStartedAt),
          stagedAt = Some(taskStagedAt),
          version = taskVersion,
          appId = appId,
          slaveId = slaveId
        )
      ))
    )

    val jsonString = applicationFormat.writes(app).toString()
    val message = Json.parse(jsonString).as[Application]
    message shouldBe app
  }

  "can parse plugin information from a valid looking JSON" in {
    val data =
      s"""
         {
           "plugins": [
             {
               "id": "webjar",
               "implementation": "mesosphere.marathon.example.plugin.http.WebJarHandler",
               "info": {
                 "version": "1.2.3",
                 "array": [ 1, 2, 3, 4, 5, 6 ],
                 "test": true
               },
               "configuration": {
                 "config-property": "with-value"
               },
               "plugin": "mesosphere.marathon.plugin.http.HttpRequestHandler",
               "tags": [ "webjar", "test" ]
               }
           ]
         }
       """
    val message = Json.parse(data).as[GetPluginsResponse]
    message.plugins.length shouldBe 1
    inside(message.plugins.head) {
      case plugin =>
        plugin.id shouldBe "webjar"
        plugin.implementation shouldBe "mesosphere.marathon.example.plugin.http.WebJarHandler"
        plugin.tags shouldBe Some(List("webjar", "test"))
        plugin.plugin shouldBe "mesosphere.marathon.plugin.http.HttpRequestHandler"
        plugin.configuration shouldBe Some(JsObject( Map("config-property" -> JsString("with-value")) ))
        plugin.info shouldBe Some(Map(
          "version" -> JsString("1.2.3"),
          "array" -> JsArray(List(JsNumber(1), JsNumber(2), JsNumber(3), JsNumber(4), JsNumber(5), JsNumber(6))),
          "test" -> JsBoolean(true)
        ))
    }
  }

  "can parse List[Application] to JSON and back" in {
    val apps = List(Application(
      id = "/test/sleep60",
      cmd = Some("sleep 60"),
      cpus = 0.3,
      instances = 2,
      mem = 9,
      dependencies = Some(List("/test/sleep120",  "/other/namespace/or/app"))
    ), Application(
      id = "/test/sleep120",
      cmd = Some("sleep 120"),
      cpus = 0.3,
      instances = 2,
      mem = 9
    ))
    val jsonString = Json.toJson(apps).toString()
    val appsFromJson = Json.parse(jsonString).as[List[Application]]
    appsFromJson.length shouldBe(2)
    inside( appsFromJson(0) ) {
      case app =>
        app.id shouldBe( apps(0).id )
        app.cmd shouldBe( apps(0).cmd )
        app.cpus shouldBe( apps(0).cpus )
        app.instances shouldBe( apps(0).instances )
        app.mem shouldBe( apps(0).mem )
        app.dependencies shouldBe( apps(0).dependencies )
    }
    inside( appsFromJson(1) ) {
      case app =>
        app.id shouldBe( apps(1).id )
        app.cmd shouldBe( apps(1).cmd )
        app.cpus shouldBe( apps(1).cpus )
        app.instances shouldBe( apps(1).instances )
        app.mem shouldBe( apps(1).mem )
    }
  }

}
