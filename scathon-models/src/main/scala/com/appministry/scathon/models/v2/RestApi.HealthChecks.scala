/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import com.appministry.scathon.models.v2.util.HealthCheckUtils
import play.api.libs.json._

trait HealthCheck extends MarathonApiObject {
  def asJson(): JsObject
}

case class HttpHealthCheck(
           val path: String = HealthCheckDefaults.path,
           val gracePeriodSeconds: Int = HealthCheckDefaults.gracePeriodSeconds,
           val intervalSeconds: Int = HealthCheckDefaults.intervalSeconds,
           val portIndex: Option[Int] = None,
           val port: Option[Int] = None,
           val timeoutSeconds: Int = HealthCheckDefaults.timeoutSeconds,
           val maxConsecutiveFailures: Int = HealthCheckDefaults.maxConsecutiveFailures ) extends HealthCheck {
  override def asJson(): JsObject = {
    JsObject(List(
      Some(("protocol", JsString("HTTP"))),
      Some(("path", JsString(path))),
      Some(("gracePeriodSeconds", JsNumber(gracePeriodSeconds))),
      Some(("intervalSeconds", JsNumber(intervalSeconds))),
      portIndex match {
        case Some(v) => Some(("portIndex", JsNumber(v)))
        case None => None
      },
      port match {
        case Some(v) => Some(("port", JsNumber(v)))
        case None => None
      },
      Some(("timeoutSeconds", JsNumber(timeoutSeconds))),
      Some(("maxConsecutiveFailures", JsNumber(maxConsecutiveFailures))) ).flatten)
  }
}

case class TcpHealthCheck(
           val gracePeriodSeconds: Int = HealthCheckDefaults.gracePeriodSeconds,
           val intervalSeconds: Int = HealthCheckDefaults.timeoutSeconds,
           val portIndex: Option[Int] = None,
           val port: Option[Int] = None,
           val timeoutSeconds: Int = HealthCheckDefaults.timeoutSeconds,
           val maxConsecutiveFailures: Int = HealthCheckDefaults.maxConsecutiveFailures ) extends HealthCheck {
  override def asJson(): JsObject = {
    JsObject(List(
      Some(("protocol", JsString("TCP"))),
      Some(("gracePeriodSeconds", JsNumber(gracePeriodSeconds))),
      Some(("intervalSeconds", JsNumber(intervalSeconds))),
      portIndex match {
        case Some(v) => Some(("portIndex", JsNumber(v)))
        case None => None
      },
      port match {
        case Some(v) => Some(("port", JsNumber(v)))
        case None => None
      },
      Some(("timeoutSeconds", JsNumber(timeoutSeconds))),
      Some(("maxConsecutiveFailures", JsNumber(maxConsecutiveFailures))) ).flatten)
  }
}

case class CommandHealthCheck(
           val command: String,
           val maxConsecutiveFailures: Int= HealthCheckDefaults.maxConsecutiveFailures ) extends HealthCheck {
  override def asJson(): JsObject = {
    JsObject(List(
      ("protocol", JsString("COMMAND")),
      ("command", JsString(command)),
      ("maxConsecutiveFailures", JsNumber(maxConsecutiveFailures)) ))
  }
}

trait HealthCheckParser {
  implicit val healthCheckFormat = HealthCheckUtils.healthCheckFormat
}
