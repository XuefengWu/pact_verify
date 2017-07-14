package com.thoughtworks.verify.pact

import play.api.libs.json._

import scala.util.Try

/**
  * Created by xfwu on 14/07/2017.
  */
object PactResponseJson {

  def hardParseJsValue(jsValue: JsValue): JsValue = {
    //println(s"hard parse: jsValue:$jsValue, class:${jsValue.getClass}")
    jsValue match {
      case js: JsArray => hardParseStringInJsArray(js)
      case js: JsObject => hardParseStringInJsObject(js)
      case js: JsString => hardParseStringAsJsObject(js)
      case _ => jsValue
    }
  }

  private def hardParseStringInJsArray(jsArray: JsArray): JsArray = {
    val value2 = jsArray.value.map(hardParseJsValue)
    JsArray(value2)
  }

  private def hardParseStringInJsObject(jsObject: JsObject): JsObject = {
    //println(s"hardParseString: $jsObject")
    val map: collection.Map[String, JsValue] = jsObject.value
    val map2: collection.Map[String, JsValue] = map.map(v => {
      (v._1, hardParseJsValue(v._2))
    })
    JsObject(map2)
  }

  private def hardParseStringAsJsObject(jsString: JsString): JsValue = {
    //println(s"hardParseStringAsJsObject: [$jsString]")
    val jsValueTry = Try(Json.parse(jsString.value))
    jsValueTry.fold(_ => jsString,
      jsValue => jsValue match {
        case j: JsString => j
        case v => hardParseJsValue(v)
      })
  }

}
