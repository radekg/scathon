/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

import com.appministry.scathon.testServer.plugins.TestPlugin
import com.twitter.finagle.http.{Method, Status}

class PluginsTests extends TestBase {

  "Plugins Tests" should {

    "retrieve a list of plugins" in {
      val testPlugin = new TestPlugin
      whenReady( client.getPlugins() ) { plugins =>
        plugins.length shouldBe(1)
        inside( plugins(0) ) {
          case plugin =>
            plugin.id shouldBe(testPlugin.info.id)
            plugin.implementation shouldBe(testPlugin.info.implementation)
        }
      }
    }

    "send a request and get a response from a plugin" in {
      whenReady( client.pluginExecuteRequest(Method.Get, "test-plugin", Some("/the/path/does/not/matter/here")) ) { response =>
        response.status shouldBe(Status.Ok)
        response.contentString shouldBe("GET")
      }
    }

    "receive an error for an unsupported method" in {
      whenReady(client.pluginExecuteRequest(Method.Head, "test-plugin", Some("/the/path/does/not/matter/here")).failed) { ex =>
        ex shouldBe a[NotAllowed]
        inside(ex.asInstanceOf[NotAllowed]) {
          case e =>
            e.status shouldBe(Status.MethodNotAllowed)
        }
      }
    }

  }

}
