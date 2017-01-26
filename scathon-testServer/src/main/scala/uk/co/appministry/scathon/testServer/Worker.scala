/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.testServer

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ConcurrentHashMap, ScheduledExecutorService, TimeUnit}
import java.util.{TimerTask, UUID}

import uk.co.appministry.scathon.models.v2._

import scala.collection.JavaConversions._

trait Scheduled extends Runnable {

  class ScheduledWorker(val executor: ScheduledExecutorService, val work: Runnable) extends TimerTask {
    override def run(): Unit = {
      executor.execute(work)
    }
  }

  private def randomDelay(): Long = {
    val range = 100 to 150
    range(rnd.nextInt(range length))
  }

  val rnd = new scala.util.Random
  def executor: ScheduledExecutorService

  private val _working: AtomicBoolean = new AtomicBoolean(true)
  def isWorking: Boolean = _working.get()
  def startWork(): Unit = {
    _working.set(true)
    schedule()
  }
  def stopWork(): Unit = _working.set(false)

  def schedule(): Unit = {
    if (isWorking) {
      executor.schedule(this, randomDelay(), TimeUnit.MILLISECONDS)
    }

  }
}

class DeploymentWorker(val marathon: TestMarathon) extends Scheduled {
  override def executor: ScheduledExecutorService = marathon.executor
  override def run(): Unit = {
    if (isWorking) {
      Option(marathon.deploymentQueue.peekFirst()) match {
        case Some(x) =>
          if (System.currentTimeMillis() - x.version.getMillis >= 1000) {
            Option(marathon.deploymentQueue.pollFirst()) match {
              case Some(deployment) =>
                schedule()
                process(deployment)
              case None => schedule()
            }
          } else {
            schedule()
          }
        case None => schedule()
      }
    }
  }
  private def process(deployment: Deployment): Unit = {
    // This is very simplistic approach.
    // We do not change the currentActions, neither currentSteps at all.
    deployment.steps.head.foreach { step =>
      step.action match {
        case DeploymentActionTypes.START_APPLICATION =>
        case DeploymentActionTypes.SCALE_APPLICATION =>

          Option(marathon.apps.get(step.app)) match {
            case None => /* nothing to do */
            case Some(app) =>
              val required = app.instances
              val tasks = marathon.appsTasks.getOrDefault(step.app, new ConcurrentHashMap[String, Task]())
              val existing = tasks.values().toList.length
              if ( required > existing ) {
                for ( i <- 0 until (required - existing) ) {
                  val task = Task(
                    id = UUID.randomUUID().toString,
                    host = "localhost",
                    ports = app.ports.getOrElse(List.empty[Int]),
                    servicePorts = List.empty[Int],
                    startedAt = Some(Version()),
                    stagedAt = Some(Version()),
                    version = Version(),
                    appId = app.id,
                    slaveId = s"slave-${UUID.randomUUID().toString}" )
                  tasks.put(task.id, task)
                }
                marathon.appsTasks.put(app.id, tasks)
              }
          }

        case DeploymentActionTypes.STOP_APPLICATION =>

          marathon.apps.remove(step.app)
          marathon.appsTasks.remove(step.app)

        case DeploymentActionTypes.RESTART_APPLICATION =>

          // just change the version of the application
          val app = marathon.apps.remove(step.app)
          marathon.apps.put(app.id, app.copy(version = Some(Version())))

      }
    }
  }
}

class QueueWorker(val marathon: TestMarathon) extends Scheduled {
  override def executor: ScheduledExecutorService = marathon.executor
  override def run(): Unit = {
    Option(marathon.queue.peekFirst()) match {
      case None => schedule()
      case Some(x) =>
        if (System.currentTimeMillis() - x.app.version.get.getMillis >= 1000) {
          Option(marathon.queue.pollFirst()) match {
            case Some(queueItem) =>
              val deployment = Deployment(id = UUID.randomUUID().toString,
                                          version = Version(),
                                          affectedApps = List(queueItem.app.id),
                                          steps = List(List(DeploymentStep(
                                            action = DeploymentActionTypes.SCALE_APPLICATION,
                                            app = queueItem.app.id
                                          ))),
                                          currentActions = List(DeploymentCurrentAction(
                                            action = DeploymentActionTypes.SCALE_APPLICATION,
                                            app = queueItem.app.id
                                          )),
                                          currentStep = 1,
                                          totalSteps = 1)
              marathon.addDeployment(deployment)
              schedule()
            case None => schedule()
          }
        } else {
          schedule()
        }
    }
  }

}