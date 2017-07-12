package com.thoughtworks.verify.pact

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

import scala.collection.mutable.Stack

/**
  * Created by xfwu on 12/07/2017.
  */
class MatchingRulesSpec extends FlatSpec with Matchers {

  "Matching Rules" should "parse matchingRule from string" in {

    val s = """ {"matchingRules": {
                                  "$.body[0].id": {
                                      "match": "type"
                                  },
                                  "$.body[1].timestamp": {
                                      "timestamp": "yyyy-MM-dd'T'HH:mm:ss"
                                  },
                                  "$.body[1].id": {
                                      "match": "type"
                                  }
                              }
                              }"""

    val json = Json.parse(s)
    val matchingRules = MatchingRules(json)
    matchingRules.size should be(3)
    val firstRule = matchingRules.head
    firstRule.selection should be("$.body[0].id")
    firstRule.matcher should be("match")
    firstRule.expression should be("type")
  }

  it should "parse pact with matching rule" in {

  }

}
