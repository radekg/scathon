/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package uk.co.appministry.scathon.testServer

import java.nio.file.{Files, Paths}

import com.twitter.finagle.http._
import com.twitter.finagle.{Http, Service}
import com.twitter.io.Buf
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class ArtifactUploadTests extends WordSpec
  with Matchers
  with BeforeAndAfterAll
  with AsyncAssertions {

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(100, Millis)))

  var server: TestMarathon = _
  var clientHost: String = _
  var testClient: Service[Request, Response] = _

  override def beforeAll: Unit = {
    server = new TestMarathon()
    server.start()
    clientHost = s"localhost:${server.port.get}"
    testClient = Http.client.withLabel("Test Client").newService(clientHost)
  }

  override def afterAll: Unit = {
    server.stop()
  }

  "FileUploads" should {

    "upload an artifact into a specific directory with file name" in {
      val w = new Waiter()
      val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
      val request = RequestBuilder().url(s"http://${clientHost}/v2/artifacts/some/path/here/spark.txt")
        .add(FileElement("file", Buf.ByteArray.Owned(bytes), Some("image/jpeg"), Some("marathon.jpg")))
        .buildFormPost(multipart = true)
      testClient( request ).onSuccess { resp =>
        resp.statusCode shouldBe(201)
        w.dismiss()
      }.onFailure { ex =>
        fail(ex)
        w.dismiss()
      }
      w.await()
    }

    "upload an artifact into a specific directory without file name" in {
      val w = new Waiter()
      val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
      val request = RequestBuilder().url(s"http://${clientHost}/v2/artifacts/some/path/here/")
        .add(FileElement("file", Buf.ByteArray.Owned(bytes), Some("image/jpeg"), Some("marathon.jpg")))
        .buildFormPost(multipart = true)
      testClient( request ).onSuccess { resp =>
        resp.statusCode shouldBe(201)
        w.dismiss()
      }.onFailure { ex =>
        fail(ex)
        w.dismiss()
      }
      w.await()
    }

    "upload an artifact without a path" in {
      val w = new Waiter()
      val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
      val request = RequestBuilder().url(s"http://${clientHost}/v2/artifacts")
        .add(FileElement("file", Buf.ByteArray.Owned(bytes), Some("image/jpeg"), Some("marathon.jpg")))
        .buildFormPost(multipart = true)
      testClient( request ).onSuccess { resp =>
        resp.statusCode shouldBe(201)
        w.dismiss()
      }.onFailure { ex =>
        fail(ex)
        w.dismiss()
      }
      w.await()
    }

  }

}
