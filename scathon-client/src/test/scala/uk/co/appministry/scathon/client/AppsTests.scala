/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

import java.net.URI

import uk.co.appministry.scathon.client.callbackServer.CallbackServer
import uk.co.appministry.scathon.client.utils.TestApplicationData
import uk.co.appministry.scathon.models.v2.{Application, EventSubscriptionSubscribeEvent}
import org.joda.time.DateTime

class AppsTests extends TestBase {

  private var callbackServer = new CallbackServer

  private var createdAppVersion: DateTime = _

  override def beforeAll: Unit = {
    super.beforeAll
    callbackServer.start
  }

  override def afterAll: Unit = {
    callbackServer.stop
    super.afterAll
  }

  "Apps Tests" should {

    "create a subscription to verify receiving callbacks from an event bus" in {
      whenReady( client.createEventSubscription(new URI(s"http://localhost:${callbackServer.port.get}${callbackServer.callbackUri}")) ) { event =>
        event shouldBe a[EventSubscriptionSubscribeEvent]
      }
    }

    "create an application and queue should contain items" in {
      val app = Application( id = TestApplicationData.id,
                             cmd = Some(TestApplicationData.cmd),
                             args = Some(TestApplicationData.args),
                             ports = Some(TestApplicationData.ports),
                             instances = TestApplicationData.instances,
                             container = Some(TestApplicationData.container),
                             env = TestApplicationData.env,
                             labels = TestApplicationData.labels,
                             acceptedResourceRoles = Some(TestApplicationData.acceptedRoles),
                             constraints = Some(TestApplicationData.constraints),
                             fetch = Some(TestApplicationData.fetch),
                             healthChecks = Some(TestApplicationData.healthChecks) )
      whenReady( client.createApp(app) ) { createdApp =>
        createdApp.id shouldBe( app.id )
        createdApp.cmd shouldBe( app.cmd )
        createdApp.args shouldBe( app.args )
        createdApp.ports shouldBe( app.ports )
        createdApp.instances shouldBe( app.instances )
        createdApp.container shouldBe( app.container )
        createdApp.env shouldBe( app.env )
        createdApp.labels shouldBe( app.labels )
        createdApp.acceptedResourceRoles shouldBe( app.acceptedResourceRoles )
        createdApp.constraints shouldBe( app.constraints )
        createdApp.fetch shouldBe( app.fetch )
        createdApp.healthChecks shouldBe( app.healthChecks )
      }

      whenReady( client.getQueue() ) { queue =>
        queue.length should be > 0
        queue.head.count shouldBe( app.instances )
        inside(queue.head.app) {
          case queueApp =>
            queueApp.id shouldBe( app.id )
            queueApp.cmd shouldBe( app.cmd )
            queueApp.args shouldBe( app.args )
            queueApp.ports shouldBe( app.ports )
            queueApp.instances shouldBe( app.instances )
            queueApp.container shouldBe( app.container )
            queueApp.env shouldBe( app.env )
            queueApp.labels shouldBe( app.labels )
            queueApp.acceptedResourceRoles shouldBe( app.acceptedResourceRoles )
            queueApp.constraints shouldBe( app.constraints )
            queueApp.fetch shouldBe( app.fetch )
            queueApp.healthChecks shouldBe( app.healthChecks )
        }
      }

      eventually {
        whenReady( client.getDeployments() ) { deployments =>
          deployments.length should be > 0
        }
      }

      eventually {
        whenReady( client.getAppTasks(TestApplicationData.id) ) { tasks =>
          tasks.length shouldBe( TestApplicationData.instances )
        }
      }
    }

    "get a list of applications" in {
      whenReady( client.getApps() ) { apps =>
        apps.length should be > 0
        inside(apps.head) {
          case app =>
            app.id shouldBe( TestApplicationData.id )
            app.cmd shouldBe( Some(TestApplicationData.cmd) )
            app.args shouldBe( Some(TestApplicationData.args) )
            app.ports shouldBe( Some(TestApplicationData.ports) )
            app.instances shouldBe( TestApplicationData.instances )
            app.container shouldBe( Some(TestApplicationData.container) )
            app.env shouldBe( TestApplicationData.env )
            app.labels shouldBe( TestApplicationData.labels )
            app.acceptedResourceRoles shouldBe( Some(TestApplicationData.acceptedRoles) )
            app.constraints shouldBe( Some(TestApplicationData.constraints) )
            app.fetch shouldBe( Some(TestApplicationData.fetch) )
            app.healthChecks shouldBe( Some(TestApplicationData.healthChecks) )
            app.version should matchPattern { case Some(_) => }
            inside(app.version) {
              case Some(v) =>
                createdAppVersion = v
            }
        }
      }
    }

    "be able to kill an application task by ID" in {
      whenReady( client.getAppTasks(TestApplicationData.id) ) { tasks =>
        tasks.length shouldBe( TestApplicationData.instances )
        whenReady( client.deleteAppTask(TestApplicationData.id, tasks.head.id) ) { response =>
          response shouldBe a[List[_]]
          whenReady( client.getAppTasks(TestApplicationData.id) ) { tasks =>
            tasks.length shouldBe( TestApplicationData.instances-1 )
          }
        }
      }
    }

    "be able to kill remaining application tasks" in {
      whenReady( client.deleteAppTasks(TestApplicationData.id) ) { response =>
        response shouldBe a[List[_]]
        whenReady( client.getAppTasks(TestApplicationData.id) ) { tasks =>
          tasks.length shouldBe( 0 )
        }
      }
    }

    "have api notification going through" in {
      eventually {
        callbackServer.apiRequestCount should be > 0L
      }
    }

    "get an application by ID" in {
      whenReady( client.getApp( TestApplicationData.id ) ) { app =>
        app.id shouldBe( TestApplicationData.id )
        app.cmd shouldBe( Some(TestApplicationData.cmd) )
        app.args shouldBe( Some(TestApplicationData.args) )
        app.ports shouldBe( Some(TestApplicationData.ports) )
        app.instances shouldBe( TestApplicationData.instances )
        app.container shouldBe( Some(TestApplicationData.container) )
        app.env shouldBe( TestApplicationData.env )
        app.labels shouldBe( TestApplicationData.labels )
        app.acceptedResourceRoles shouldBe( Some(TestApplicationData.acceptedRoles) )
        app.constraints shouldBe( Some(TestApplicationData.constraints) )
        app.fetch shouldBe( Some(TestApplicationData.fetch) )
        app.healthChecks shouldBe( Some(TestApplicationData.healthChecks) )
        app.version should matchPattern { case Some(_) => }
      }
    }

    "get application versions" in {
      whenReady( client.getAppVersions(TestApplicationData.id) ) { versions =>
        versions.length should be > 0
      }
    }

    "get application by version" in {
      whenReady( client.getAppVersion(TestApplicationData.id, createdAppVersion) ) { app =>
        app.id shouldBe( TestApplicationData.id )
        app.cmd shouldBe( Some(TestApplicationData.cmd) )
        app.args shouldBe( Some(TestApplicationData.args) )
        app.ports shouldBe( Some(TestApplicationData.ports) )
        app.instances shouldBe( TestApplicationData.instances )
        app.container shouldBe( Some(TestApplicationData.container) )
        app.env shouldBe( TestApplicationData.env )
        app.labels shouldBe( TestApplicationData.labels )
        app.acceptedResourceRoles shouldBe( Some(TestApplicationData.acceptedRoles) )
        app.constraints shouldBe( Some(TestApplicationData.constraints) )
        app.fetch shouldBe( Some(TestApplicationData.fetch) )
        app.healthChecks shouldBe( Some(TestApplicationData.healthChecks) )
        app.version shouldBe( Some(createdAppVersion) )
      }
    }

    "eventually receive a new version after restarting the application" in {
      whenReady( client.restartApp(TestApplicationData.id) ) { deploymentId =>
        eventually {
          whenReady( client.getApp(TestApplicationData.id) ) { result =>
            Some(createdAppVersion).toString should not be result.toString
          }
        }
      }
    }

    "eventually not find an application after removing" in {
      whenReady( client.deleteApp(TestApplicationData.id) ) { deploymentId =>
        eventually {
          whenReady(client.getApp(TestApplicationData.id).failed) { ex =>
            ex shouldBe a[NotFound]
          }
        }
      }
    }

    "receive 404 when removing a non-existing application" in {
      whenReady(client.getApp("/non-existing-application").failed) { ex =>
        ex shouldBe a[NotFound]
      }
    }

    "receive 404 restarting a non-existing application" in {
      whenReady(client.restartApp("/non-existing-application").failed) { ex =>
        ex shouldBe a[NotFound]
      }
    }

    "receive 404 removing a non-existing application" in {
      whenReady(client.deleteApp("/non-existing-application").failed) { ex =>
        ex shouldBe a[NotFound]
      }
    }

    "receive 404 removing a non-existing deployment" in {
      whenReady(client.deleteDeployment("non-existing-deployment").failed) { ex =>
        ex shouldBe a[NotFound]
      }
    }

    "receive 404 resetting a queue delay for a non-existing application" in {
      whenReady(client.deleteQueue("non-existing-app").failed) { ex =>
        ex shouldBe a[NotFound]
      }
    }

  }

}
