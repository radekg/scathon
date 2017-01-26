/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client.utils

import uk.co.appministry.scathon.models.mesos.UniqueConstraint
import uk.co.appministry.scathon.models.v2._

object TestApplicationData {

  val id = "/app/test"
  val cmd = "/bin/sleep"
  val args = List("10")
  val ports = List(10000,10001,10002)
  val instances = 3
  val dockerImage = "ubuntu:14.04"
  val env = Map("ENV_VAR" -> "some-value")
  val labels = Map( "consul.watch" -> "true",
                    "consul.id" -> "test-app-0",
                    "consul.name" -> "test-app",
                    "consul.tags" -> "tag1,tag2" )
  val acceptedRoles = List("*")
  val constraints = List(UniqueConstraint("hostname"))
  val container = Container(
                    docker = Some(ContainerDocker(
                      image = dockerImage,
                      network = DockerNetworkTypes.BRIDGE,
                      portMappings = Some(List(
                        ContainerPortMapping(10000, 0, None, PortMappingTypes.TCP),
                        ContainerPortMapping(10001, 0, None, PortMappingTypes.TCP),
                        ContainerPortMapping(10002, 0, None, PortMappingTypes.TCP)
                      ))
                    ))
                  )
  val fetch = List(FetchUri("http://www.apache.org/dyn/closer.lua/spark/spark-1.6.1/spark-1.6.1.tgz"))
  val healthChecks = List(HttpHealthCheck())

}
