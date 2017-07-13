package com.thoughtworks.verify.pact

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 12/07/2017.
  */
class MatchingRulesSpec extends FlatSpec with Matchers {

  "Matching Rule Select" should "find field" in {
      val rule = MatchingRule("$.body.data.array1","type","array",None)
      val result = rule.select(body)
      result.isDefined should be(true)
      val expected = body.\("data").\("array1")
      result should be(expected)
  }

  it should "select element in array " in {
    val rule = MatchingRule("$.body.data.array1[0].id","match","integer",None)
    val expected = body \ "data" \ "array1" \ 0 \ "id"
    val result = rule.select(body)
    result should be(expected)
  }

  it should "select element in array of array" in {
    val rule = MatchingRule("$.body.data.array3[0][0].itemCount","match","integer",None)
    val expected = body \ "data" \ "array3" \ 0 \ 0 \ "itemCount"
    val result = rule.select(body)
    result should be(expected)
  }

  "Matching Rule Match" should " with type array" in {
    val rule = MatchingRule("xxx","type","array",None)
    val value = """[{"dob":"2017-07-12","id":613313905,"name":"ehGKdDIADDeeWpnNiZru"}]"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(true)
  }

  it should "match value with type number" in {
    val rule = MatchingRule("xxx","type","number",None)
    val value = """613313905"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(true)
  }

  private val body = {
    val bodyStr =
      """
          {
             "data": {
                 "array1": [
                     {
                         "dob": "2017-07-12",
                         "id": 613313905,
                         "name": "ehGKdDIADDeeWpnNiZru"
                     }
                 ],
                 "array2": [
                     {
                         "address": "127.0.0.1",
                         "name": "AwpSKbcrQCSxKFKBcieW"
                     }
                 ],
                 "array3": [
                     [
                         {
                             "itemCount": 342721542
                         }
                     ]
                 ]
             },
             "id": 5177628645
         }
        """
    Json.parse(bodyStr)
  }
}
