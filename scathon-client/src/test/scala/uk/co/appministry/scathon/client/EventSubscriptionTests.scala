/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

import java.net.URI

import uk.co.appministry.scathon.models.v2.EventSubscriptionSubscribeEvent
import com.twitter.finagle.http.Status

class EventSubscriptionTests extends TestBase {

  "Event subscriptions operations" should {

    val callbackUri = "http://callback-uri.com/some-path"

    "create a subscription when requested" in {
      whenReady( client.createEventSubscription(new URI(callbackUri)) ) { event =>
        event shouldBe a[EventSubscriptionSubscribeEvent]
      }
    }

    "get a subscription list when requested" in {
      whenReady( client.getEventSubscriptions() ) { items =>
        items shouldBe( List(callbackUri) )
      }
    }

    "delete a subscription when requested" in {
      whenReady( client.deleteEventSubscription(new URI(callbackUri)) ) { status =>
        status shouldBe(Status.Ok)
        whenReady( client.getEventSubscriptions() ) { items =>
          items shouldBe( List.empty[String] )
        }
      }
    }

  }

}
