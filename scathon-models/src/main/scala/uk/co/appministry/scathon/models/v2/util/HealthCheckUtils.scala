/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2.util

import uk.co.appministry.scathon.models.v2._
import play.api.libs.json._

object HealthCheckUtils {

  def healthCheckReads[E <: HealthCheck]: Reads[HealthCheck] = new Reads[HealthCheck] {
    def reads(json: JsValue): JsResult[HealthCheck] = {
      try {
        val obj = json.asInstanceOf[JsObject]

        ( obj \ "protocol" ).asOpt[String] match {
          case Some(v) if v == "HTTP" =>
            JsSuccess(
              HttpHealthCheck(
                (obj \ "path").asOpt[String].getOrElse( HealthCheckDefaults.path ),
                (obj \ "gracePeriodSeconds").asOpt[Int].getOrElse(HealthCheckDefaults.gracePeriodSeconds),
                (obj \ "intervalSeconds").asOpt[Int].getOrElse(HealthCheckDefaults.intervalSeconds),
                (obj \ "portIndex").asOpt[Int],
                (obj \ "port").asOpt[Int],
                (obj \ "timeoutSeconds").asOpt[Int].getOrElse(HealthCheckDefaults.timeoutSeconds),
                (obj \ "maxConsecutiveFailures").asOpt[Int].getOrElse(HealthCheckDefaults.maxConsecutiveFailures) ) )
          case Some(v) if v == "TCP" =>
            JsSuccess(
              TcpHealthCheck(
                (obj \ "gracePeriodSeconds").asOpt[Int].getOrElse(HealthCheckDefaults.gracePeriodSeconds),
                (obj \ "intervalSeconds").asOpt[Int].getOrElse(HealthCheckDefaults.intervalSeconds),
                (obj \ "portIndex").asOpt[Int],
                (obj \ "port").asOpt[Int],
                (obj \ "timeoutSeconds").asOpt[Int].getOrElse(HealthCheckDefaults.timeoutSeconds),
                (obj \ "maxConsecutiveFailures").asOpt[Int].getOrElse(HealthCheckDefaults.maxConsecutiveFailures) ) )
          case Some(v) if v == "COMMAND" =>
            JsSuccess(
              CommandHealthCheck(
                (obj \ "command").as[String],
                (obj \ "maxConsecutiveFailures").asOpt[Int].getOrElse(HealthCheckDefaults.maxConsecutiveFailures) ) )
          case Some(anyOther) =>
            JsError(s"Unsupported HealthCheck.protocol: $anyOther.")
          case None =>
            JsError(s"HealthCheck.protocol missing.")
        }

      } catch {
        case _: ClassCastException => JsError(s"Expected an object but value '$json' does not look like one.")
      }
    }
  }

  implicit def healthCheckWrites[ E <: HealthCheck ]: Writes[HealthCheck] = new Writes[HealthCheck] {
    def writes(v: HealthCheck): JsValue = v.asJson()
  }

  implicit def healthCheckFormat: Format[HealthCheck] = {
    Format(HealthCheckUtils.healthCheckReads, HealthCheckUtils.healthCheckWrites)
  }

}
