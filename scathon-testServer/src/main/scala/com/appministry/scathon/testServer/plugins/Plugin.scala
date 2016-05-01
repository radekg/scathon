/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.testServer.plugins

import com.appministry.scathon.models.v2.Plugin
import com.twitter.finagle.http.{Request, Status, Response}
import com.twitter.io.{Buf, Reader}
import play.api.libs.json.{JsString, Json}

trait TPlugin {

  def info: Plugin
  def get(request: Request, path: String): Response = Response(Status.NoContent)
  def put(request: Request, path: String): Response = Response(Status.NoContent)
  def post(request: Request, path: String): Response = Response(Status.NoContent)
  def delete(request: Request, path: String): Response = Response(Status.NoContent)

}

class TestPlugin extends TPlugin {

  def info: Plugin = {
    Plugin(
      id = "test-plugin",
      plugin = this.getClass.getName,
      implementation = this.getClass.getName,
      tags = Some(List("test", "plugin")),
      info = Some(Map("test" -> JsString("value")))
    )
  }

  override def get(request: Request, path: String): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("GET")))
  }

  override def put(request: Request, path: String): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("PUT")))
  }

  override def post(request: Request, path: String): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("POST")))
  }

  override def delete(request: Request, path: String): Response = {
    Response(request.version, Status.Ok, Reader.fromBuf(Buf.Utf8("DELETE")))
  }

}