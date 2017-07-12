package com.thoughtworks.verify.pact

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsValue, Json}

/**
  * Created by xfwu on 12/07/2017.
  */
class MatchingRulesSpec extends FlatSpec with Matchers {

  "Matching Rule" should "match field with type" in {
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
      val body = Json.parse(bodyStr)
      val rule = MatchingRule("$.body.data.array1","type","array",None)
      val result = rule.select(body)
      result.isDefined should be(true)
      val value = result.get
      val expected = body.\("data").\("array1").get
      println(value)
      value should be(expected)
  }

  it should "match value with type" in {
    val rule = MatchingRule("$.body.data.array1","type","array",None)
    val value = """[{"dob":"2017-07-12","id":613313905,"name":"ehGKdDIADDeeWpnNiZru"}]"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(true)
  }

}
