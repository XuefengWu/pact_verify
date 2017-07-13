package com.thoughtworks.verify.pact

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 13/07/2017.
  */
class PactResponseSpec  extends FlatSpec with Matchers {

  "Pact Response " should "match fields" in {
    val expectStr =
      """
        [
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
                        ]
      """.stripMargin
    val matchingRulesStr = """{
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
                                             """

    val actualStr = """
                      [
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
                                      ]
                    """

    checkPactResponse(actualStr,expectStr, matchingRulesStr) should be(None)

  }

  private def  checkPactResponse(actualStr: String, expectStr: String,matchingRulesStr:String) = {
    val expect = Json.parse(expectStr)
    val matchingRules = Json.parse(matchingRulesStr)
    val actual = Json.parse(actualStr)

    val response = PactResponse(200,Some(expect),Some(matchingRules))
    response.matchFields(actual)
  }

}
