/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

class EventsTests extends TestBase {

  "Events Tests" should {

    "receive events as a stream" in {
      val toReceive = 5
      var received = 0
      client.streamEvents(toReceive) { msg =>
        received += 1
      }
      eventually {
        received shouldBe(toReceive)
      }
    }

  }

}
