package com.thoughtworks.verify.pact

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable.Stack

/**
  * Created by xfwu on 12/07/2017.
  */
class PactFileSpec extends FlatSpec with Matchers {

  implicit val pactRequestFormat = Json.format[PactRequest]
  implicit val pactResponseFormat = Json.format[PactResponse]
  implicit val interactionFormat = Json.format[Interaction]
  implicit val pactFormat = Json.format[Pact]

  "Pact File" should "parse pact json" in {
    val dir = new File("src/test/resources/pacts/account")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.name should be("login Service")
  }

  it should "parse pact json file with matching" in {
    val dir = new File("src/test/resources/pacts/matching")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.name should be("test_provider_array")
    val matchRuleJs = pact.interactions.head.response.matchingRules.head
    val matchRule = MatchingRules(matchRuleJs).head
    matchRule.selection should be("$.body[0].id")
  }


  "Matching Rules" should "parse matchingRule from string" in {

    val s = """ {
                                  "$.body[0].id": {
                                      "match": "type"
                                  },
                                  "$.body[1].timestamp": {
                                      "timestamp": "yyyy-MM-dd'T'HH:mm:ss"
                                  },
                                  "$.body[1].id": {
                                      "match": "type"
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

  it should "parse interactions with matching rule" in {

    val s =
      """
        {
                    "description": "java test interaction with a DSL array body",
                    "request": {
                        "method": "GET",
                        "path": "/"
                    },
                    "response": {
                        "status": 200,
                        "headers": {
                            "Content-Type": "application/json; charset=UTF-8"
                        },
                        "body": [
                            {
                                "dob": "07/12/2017",
                                "id": 8480334967,
                                "name": "Rogger the Dogger",
                                "timestamp": "2017-07-12T19:51:56"
                            },
                            {
                                "dob": "07/12/2017",
                                "id": 6885210683,
                                "name": "Cat in the Hat",
                                "timestamp": "2017-07-12T19:51:56"
                            }
                        ],
                        "matchingRules": {
                            "$.body[0].id": {
                                "match": "type"
                            },
                            "$.body[1].timestamp": {
                                "timestamp": "yyyy-MM-dd'T'HH:mm:ss"
                            },
                            "$.body[1].id": {
                                "match": "type"
                            },
                            "$.body[1].dob": {
                                "date": "MM/dd/yyyy"
                            },
                            "$.body[0].timestamp": {
                                "timestamp": "yyyy-MM-dd'T'HH:mm:ss"
                            },
                            "$.body[0].dob": {
                                "date": "MM/dd/yyyy"
                            }
                        }
                    }
                }
      """

    val pact = Json.parse(s).as[Interaction]

  }


}