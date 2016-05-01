/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

import com.twitter.finagle.http.Status

class LeaderTests extends TestBase {

  "Leader Tests" should {

    "get leader info from the server" in {
      whenReady( client.getLeader() ) { data =>
        data should matchPattern { case Some(_) => }
      }
    }

    "receive Ok when removing a leader" in {
      whenReady( client.deleteLeader() ) { status =>
        status shouldBe(Status.Ok)
      }
    }

    "receive NotFound when removing a leader (leader not there)" in {
      whenReady( client.deleteLeader().failed ) { ex =>
        ex shouldBe a[NotFound]
      }
    }

  }

}
