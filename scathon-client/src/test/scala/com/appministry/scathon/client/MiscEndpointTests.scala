/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

import com.appministry.scathon.models.v2.GetInfoResponse
import com.twitter.finagle.http.Status
import play.api.libs.json.JsValue

class MiscEndpointTests extends TestBase {

  "Misc Endpoints" should {

    "receive JsValue for metrics endpoint" in {
      whenReady( client.metrics() ) { value =>
        value shouldBe a[JsValue]
      }
    }

    "receive Ok for ping endpoint" in {
      whenReady( client.ping() ) { status =>
        status shouldBe( Status.Ok )
      }
    }

    "receive server info from getInfo endpoint" in {
      whenReady( client.getInfo() ) { response =>
        response shouldBe a[GetInfoResponse]
      }
    }

  }

}
