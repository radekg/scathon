/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.models.mesos

import play.api.libs.json._

trait Constraint {
  def toJson(): JsArray
}

case class UniqueConstraint(val attribute: String) extends Constraint {
  def toJson(): JsArray = {
    JsArray( Seq( JsString(attribute), JsString("UNIQUE") ) )
  }
}

case class ClusterConstraint(val attribute: String, val value: String) extends Constraint {
  def toJson(): JsArray = {
    JsArray(Seq( JsString(attribute), JsString("CLUSTER"), JsString(value) ))
  }
}

case class GroupByConstraint(val attribute: String, val value: Option[String]=None) extends Constraint {
  def toJson(): JsArray = {
    value match {
      case Some(v) => JsArray( Seq( JsString(attribute), JsString("GROUP_BY"), JsString(v) ) )
      case None => JsArray( Seq( JsString(attribute), JsString("GROUP_BY") ) )
    }
  }
}

case class LikeConstraint(val attribute: String, val value: String) extends Constraint {
  def toJson(): JsArray = {
    JsArray( Seq( JsString(attribute), JsString("LIKE"), JsString(value) ) )
  }
}

case class UnlikeConstraint(val attribute: String, val value: String) extends Constraint {
  def toJson(): JsArray = {
    JsArray( Seq( JsString(attribute), JsString("UNLIKE"), JsString(value) ) )
  }
}

trait ConstraintParser {
  implicit val mesosAppConstraintFormat = MesosConstraintUtils.appConstraintFormat
}