/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.v2

import org.scalatest.{Matchers, WordSpec}

class VersionTests extends WordSpec with Matchers {

  "Version" should {
    "convert both ways" in {
      val version = Version()
      Version( Version(version) ) shouldBe( version )
    }
  }

}
