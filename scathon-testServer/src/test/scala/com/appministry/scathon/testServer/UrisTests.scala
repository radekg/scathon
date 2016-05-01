/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.testServer

import java.util.Properties

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http._
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{Eventually, AsyncAssertions}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Inside, BeforeAndAfterAll, Matchers, WordSpec}

class UrisTests extends WordSpec
  with Matchers
  with BeforeAndAfterAll
  with AsyncAssertions
  with Eventually
  with Inside {

  implicit override val patienceConfig = PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(100, Millis)))

  var server: TestMarathon = _
  var clientHost: String = _
  var testClient: Service[Request, Response] = _

  override def beforeAll: Unit = {
    val props = new Properties()
    props.setProperty("testMarathon.for-unit-tests", "true")
    server = new TestMarathon(Some(ConfigFactory.parseProperties(props)))
    server.start()
    clientHost = s"localhost:${server.port.get}"
    testClient = Http.client.withLabel("Test Client").newService(clientHost)
  }

  override def afterAll: Unit = {
    server.stop()
  }

  private def request(method: Method, endpoint: String): Request = {
    val request = Request(endpoint)
    request.method = method
    request.host = clientHost
    request
  }

  private def withExpectedResult(request: Request, expected: String): Unit = {
    val w = new Waiter
    testClient(request).onSuccess { resp =>
      resp.contentString shouldBe (expected)
      w.dismiss()
    }.onFailure { ex =>
      fail(ex)
    }
    w.await()
  }

  "TestMarathon GET Uris" should {

    "GET /v2/apps" in {
      withExpectedResult(request(Method.Get, "/v2/apps"), "GET:apps")
    }

    "GET /v2/apps/:id" in {
      withExpectedResult(request(Method.Get, "/v2/apps/appId"), "GET:app:appId")
    }

    "GET /v2/apps/:id/tasks" in {
      withExpectedResult(request(Method.Get, "/v2/apps/appId/tasks"), "GET:appTasks:appId")
    }

    "GET /v2/apps/:id/versions" in {
      withExpectedResult(request(Method.Get, "/v2/apps/appId/versions"), "GET:appVersions:appId")
    }

    "GET /v2/apps/:id/versions/:version" in {
      withExpectedResult(request(Method.Get, "/v2/apps/appId/versions/test-version"), "GET:appVersion:appId:test-version")
    }

    "GET /v2/deployments" in {
      withExpectedResult(request(Method.Get, "/v2/deployments"), "GET:deployments")
    }

    "GET /v2/groups" in {
      withExpectedResult(request(Method.Get, "/v2/groups"), "GET:groups")
    }

    "GET /v2/groups/versions" in {
      withExpectedResult(request(Method.Get, "/v2/groups/versions"), "GET:groupVersions")
    }

    "GET /v2/groups/:group-id" in {
      withExpectedResult(request(Method.Get, "/v2/groups/../../test-group"), "GET:group:../../test-group")
    }

    "GET /v2/groups/:group-id/versions" in {
      withExpectedResult(request(Method.Get, "/v2/groups/../../test-group/versions"), "GET:groupVersions:../../test-group")
    }

    "GET /v2/artifacts/:some/artifact/path" in {
      withExpectedResult(request(Method.Get, "/v2/artifacts/this/is/the/path"), "GET:artifact:this/is/the/path")
    }

    "GET /v2/events" in {
      withExpectedResult(request(Method.Get, "/v2/events"), "GET:events")
    }

    "GET /v2/eventSubscriptions" in {
      withExpectedResult(request(Method.Get, "/v2/eventSubscriptions"), "GET:eventSubscriptions")
    }

    "GET /v2/info" in {
      withExpectedResult(request(Method.Get, "/v2/info"), "GET:info")
    }

    "GET /v2/leader" in {
      withExpectedResult(request(Method.Get, "/v2/leader"), "GET:leader")
    }

    "GET /v2/plugins" in {
      withExpectedResult(request(Method.Get, "/v2/plugins"), "GET:plugins")
    }

    "GET /v2/plugins/:plugin-id/:path" in {
      withExpectedResult(request(Method.Get, "/v2/plugins/org.example.Plugin//this/is/the/path"), "GET:plugin:org.example.Plugin:/this/is/the/path")
    }

    "GET /v2/queue" in {
      withExpectedResult(request(Method.Get, "/v2/queue"), "GET:queue")
    }

    "GET /ping" in {
      withExpectedResult(request(Method.Get, "/ping"), "GET:ping")
    }

    "GET /metrics" in {
      withExpectedResult(request(Method.Get, "/metrics"), "GET:metrics")
    }

    "GET /logging" in {
      withExpectedResult(request(Method.Get, "/logging"), "GET:logging")
    }

    "GET /help" in {
      withExpectedResult(request(Method.Get, "/help"), "GET:help")
    }

  }

  "TestMarathon POST Uris" should {

    "POST /v2/apps" in {
      withExpectedResult(request(Method.Post, "/v2/apps"), "POST:apps")
    }

    "POST /v2/apps/:id/restart" in {
      withExpectedResult(request(Method.Post, "/v2/apps/appId/restart"), "POST:appRestart:appId")
    }

    "POST /v2/groups" in {
      withExpectedResult(request(Method.Post, "/v2/groups"), "POST:groups")
    }

    "POST /v2/groups/:group-id" in {
      withExpectedResult(request(Method.Post, "/v2/groups/groupId"), "POST:group:groupId")
    }

    "POST /v2/tasks/delete" in {
      withExpectedResult(request(Method.Post, "/v2/tasks/delete"), "POST:tasksDelete")
    }

    "POST /v2/artifacts" in {
      withExpectedResult(request(Method.Post, "/v2/artifacts"), "POST:artifacts")
    }

    "POST /v2/artifacts/:path" in {
      withExpectedResult(request(Method.Post, "/v2/artifacts/this/is/the/path"), "POST:artifact:this/is/the/path")
    }

    "POST /v2/eventSubscriptions" in {
      withExpectedResult(request(Method.Post, "/v2/eventSubscriptions"), "POST:eventSubscriptions")
    }

    "POST /v2/plugins/:plugin/:path" in {
      withExpectedResult(request(Method.Post, "/v2/plugins/org.example.Plugin//this/is/the/path"), "POST:plugin:org.example.Plugin:/this/is/the/path")
    }

  }

  "TestMarathon PUT Uris" should {

    "PUT /v2/apps" in {
      withExpectedResult(request(Method.Put, "/v2/apps"), "PUT:apps")
    }

    "PUT /v2/apps/:id" in {
      withExpectedResult(request(Method.Put, "/v2/apps/appId"), "PUT:app:appId")
    }

    "PUT /v2/groups" in {
      withExpectedResult(request(Method.Put, "/v2/groups"), "PUT:groups")
    }

    "PUT /v2/groups/:group-id" in {
      withExpectedResult(request(Method.Put, "/v2/groups/groupId"), "PUT:group:groupId")
    }

    ( "PUT /v2/artifacts/:path" ) ignore {
      // This test is ignored as there's no out of the box way to handle PUT multipart data in Finagle
      // no need to hassle with this, the client can upload the artifacts using POST and PUT does not seem to
      // be magical in any way.
      // Furthermore, TestMarathon does not handle PUT /v2/artifacts/:path.
      withExpectedResult(request(Method.Put, "/v2/artifacts/this/is/the/path"), "PUT:artifact:this/is/the/path")
    }

    "PUT /v2/plugins/:plugin/:path" in {
      withExpectedResult(request(Method.Put, "/v2/plugins/org.example.Plugin//this/is/the/path"), "PUT:plugin:org.example.Plugin:/this/is/the/path")
    }

  }

  "TestMarathon DELETE Uris" should {

    "DELETE /v2/apps/:id" in {
      withExpectedResult(request(Method.Delete, "/v2/apps/appId"), "DELETE:app:appId")
    }

    "DELETE /v2/apps/:id/tasks" in {
      withExpectedResult(request(Method.Delete, "/v2/apps/appId/tasks"), "DELETE:appTasks:appId")
    }

    "DELETE /v2/apps/:id/tasks/:task-id" in {
      withExpectedResult(request(Method.Delete, "/v2/apps/appId/tasks/taskId"), "DELETE:appTask:appId:taskId")
    }

    "DELETE /v2/deployments/:deployment-id" in {
      withExpectedResult(request(Method.Delete, "/v2/deployments/deploymentId"), "DELETE:deployment:deploymentId")
    }

    "DELETE /v2/groups" in {
      withExpectedResult(request(Method.Delete, "/v2/groups"), "DELETE:groups")
    }

    "DELETE /v2/groups/:group-id" in {
      withExpectedResult(request(Method.Delete, "/v2/groups/groupId"), "DELETE:group:groupId")
    }

    "DELETE /v2/artifacts/:path" in {
      withExpectedResult(request(Method.Delete, "/v2/artifacts/this/is/the/path"), "DELETE:artifact:this/is/the/path")
    }

    "DELETE /v2/eventSubscriptions" in {
      withExpectedResult(request(Method.Delete, "/v2/eventSubscriptions"), "DELETE:eventSubscriptions")
    }

    "DELETE /v2/leader" in {
      withExpectedResult(request(Method.Delete, "/v2/leader"), "DELETE:leader")
    }

    "DELETE /v2/plugins/:plugin/:path" in {
      withExpectedResult(request(Method.Delete, "/v2/plugins/org.example.Plugin//this/is/the/path"), "DELETE:plugin:org.example.Plugin:/this/is/the/path")
    }

    "DELETE /v2/queue/:app-id/delay" in {
      withExpectedResult(request(Method.Delete, "/v2/queue/appId/delay"), "DELETE:queueAppDelay:appId")
    }

  }

}