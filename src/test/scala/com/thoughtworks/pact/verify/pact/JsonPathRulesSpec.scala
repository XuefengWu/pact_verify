package com.thoughtworks.pact.verify.pact

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 16/08/2017.
  */
class JsonPathRulesSpec extends FlatSpec with Matchers {
  "Matching Rule Select" should "find field" in {
    val result = JsonPath.select(body,"$.body.data.array1")
    val expected = body.\("data").\("array1")
    result should be(expected)
  }

  it should "select element in array " in {
    val expected = body \ "data" \ "array1" \ 0 \ "id"
    val result = JsonPath.select(body,"$.body.data.array1[0].id")
    result should be(expected)
  }

  it should "select element in array of array" in {
    val expected = body \ "data" \ "array3" \ 0 \ 0 \ "itemCount"
    val result = JsonPath.select(body,"$.body.data.array3[0][0].itemCount")
    result should be(expected)
  }

  it should "select element in array from root array" in {
    val body = Json.parse("""[{"id":123}]""")
    val expected = body \ 0 \ "id"
    val result = JsonPath.select(body,"$.body[0].id")
    result should be(expected)
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
