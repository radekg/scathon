/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import com.appministry.scathon.models.mesos.ConstraintParser
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class HealthCheckTests extends WordSpec
  with Matchers
  with Inside
  with ConstraintParser
  with HealthCheckParser {

  "HealthCheckParser" should {

    "parse HTTP health check json string" in {
      val jsonString =
        """
          |{
          |  "protocol": "HTTP",
          |  "path": "/test/path",
          |  "portIndex": 1
          |}
        """.stripMargin
      inside( Json.parse(jsonString).asOpt[HealthCheck] ) {
        case Some(healthCheck) =>
          healthCheck should be( HttpHealthCheck( "/test/path", portIndex = Some(1) ) )
        case None => fail("Expected Some, got None.")
      }
    }

    "parse TCP health check json string" in {
      val jsonString =
        """
          |{
          |  "protocol": "TCP",
          |  "timeoutSeconds": 10,
          |  "portIndex": 2
          |}
        """.stripMargin
      inside( Json.parse(jsonString).asOpt[HealthCheck] ) {
        case Some(healthCheck) =>
          healthCheck should be( TcpHealthCheck( portIndex = Some(2), timeoutSeconds = 10 ) )
        case None => fail("Expected Some, got None.")
      }
    }

    "parse command health check json string" in {
      val jsonString =
        """
          |{
          |  "protocol": "COMMAND",
          |  "command": "echo 1"
          |}
        """.stripMargin
      inside( Json.parse(jsonString).asOpt[HealthCheck] ) {
        case Some(healthCheck) =>
          healthCheck should be( CommandHealthCheck( command = "echo 1" ) )
        case None => fail("Expected Some, got None.")
      }
    }

    "handle HTTP health checks" in {
      val path = "/test/path"
      val gracePeriodSeconds = 15
      val intervalSeconds = 8
      val portIndex = Some(2)
      val port = Some(10000)
      val timeoutSeconds = 60
      val maxConsecutiveFailures = 5

      val formatted = healthCheckFormat.writes( HttpHealthCheck(
        path,
        gracePeriodSeconds,
        intervalSeconds,
        portIndex,
        port,
        timeoutSeconds,
        maxConsecutiveFailures ) ).toString()

      inside( Json.parse(formatted).asOpt[HealthCheck] ) {
        case Some(healthCheck) =>
          healthCheck should be( HttpHealthCheck(
            path,
            gracePeriodSeconds,
            intervalSeconds,
            portIndex,
            port,
            timeoutSeconds,
            maxConsecutiveFailures ) )
        case None => fail("Expected Some, got None.")
      }
    }

    "handle TCP health checks" in {
      val gracePeriodSeconds = 15
      val intervalSeconds = 8
      val portIndex = Some(2)
      val port = Some(10000)
      val timeoutSeconds = 60
      val maxConsecutiveFailures = 5

      val formatted = healthCheckFormat.writes( TcpHealthCheck(
        gracePeriodSeconds,
        intervalSeconds,
        portIndex,
        port,
        timeoutSeconds,
        maxConsecutiveFailures ) ).toString()

      inside( Json.parse(formatted).asOpt[HealthCheck] ) {
        case Some(healthCheck) =>
          healthCheck should be( TcpHealthCheck(
            gracePeriodSeconds,
            intervalSeconds,
            portIndex,
            port,
            timeoutSeconds,
            maxConsecutiveFailures ) )
        case None => fail("Expected Some, got None.")
      }
    }

    "handle command health checks" in {
      val command = "echo 1"
      val maxConsecutiveFailures = 5

      val formatted = healthCheckFormat.writes( CommandHealthCheck(
        command,
        maxConsecutiveFailures ) ).toString()

      inside( Json.parse(formatted).asOpt[HealthCheck] ) {
        case Some(healthCheck) =>
          healthCheck should be( CommandHealthCheck(
            command,
            maxConsecutiveFailures ) )
        case None => fail("Expected Some, got None.")
      }
    }

    "fail to parse an object with no protocol" in {
      val jsonString =
        """
          |{
          |  "path": "/test/path",
          |  "portIndex": 1
          |}
        """.stripMargin
      inside( Json.parse(jsonString).asOpt[HealthCheck] ) {
        case Some(healthCheck) => fail("Expected Some, got None.")
        case None =>
      }
    }

    "fail to parse an object with unknown protocol" in {
      val jsonString =
        """
          |{
          |  "protocol": "UNSUPPORTED",
          |  "path": "/test/path",
          |  "portIndex": 1
          |}
        """.stripMargin
      inside( Json.parse(jsonString).asOpt[HealthCheck] ) {
        case Some(healthCheck) => fail("Expected Some, got None.")
        case None =>
      }
    }

    "fail to parse an object with protocol which isn't a String" in {
      val jsonString =
        """
          |{
          |  "protocol": { "property": "not a string" },
          |  "path": "/test/path",
          |  "portIndex": 1
          |}
        """.stripMargin
      inside( Json.parse(jsonString).asOpt[HealthCheck] ) {
        case Some(healthCheck) => fail("Expected Some, got None.")
        case None =>
      }
    }

  }

}
