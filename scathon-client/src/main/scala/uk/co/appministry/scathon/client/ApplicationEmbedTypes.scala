/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

object ApplicationEmbedTypes extends Enumeration {
  type ApplicationEmbedType = Value
  val TASKS = Value("tasks")
  val COUNTS = Value("counts")
  val DEPLOYMENTS = Value("deployments")
  val LAST_TASK_FAILURE = Value("lastTaskFailures")
  val TASK_STATS = Value("taskStats")
}
