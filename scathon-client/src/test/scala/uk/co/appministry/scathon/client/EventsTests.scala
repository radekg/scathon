/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

import uk.co.appministry.scathon.models.v2.{Application, EventTypes}

class EventsTests extends TestBase {

  "Events Tests" should {

    "receive events as a stream" in {
      val toReceive = 5
      var received = 0
      client.streamEvents(toReceive) { event =>
        if (event.eventType == Some(EventTypes.api_post_event.toString)) received += 1
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
