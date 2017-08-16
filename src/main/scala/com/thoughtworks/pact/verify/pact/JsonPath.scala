package com.thoughtworks.pact.verify.pact

import com.jayway.jsonpath
import net.minidev.json.JSONArray
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.logging.LogFactory
import play.api.libs.json._

import scala.util.{Failure, Success, Try}
/**
  * Created by xfwu on 16/08/2017.
  */
object JsonPath {

  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  def select(body: JsValue,selection: String): JsLookupResult = {

    val bodyStr = Json.stringify(JsObject(Seq(("body", body))))

    val resultTr = Try(jsonpath.JsonPath.parse(bodyStr).read(selection, classOf[Object]))

    val result = resultTr match {
      case Success(v) =>  JsDefined(convertToJsValue(v))
      case Failure(err) => JsUndefined(ExceptionUtils.getStackTrace(err))
    }

    result
  }

  private def convertToJsValue(o: Object):JsValue = {
    o match {
      case v: java.lang.Boolean => JsBoolean(v)
      case v:Number => JsNumber(BigDecimal.valueOf(v.doubleValue()))
      case v: String =>  JsString(v)
      case v: JSONArray =>  Json.parse(v.toJSONString)
      case map: java.util.LinkedHashMap[String,Object] =>
        val sMap = scala.collection.JavaConverters.mapAsScalaMap(map)        
        JsObject(sMap.map{case (k,v) => (k, convertToJsValue(v))})
    }
  }

}
