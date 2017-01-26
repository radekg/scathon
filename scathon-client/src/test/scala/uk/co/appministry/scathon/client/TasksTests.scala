/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

import uk.co.appministry.scathon.client.utils.TestApplicationData
import uk.co.appministry.scathon.models.v2.Application

class TasksTests extends TestBase {

  "Tasks Tests" should {

    "create an application with tasks" in {
      val app = Application(id = TestApplicationData.id,
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
        healthChecks = Some(TestApplicationData.healthChecks))
      whenReady(client.createApp(app)) { createdApp =>
        eventually {
          whenReady(client.getAppTasks(TestApplicationData.id)) { tasks =>
            tasks.length shouldBe (TestApplicationData.instances)
          }
        }
      }
    }

    "get a list of tasks" in {
      whenReady( client.getTasks() ) { tasks =>
        tasks.length shouldBe( TestApplicationData.instances )
        tasks.head.appId shouldBe(TestApplicationData.id)
      }
    }

    "delete all tasks" in {
      whenReady( client.deleteTasks() ) { removedTasks =>
        removedTasks.length shouldBe( TestApplicationData.instances )
        whenReady( client.getTasks() ) { tasks =>
          tasks.length shouldBe( 0 )
        }
      }
    }



  }

}
