/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.v2.util

import uk.co.appministry.scathon.models.v2.Version
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

object VersionUtils {

  val format = DateTimeFormat.forPattern("YYYY-MM-DD'T'HH:mm:ss.SSS'Z'").withZoneUTC()

  def dateTimeReads[E <: DateTime]: Reads[DateTime] = new Reads[DateTime] {
    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) => {
        try {
          JsSuccess(Version(s))
        } catch {
          case _: IllegalArgumentException => JsError(s"Expected a String representation of a date. Value '$s' does not look like one.")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def dateTimeWrites[ E <: DateTime ]: Writes[DateTime] = new Writes[DateTime] {
    def writes(v: DateTime): JsValue = JsString(Version(v))
  }

  implicit def dateTimeformat: Format[DateTime] = {
    Format(VersionUtils.dateTimeReads, VersionUtils.dateTimeWrites)
  }

}
