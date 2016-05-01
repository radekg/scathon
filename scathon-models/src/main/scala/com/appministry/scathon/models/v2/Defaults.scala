/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

object ApplicationDefaults {
  val cpus: Double = 1
  val mem: Double = 1024
  val disk: Double = 0
  val requirePorts: Boolean = false
  val instances: Int = 1
  val env: Map[String, String] = Map.empty[String, String]
  val labels: Map[String, String] = Map.empty[String, String]
  val backoffSeconds: Int = 1
  val backoffFactor: Double = 1.15
  val maxLaunchDelaySeconds: Int = 3600
}

object HealthCheckDefaults {
  val path: String = "/"
  val gracePeriodSeconds: Int = 3
  val intervalSeconds: Int = 5
  val timeoutSeconds: Int = 5
  val maxConsecutiveFailures: Int = 3
}
