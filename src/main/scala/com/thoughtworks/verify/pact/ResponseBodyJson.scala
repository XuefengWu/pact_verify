package com.thoughtworks.verify.pact

import org.apache.commons.logging.LogFactory
import play.api.libs.json._

import scala.util.{Success, Try}

/**
  * Created by xfwu on 14/07/2017.
  */
object ResponseBodyJson {

  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  def tryHardParseJsValue(body: String): Try[JsValue] = {
    hardParseJsValue(Json.parse(body))
  }

  def hardParseJsValue(jsValue: JsValue): Try[JsValue] = {
    //println(s"hard parse: jsValue:$jsValue, class:${jsValue.getClass}")
    jsValue match {
      case js: JsArray => hardParseStringInJsArray(js)
      case js: JsObject => hardParseStringInJsObject(js)
      case js: JsString => hardParseStringAsJsObject(js)
      case _ => Success(jsValue)
    }
  }


  private def hardParseStringInJsArray(jsArray: JsArray): Try[JsArray] = {
    val value2 = jsArray.value.map(hardParseJsValue)
    Try(JsArray(value2.map(_.get)))
  }

  private def hardParseStringInJsObject(jsObject: JsObject): Try[JsObject] = {
    //logger.trace(s"hardParseString: $jsObject")
    val map: collection.Map[String, JsValue] = jsObject.value
    val map2: collection.Map[String, Try[JsValue]] = map.map(v => {
      (v._1, hardParseJsValue(v._2))
    })
    Try(JsObject(map2.map(v => (v._1,v._2.get))))
  }

  private def hardParseStringAsJsObject(jsString: JsString): Try[JsValue] = {
    logger.trace(s"hardParseStringAsJsObject: [$jsString]")
    val jsValueTry = Try(Json.parse(jsString.value))
    jsValueTry.fold(_ => Success(jsString),
      jsValue => jsValue match {
        case j: JsString => Success(j)
        case v => hardParseJsValue(v)
      })
  }

}
