/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

import com.twitter.finagle.http.Status

class MarathonClientException(val status: Status, val message: String) extends Exception(message)
case class Forbidden(override val status: Status, override val message: String) extends MarathonClientException(status, message)
case class Unauthorized(override val status: Status, override val message: String) extends MarathonClientException(status, message)
case class NotFound(override val status: Status, override val message: String) extends MarathonClientException(status, message)
case class NotAllowed(override val status: Status, override val message: String) extends MarathonClientException(status, message)
case class InvalidResponse(override val status: Status, override val message: String) extends MarathonClientException(status, message)
case class UnknownResponse(override val status: Status, override val message: String) extends MarathonClientException(status, message)
case class UnknownBinaryResponse(override val status: Status, override val message: String="Binary truncated.") extends MarathonClientException(status, message)
