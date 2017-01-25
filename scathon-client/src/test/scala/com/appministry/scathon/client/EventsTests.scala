/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

import com.appministry.scathon.models.v2.Application

class EventsTests extends TestBase {

  "Events Tests" should {

    "receive events as a stream" in {
      val toReceive = 5
      var received = 0
      client.streamEvents(toReceive) { msg =>
        received += 1
      }

      (0 to toReceive).foreach { idx =>
        client.createApp(Application(id=s"application-$idx"))
      }

      eventually {
        received shouldBe(toReceive)
      }
    }

  }

}
