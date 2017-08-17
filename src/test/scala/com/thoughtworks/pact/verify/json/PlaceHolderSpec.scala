package com.thoughtworks.pact.verify.json

import com.thoughtworks.pact.verify.pact.PactRequest
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._
/**
  * Created by xfwu on 16/08/2017.
  */
class PlaceHolderSpec extends FlatSpec with Matchers {

  "Place Holder" should "extract parameter from body by setParameter rule" in {

    val body = """ {
                      "loginToken": "xxxyyy",
                      "username": "admin"
                    } """
    val setParameters = Map("login.token" -> "$.body.loginToken","login.username" -> "$.body.username")
    val parameters = PlaceHolder.getParameterFormBody(Json.parse(body),Some(setParameters),Map[String, JsLookupResult]())
    parameters should be(Map("login.token" -> JsDefined(JsString("xxxyyy")),"login.username" -> JsDefined(JsString("admin"))))

  }

  it should "calculate number parameter from body by setParameter rule" in {

    val body = """ {
                      "a": 1,
                      "b": 2
                    } """
    val setParameters = Map("a" -> "$.body.a","b" -> "$.body.b","c" -> "$a$+$b$")
    val parameters = PlaceHolder.getParameterFormBody(Json.parse(body),Some(setParameters),Map[String, JsLookupResult]())
    parameters should be(Map("a" -> JsDefined(JsNumber(1)),"b" -> JsDefined(JsNumber(2)),"c" -> JsDefined(JsNumber(3))))

  }

  it should "calculate String parameter from body by setParameter rule" in {

    val body = """ {
                      "a": "hello",
                      "b": "world"
                    } """
    val setParameters = Map("a" -> "$.body.a","b" -> "$.body.b","c" -> "$a$ + $b$")
    val parameters = PlaceHolder.getParameterFormBody(Json.parse(body),Some(setParameters),Map[String, JsLookupResult]())
    parameters should be(Map("a" -> JsDefined(JsString("hello")),"b" -> JsDefined(JsString("world")),"c" -> JsDefined(JsString("hello  world"))))

  }

  it should "replace parameter by setParameterStack success" in {
    val parameterStack = Map("login.token" -> JsDefined(JsString("xxxyyy")),"login.username" -> JsDefined(JsString("admin")))
    val body = Json.parse("""{"loginToken":"$login.token$"}""")
    val request: PactRequest = PactRequest("post","/user/$login.username$",None,Some(body),None,None,None)
    val replacedRequest = PlaceHolder.replacePlaceHolderParameter(request,parameterStack)
    val replacedBody = """{"loginToken":"xxxyyy"}"""
    replacedRequest should be(PactRequest("post","/user/admin",None,Some(Json.parse(replacedBody)),None,None,None))
  }

}
