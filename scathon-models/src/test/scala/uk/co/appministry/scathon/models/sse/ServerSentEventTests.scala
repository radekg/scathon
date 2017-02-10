package uk.co.appministry.scathon.models.sse

import java.util.Scanner

import org.scalatest.{Inside, Matchers, WordSpec}

class ServerSentEventTests extends WordSpec
  with Matchers
  with Inside {

  "ServerSentEvent" must {

    "parse to the String form and back" in {

      val eventType = "test_event"
      val data = "test event data"
      val event = ServerSentEvent(eventType = Some(eventType), data = Some(data))
      val stringEvent = event.toString()
      ServerSentEventParser.parse(stringEvent) should matchPattern {
        case Some(event) =>
      }

    }

  }
}
