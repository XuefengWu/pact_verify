package com.thoughtworks.pact.verify.pact

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.impl.LogFactoryImpl
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

/**
  * Created by xfwu on 12/07/2017.
  */
class MatchingRulesSpec extends FlatSpec with Matchers {

  val s  = LogFactory.getFactory.setAttribute(LogFactoryImpl.LOG_PROPERTY, "org.apache.commons.logging.impl.SimpleLog")
  val b = System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "trace")

  "Matching Rule Match Raw Type" should " with type array" in {
    val rule = MatchingRule("$.body.data.array1[0].id", "type", "array", None)
    val value = """[{"dob":"2017-07-12","id":613313905,"name":"ehGKdDIADDeeWpnNiZru"}]"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(None)
  }

  "Matching Rule Match Customer type" should "with  number" in {
    val value = """{"dob":"2017-07-12","id":613313905,"name":"ehGKdDIADDeeWpnNiZru","school":{"name":"TWU","address":"SH"}} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.id", "match", "type", None)
    rule1.isMatchExpress(JsonPath.select(jsValue,rule1.selection).get, jsValue) should be(None)
    val rule2 = MatchingRule("$.body.name", "match", "type", None)
    rule2.isMatchExpress(JsonPath.select(jsValue,rule2.selection).get, jsValue) should be(None)
    val rule3 = MatchingRule("$.body.dob", "match", "type", None)
    rule3.isMatchExpress(JsonPath.select(jsValue,rule3.selection).get, jsValue) should be(None)
    val rule4 = MatchingRule("$.body.school", "match", "type", None)
    rule4.isMatchExpress(JsonPath.select(jsValue,rule4.selection).get, jsValue) should be(None)
  }

  it should "match customer type in array" in {
    val expected = """{"numbers":[{"a":1,"b":2,"c":3},{"a":4,"b":5,"c":6}]} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.numbers", "match", "type", None)
    rule1.isMatchExpress(JsonPath.select(expectedJsValue,rule1.selection).get, expectedJsValue) should be(None)

    val actual1 = Json.parse("""{"numbers":[{"a":1,"b":2,"c":3}]} """)
    rule1.isMatchExpress(JsonPath.select(actual1,rule1.selection).get, expectedJsValue) should be(None)

    val actual2 = Json.parse("""{"numbers":[{"a":1,"b":2,"c":3,"d":4}]} """)
    rule1.isMatchExpress(JsonPath.select(actual2,rule1.selection).get, expectedJsValue) should be(None)

  }

  it should "match customer type whole in object in object" in {
    val expected = """{"school":{"class":{"a":5,"b":6}}} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body", "match", "type", None)
    rule1.isMatchExpress(JsonPath.select(expectedJsValue,rule1.selection).get, expectedJsValue) should be(None)
  }

  it should "not match customer type whole in object in object" in {
    val expected = """{"school":{"class":{"a":5,"b":6}}} """
    val expectedJsValue = Json.parse(expected)
    val acutal = """{"school":{"class":{"a":5}}} """
    val acutalJsValue = Json.parse(acutal)
    val rule1 = MatchingRule("$.body", "match", "type", None)
    val errorMsg = "match rule[MatchingRule($.body,match,type,None)] failed; expected field:[b] is not exists"
    rule1.isMatchExpress(JsonPath.select(acutalJsValue,rule1.selection).get, expectedJsValue) should be(Some(errorMsg))
  }

  it should "not match customer type when more inner field in body" in {
    val expected = """{"numbers":[{"a":1,"b":2,"c":3},{"a":4,"b":5,"c":6}]} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.numbers", "match", "type", None)
    val actual3 = Json.parse("""{"numbers":[{"a":9,"b":2,"d":4}]} """)
    val expectedErr = "match rule[MatchingRule($.body.numbers,match,type,None)] failed; expected field:[c] is not exists"
    rule1.isMatchExpress(JsonPath.select(actual3,rule1.selection).get, expectedJsValue) should be(Some(expectedErr))
  }

  it should "not match customer type when more outter field in body" in {
    val expected = """{"a":1,"b":2,"c":3}"""
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body", "match", "type", None)
    val actual3 = Json.parse("""{"a":9,"b":2,"d":4} """)
    val expectedErr = "match rule[MatchingRule($.body,match,type,None)] failed; expected field:[c] is not exists"
    rule1.isMatchExpress(JsonPath.select(actual3,rule1.selection).get, expectedJsValue) should be(Some(expectedErr))
  }

  it should "not match customer type when less field in object" in {
    val expected = """{"numbers":{"a":4,"b":5,"c":6}} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.numbers", "match", "type", None)
    val actual3 = Json.parse("""{"numbers":{"a":9,"b":2,"d":4}} """)
    val expectedErr = "match rule[MatchingRule($.body.numbers,match,type,None)] failed; expected field:[c] is not exists"
    rule1.isMatchExpress(JsonPath.select(actual3,rule1.selection).get, expectedJsValue) should be(Some(expectedErr))
  }

  it should "not match customer type when less tow field in object" in {
    val expected = """{"numbers":{"a":4,"b":5,"c":6}} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.numbers", "match", "type", None)
    val actual3 = Json.parse("""{"numbers":{"a":9,"d":4}} """)
    val expectedErr = "match rule[MatchingRule($.body.numbers,match,type,None)] failed; expected field:[b] is not exists\n"+
        "match rule[MatchingRule($.body.numbers,match,type,None)] failed; expected field:[c] is not exists"
    rule1.isMatchExpress(JsonPath.select(actual3,rule1.selection).get, expectedJsValue) should be(Some(expectedErr))
  }

  it should "not match customer type when less field in object in object" in {
    val expected = """{"school":{"class":{"a":5,"b":6}}} """
    val expectedJsValue = Json.parse(expected)
    val rule1 = MatchingRule("$.body.school", "match", "type", None)
    val actual3 = Json.parse("""{"school":{"class":{"b":5,"c":6}}} """)
    val expectedErr = Some("match rule[MatchingRule($.body.school,match,type,None)] failed; expected field:[a] is not exists")
    rule1.isMatchExpress(JsonPath.select(actual3,rule1.selection).get, expectedJsValue) should be(expectedErr)
  }

  it should "match value with type number" in {
    val rule = MatchingRule("xxx", "type", "number", None)
    val value = """613313905"""
    val jsValue = Json.parse(value)
    rule.isMatchExpress(jsValue) should be(None)
  }


  "Matching Rule Match date format" should "success with  express" in {
    val value = """{"dob":"2017-07-12","id":613313905} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.dob", "date", "yyyy-MM-dd", None)
    rule1.isMatchExpress(JsonPath.select(jsValue,rule1.selection).get, jsValue) should be(None)
  }

  "Matching Rule Match date timestamp" should "success with  express" in {
    val value = """{"time":"2017-07-12T19:51:56","id":613313905} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.time", "timestamp", "yyyy-MM-dd'T'HH:mm:ss", None)
    rule1.isMatchExpress(JsonPath.select(jsValue,rule1.selection).get, jsValue) should be(None)
  }


  "Matching Rule Match regex" should "success with  express" in {
    val value = """{"dob":"2017-07-12","id":613313905,"ip":"127.0.0.1","school":{"name":"TWU","address":"SH"}} """
    val jsValue = Json.parse(value)
    val rule1 = MatchingRule("$.body.ip", "regex", "(\\d{1,3}\\.)+\\d{1,3}", None)
    rule1.isMatchExpress(JsonPath.select(jsValue,rule1.selection).get, jsValue) should be(None)
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
