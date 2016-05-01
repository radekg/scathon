/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class QueueItem(
           val app: Application,
           val count: Int,
           val delay: QueueDelay ) extends MarathonApiObject

case class QueueDelay(
           val timeLeftSeconds: Int,
           val overdue: Boolean ) extends MarathonApiObject

trait QueueItemParser extends ApplicationParser with QueueDelayParser {
  implicit val queueItemFormat: Format[QueueItem] = (
    ( __ \ "app" ).format[Application] and
    ( __ \ "count" ).format[Int] and
    ( __ \ "delay" ).format[QueueDelay]
  )(QueueItem.apply, unlift(QueueItem.unapply))
}

trait QueueDelayParser {
  implicit val queueDelayFormat: Format[QueueDelay] = (
    ( __ \ "timeLeftSeconds" ).format[Int] and
    ( __ \ "overdue" ).format[Boolean]
  )(QueueDelay.apply, unlift(QueueDelay.unapply))
}