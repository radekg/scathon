/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.client

import org.scalatest.concurrent._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, WordSpec}
import uk.co.appministry.scathon.testServer.TestMarathon

class TestBase extends WordSpec
  with Eventually
  with ScalaFutures
  with Inside
  with Matchers
  with BeforeAndAfterAll
  with AsyncAssertions {

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(100, Millis)))

  var client: Client = _
  var server: TestMarathon = _

  override def beforeAll: Unit = {
    server = new TestMarathon
    server.start()
    client = new Client(port = server.port.get)
  }

  override def afterAll: Unit = {
    server.stop()
  }

}
