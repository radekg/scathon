/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.mesos

import play.api.libs.json._

object MesosConstraintUtils {

  def appConstraintReads[E <: Constraint]: Reads[Constraint] = new Reads[Constraint] {
    def reads(json: JsValue): JsResult[Constraint] = {
      try {
        val arr = json.asInstanceOf[JsArray]
        arr.value.toList match {
          case JsString(attr) :: JsString(op) :: Nil if op == "UNIQUE" => JsSuccess(UniqueConstraint(attr))
          case JsString(attr) :: JsString(op) :: Nil if op == "GROUP_BY" => JsSuccess(GroupByConstraint(attr))
          case JsString(attr) :: JsString(op) :: JsString(value) :: Nil if op == "CLUSTER" => JsSuccess(ClusterConstraint(attr, value))
          case JsString(attr) :: JsString(op) :: JsString(value) :: Nil if op == "GROUP_BY" => JsSuccess(GroupByConstraint(attr, Some(value)))
          case JsString(attr) :: JsString(op) :: JsString(value) :: Nil if op == "LIKE" => JsSuccess(LikeConstraint(attr, value))
          case JsString(attr) :: JsString(op) :: JsString(value) :: Nil if op == "UNLIKE" => JsSuccess(UnlikeConstraint(attr, value))
          case _ => JsError(s"Expected an Array(String, String) or Array(String, String, String) but value '$json' does not look like one.")
        }
      } catch {
        case _: ClassCastException => JsError(s"Expected an array but value '$json' does not look like one.")
      }
    }
  }

  implicit def appConstraintWrites[ E <: Constraint ]: Writes[Constraint] = new Writes[Constraint] {
    def writes(v: Constraint): JsValue = v.toJson()
  }

  implicit def appConstraintFormat: Format[Constraint] = {
    Format(MesosConstraintUtils.appConstraintReads, MesosConstraintUtils.appConstraintWrites)
  }

}
