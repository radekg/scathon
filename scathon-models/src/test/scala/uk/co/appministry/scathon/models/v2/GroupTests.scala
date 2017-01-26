/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2

import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class GroupTests extends WordSpec
  with Matchers
  with Inside
  with GroupParser {

  "GroupParser" should {

    "parse valid group JSON" in {

      val mainGroupId = "/"
      val firstSubGroupId = "/tools"
      val firstSubGroupFirstAppId = "/tools/oauth-server"
      val firstSubGroupSecondAppId = "/tools/datadog-agent"
      val firstSubGroupFirstGroupId = "/tools/log"
      val firstSubGroupSecondGroupId = "/tools/docker"
      val mainGroupVersion = Version("2015-09-17T10:38:20.875Z")

      val data =
        s"""
           |{
           |  "id": "$mainGroupId",
           |  "apps": [],
           |  "groups": [
           |    {
           |      "id": "$firstSubGroupId",
           |      "apps": [
           |        {
           |          "id": "$firstSubGroupFirstAppId",
           |          "instances": 2,
           |          "cpus": 1,
           |          "mem": 1024,
           |          "disk": 0,
           |          "constraints": [
           |            [
           |              "hostname",
           |              "UNIQUE"
           |            ]
           |          ],
           |          "uris": [],
           |          "storeUrls": [],
           |          "ports": [
           |            1980
           |          ],
           |          "requirePorts": false,
           |          "backoffSeconds": 1,
           |          "backoffFactor": 1.15,
           |          "maxLaunchDelaySeconds": 3600,
           |          "container": {
           |            "type": "DOCKER",
           |            "volumes": [],
           |            "docker": {
           |              "image": "docker-registry/oauth_server:6d7d463cb8b1517002080a81cf23f9cf7b7fc774",
           |              "network": "HOST",
           |              "privileged": false,
           |              "parameters": [],
           |              "forcePullImage": false
           |            }
           |          },
           |          "healthChecks": [
           |            {
           |              "path": "/",
           |              "protocol": "HTTP",
           |              "portIndex": 0,
           |              "gracePeriodSeconds": 300,
           |              "intervalSeconds": 20,
           |              "timeoutSeconds": 20,
           |              "maxConsecutiveFailures": 3,
           |              "ignoreHttp1xx": false
           |            }
           |          ],
           |          "dependencies": [],
           |          "upgradeStrategy": {
           |            "minimumHealthCapacity": 0.5,
           |            "maximumOverCapacity": 0
           |          },
           |          "version": "2015-09-28T19:47:37.681Z",
           |          "versionInfo": {
           |            "lastScalingAt": "2015-09-28T19:47:37.681Z",
           |            "lastConfigChangeAt": "2015-09-28T19:47:37.681Z"
           |          }
           |        },
           |        {
           |          "id": "$firstSubGroupSecondAppId",
           |          "instances": 5,
           |          "cpus": 1,
           |          "mem": 512,
           |          "disk": 0,
           |          "executor": "",
           |          "constraints": [
           |            [
           |              "hostname",
           |              "UNIQUE"
           |            ]
           |          ],
           |          "uris": [],
           |          "storeUrls": [],
           |          "ports": [
           |            10018
           |          ],
           |          "requirePorts": false,
           |          "backoffSeconds": 1,
           |          "backoffFactor": 1.15,
           |          "maxLaunchDelaySeconds": 3600,
           |          "container": {
           |            "type": "DOCKER",
           |            "volumes": [
           |              {
           |                "containerPath": "/var/run/docker.sock",
           |                "hostPath": "/var/run/docker.sock",
           |                "mode": "RW"
           |              },
           |              {
           |                "containerPath": "/host/proc/mounts",
           |                "hostPath": "/proc/mounts",
           |                "mode": "RO"
           |              },
           |              {
           |                "containerPath": "/host/sys/fs/cgroup",
           |                "hostPath": "/sys/fs/cgroup/",
           |                "mode": "RO"
           |              }
           |            ],
           |            "docker": {
           |              "image": "datadog/docker-dd-agent:latest",
           |              "network": "HOST",
           |              "privileged": true,
           |              "parameters": [],
           |              "forcePullImage": false
           |            }
           |          },
           |          "healthChecks": [],
           |          "dependencies": [],
           |          "upgradeStrategy": {
           |            "minimumHealthCapacity": 0.5,
           |            "maximumOverCapacity": 0
           |          },
           |          "version": "2015-08-26T22:33:24.225Z",
           |          "versionInfo": {
           |            "lastScalingAt": "2015-08-26T22:33:24.225Z",
           |            "lastConfigChangeAt": "2015-05-19T13:59:18.899Z"
           |          }
           |        }
           |      ],
           |      "groups": [
           |        {
           |          "id": "$firstSubGroupFirstGroupId",
           |          "apps": [],
           |          "groups": [],
           |          "dependencies": [],
           |          "version": "2015-09-17T10:38:20.875Z"
           |        },
           |        {
           |          "id": "$firstSubGroupSecondGroupId",
           |          "apps": [
           |            {
           |              "id": "/tools/docker/registry",
           |              "instances": 1,
           |              "cpus": 0.5,
           |              "mem": 4096,
           |              "disk": 0,
           |              "executor": "",
           |              "constraints": [],
           |              "uris": [],
           |              "storeUrls": [],
           |              "ports": [
           |                5000
           |              ],
           |              "requirePorts": false,
           |              "backoffSeconds": 1,
           |              "backoffFactor": 1.15,
           |              "maxLaunchDelaySeconds": 3600,
           |              "container": {
           |                "type": "DOCKER",
           |                "volumes": [
           |                  {
           |                    "containerPath": "/docker_storage",
           |                    "hostPath": "/hdd/tools/docker/registry",
           |                    "mode": "RW"
           |                  }
           |                ],
           |                "docker": {
           |                  "image": "registry",
           |                  "network": "BRIDGE",
           |                  "portMappings": [
           |                    {
           |                      "containerPort": 5000,
           |                      "hostPort": 0,
           |                      "servicePort": 5000,
           |                      "protocol": "tcp"
           |                    }
           |                  ],
           |                  "privileged": false,
           |                  "parameters": [],
           |                  "forcePullImage": false
           |                }
           |              },
           |              "healthChecks": [],
           |              "dependencies": [],
           |              "upgradeStrategy": {
           |                "minimumHealthCapacity": 1,
           |                "maximumOverCapacity": 1
           |              },
           |              "version": "2015-08-19T21:26:47.616Z",
           |              "versionInfo": {
           |                "lastScalingAt": "2015-08-19T21:26:47.616Z",
           |                "lastConfigChangeAt": "2015-08-19T21:00:54.621Z"
           |              }
           |            }
           |          ],
           |          "groups": [],
           |          "dependencies": [],
           |          "version": "2015-09-17T10:38:20.875Z"
           |        }
           |      ],
           |      "dependencies": [],
           |      "version": "2015-09-17T10:38:20.875Z"
           |    }
           |  ],
           |  "dependencies": [],
           |  "version": "${Version(mainGroupVersion)}"
           |}
         """.stripMargin

        val group = Json.parse(data).as[Group]
        group.id shouldBe( mainGroupId )
        group.apps.length shouldBe(0)
        group.groups.length shouldBe(1)
        group.dependencies shouldBe(List.empty[String])
        group.version shouldBe(mainGroupVersion)

        inside(group.groups(0)) {
          case group =>
            group.apps.length shouldBe(2)
            inside(group.apps(0)) {
              case app =>
                app.id shouldBe(firstSubGroupFirstAppId)
            }
            inside(group.apps(1)) {
              case app =>
                app.id shouldBe(firstSubGroupSecondAppId)
            }
            group.groups.length shouldBe(2)
            inside( group.groups(0) ) {
              case subGroup =>
                subGroup.id shouldBe(firstSubGroupFirstGroupId)
                subGroup.dependencies shouldBe(List.empty[String])
                subGroup.apps shouldBe(List.empty[Application])
                subGroup.groups shouldBe(List.empty[Group])
            }
            inside( group.groups(1) ) {
              case subGroup =>
                subGroup.id shouldBe(firstSubGroupSecondGroupId)
                subGroup.dependencies shouldBe(List.empty[String])
                subGroup.apps.length shouldBe(1)
                subGroup.groups shouldBe(List.empty[Group])
            }
        }


    }

  }

}
