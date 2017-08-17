package com.thoughtworks.pact.verify.pact

import java.io.File

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.impl.LogFactoryImpl
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 12/07/2017.
  */
class PactFileSpec extends FlatSpec with Matchers {

  implicit val pactRequestFormat = Json.format[PactRequest]
  implicit val pactResponseFormat = Json.format[PactResponse]
  implicit val interactionFormat = Json.format[Interaction]
  implicit val providerFormat = Json.format[Provider]
  implicit val consumerFormat = Json.format[Consumer]

  implicit val pactFormat = Json.format[Pact]

  private val sss = LogFactory.getFactory.setAttribute(LogFactoryImpl.LOG_PROPERTY, "org.apache.commons.logging.impl.SimpleLog")
  private val ss = System.setProperty("org.apache.commons.logging.simplelog.defaultlog","debug")

  "Pact File" should "parse pact json" in {
    val dir = new File("src/test/resources/pacts/placeholder")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.get.provider.map(_.name).getOrElse("target provider") should be("account Service")
    pact.get.interactions.head.setParameters should be(Some(Map("login.username" -> "$.body.username")))
  }

  it should "parse pact json file with matching" in {
    val dir = new File("src/test/resources/pacts/matching")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.get.provider.map(_.name).getOrElse("target provider") should be("test_provider_array")
    val matchRuleJs = pact.get.interactions.head.response.matchingRules.head
    val matchRule = MatchingRules(matchRuleJs).head
    matchRule.selection should be("$.body[0].id")
  }


  it should "parse pact json file with login before" in {
    val dir = new File("src/test/resources/pacts/before")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.get.interactions.head.description should be("_before_login")
    pact.get.source.get should be("before/test.json")
  }

  it should "parse wrong json file failed" in {
    val dir = new File("src/test/resources/pacts/failed_parse")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.isFailure should be(true)
    pact.failed.get.getSuppressed.toSeq(0).getMessage should include("failed_parse/test.json")
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
    firstRule.matcherType should be("match")
    firstRule.expression should be("type")
  }

  "Matching Rules" should "parse matchingRule from string with min" in {

    val s = """ {
                    "$.body.data.array3[0]": {
                         "max": 5,
                         "match": "type"
                     },
                     "$.body.data.array1": {
                         "min": 0,
                         "match": "type"
                     },
                     "$.body.data.array2": {
                         "min": 1,
                         "match": "type"
                     }
                              }"""

    val json = Json.parse(s)
    val matchingRules = MatchingRules(json)
    matchingRules.size should be(3)
    val firstRule = matchingRules.head
    firstRule.selection should be("$.body.data.array3[0]")
    firstRule.matcherType should be("match")
    firstRule.expression should be("type")
    firstRule.mx should be(Some(Mx("max",5)))
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
