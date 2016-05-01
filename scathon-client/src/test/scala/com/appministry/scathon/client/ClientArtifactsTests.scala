/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client

import java.nio.file.{Files, Paths}

import com.twitter.finagle.http.Status

class ClientArtifactsTests extends TestBase {

  "Client Artifacts" should {

    "upload an artifact without a path" in {
      val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
      whenReady( client.uploadArtifact(bytes, "marathon.jpg") ) { result =>
        result._1 shouldBe(Status.Created)
        result._2 shouldBe( Some( s"http://${server.host.get}:${server.port.get}/v2/artifacts/marathon.jpg" ) )
      }
    }

    "upload an artifact with a path" in {
      val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
      whenReady( client.uploadArtifact(bytes, "marathon.jpg", Some("this/is/a/path/marathon.jpg")) ) { result =>
        result._1 shouldBe(Status.Created)
        result._2 shouldBe( Some( s"http://${server.host.get}:${server.port.get}/v2/artifacts/this/is/a/path/marathon.jpg" ) )
      }
    }

    "get an uploaded artifact" in {
      whenReady( client.getArtifact("marathon.jpg") ) { data =>
        val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
        bytes shouldBe( data )
      }
      whenReady( client.getArtifact("this/is/a/path/marathon.jpg") ) { data =>
        val bytes = Files.readAllBytes(Paths.get(this.getClass.getClassLoader.getResource("marathon.jpg").toURI))
        bytes shouldBe( data )
      }
    }

    "delete an artifact" in {
      whenReady( client.deleteArtifact("this/is/a/path/marathon.jpg") ) { status =>
        status shouldBe( Status.Ok )
        whenReady( client.deleteArtifact("this/is/a/path/marathon.jpg").failed ) { ex =>
          ex shouldBe a[MarathonClientException]
          ex.asInstanceOf[MarathonClientException].status shouldBe (Status.NotFound)
        }
      }
    }

  }

}
