/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.models.mesos

import com.appministry.scathon.models.v2.{Application, ApplicationParser}
import org.scalatest.{Inside, Matchers, WordSpec}
import play.api.libs.json.Json

class MesosConstraintsTests extends WordSpec
  with Matchers
  with ApplicationParser
  with ConstraintParser
  with Inside {

  "ConstraintParser" should {
    "parse valid looking constraints" in {
      Json.parse("""["hostname", "UNIQUE"]""").as[Constraint] shouldBe( UniqueConstraint("hostname") )
      Json.parse("""["rack", "CLUSTER", "value"]""").as[Constraint] shouldBe( ClusterConstraint("rack", "value") )
      Json.parse("""["hostname", "GROUP_BY"]""").as[Constraint] shouldBe( GroupByConstraint("hostname") )
      Json.parse("""["hostname", "GROUP_BY", "with_value"]""").as[Constraint] shouldBe( GroupByConstraint("hostname", Some("with_value")) )
      Json.parse("""["rack", "LIKE", "rack-[1-3]"]""").as[Constraint] shouldBe( LikeConstraint("rack", "rack-[1-3]") )
      Json.parse("""["rack", "UNLIKE", "rack-[1-3]"]""").as[Constraint] shouldBe( UnlikeConstraint("rack", "rack-[1-3]") )
    }

    "parse an application with constraints" in {

      val constraints = List(
        UniqueConstraint("hostname"),
        ClusterConstraint("rack", "value"),
        GroupByConstraint("hostname"),
        GroupByConstraint("hostname", Some("with_value")),
        LikeConstraint("rack", "rack-[1-3]"),
        UnlikeConstraint("rack", "rack-[1-3]") )

      val parsed = applicationFormat.writes( Application(
        id = "/some-test-id",
        constraints = Some(constraints) ) ).toString()

      inside( Json.parse(parsed).asOpt[ Application ] ) {
        case Some(parsedApp) =>
          inside( parsedApp.constraints ) {
            case Some(vals) => vals should be(constraints)
            case None => fail("Expected Some, got None.")
          }
        case None => fail("Expected Some, got None.")
      }

    }

  }

}
