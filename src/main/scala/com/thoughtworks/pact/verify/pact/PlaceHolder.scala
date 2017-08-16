package com.thoughtworks.pact.verify.pact

import org.apache.commons.logging.LogFactory
import play.api.libs.json._

/**
  * Created by xfwu on 12/07/2017.
  */
object PlaceHolder {

  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  private val PlaceHolderR = """"\$([a-zA-Z\.]+)\$"""".r
  private val PlaceHolderWithoutQuoR = """\$([a-zA-Z\.]+)\$""".r

  def getParameterFormBody(responseBody: JsValue, setParametersOpt: Option[Map[String, String]],
                           parameterStack: Map[String, JsLookupResult]): Map[String, JsLookupResult] = {

    setParametersOpt.map(_.foldLeft(parameterStack){(acc,v) => {
     v match {
       case (k, r) if(r.startsWith("$.body")) =>
         acc + ((k, JsonPath.select(responseBody, r)))
       case (k, r) if(!r.startsWith("$.body")) =>
         acc + ((k, JsDefined(calcParameter(r,acc))))
     }
    }}).getOrElse(Map[String, JsLookupResult]())

  }

  private def calcParameter(rawEval:String, parameterStack: Map[String, JsLookupResult]): JsValue = {
    val eval = relacePlaceHolder(rawEval, parameterStack)
    logger.debug(s"rawEval: $rawEval, eval: $eval, parameterStack:$parameterStack")
    val res:Object = ""

    res match {
      case v: Number => JsNumber(BigDecimal.valueOf(v.doubleValue()))
      case v: String => JsString(v)
    }
  }


  private def relacePlaceHolder(raw: String, parametersStack: Map[String, JsLookupResult]): String = {
    var temp = raw
    PlaceHolderWithoutQuoR.findAllMatchIn(raw).map { m => m.group(1) }.foreach { placeId =>
      val placeJsValueOpt = parametersStack.get(placeId)
      if (placeJsValueOpt.isDefined && !placeJsValueOpt.get.isInstanceOf[JsUndefined]) {
        val placeValue = placeJsValueOpt.get.get match {
          case JsString(s) => s
          case v: JsValue => Json.stringify(v)
        }
        temp = temp.replaceAll("\\$" + placeId + "\\$", placeValue)
      }
    }
    temp
  }

  def replacePlaceHolderParameter(request: PactRequest, parametersStack: Map[String, JsLookupResult]): PactRequest = {

    //parameters
    val body = request.body
    var requestBufOpt = body.map(_.toString())

    if (request.body.isDefined) {
      PlaceHolderR.findAllMatchIn(request.body.get.toString()).map { m => m.group(1) }.foreach { placeId =>
        val placeJsValueOpt: Option[JsLookupResult] = parametersStack.get(placeId)
        if (placeJsValueOpt.isDefined && !placeJsValueOpt.get.isInstanceOf[JsUndefined]) {
          val placeValue = placeJsValueOpt.get.get.result.get.toString().drop(1).dropRight(1)
          logger.debug(placeValue)
          logger.trace(requestBufOpt)
          requestBufOpt = requestBufOpt.map(requestBuf => requestBuf.replaceAll("\\$" + placeId + "\\$", placeValue))
          logger.trace(requestBufOpt)
        }
      }
    }

    val url = relacePlaceHolder(request.path, parametersStack)
    request.copy(path = url, body = requestBufOpt.map(requestBuf => Json.parse(requestBuf)))

  }

}
