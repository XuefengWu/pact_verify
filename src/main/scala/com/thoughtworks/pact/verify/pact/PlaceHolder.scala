package com.thoughtworks.pact.verify.pact

import play.api.libs.json.{JsLookupResult, JsUndefined, JsValue, Json}

import scala.util.Try

/**
  * Created by xfwu on 12/07/2017.
  */
object PlaceHolder {


  private val PlaceHolderR = """"\$([a-zA-Z\.]+)\$"""".r
  private val PlaceHolderWithoutQuoR = """\$([a-zA-Z\.]+)\$""".r

  def getParameterFormBody(responseBody: JsValue, setParametersOpt: Option[Map[String, String]]): Map[String, JsLookupResult] = {
    setParametersOpt.map(_.map { case (k, r) => (k, JsonPath.select(responseBody, r)) }).getOrElse(Map[String, JsLookupResult]())
  }

  def replacePlaceHolderParameter(request: PactRequest, parametersStack: Map[String, JsLookupResult]): PactRequest = {

    //parameters
    val body = request.body
    var requestBufOpt = body.map(_.toString())
    var url = request.path

    if (request.body.isDefined) {
      PlaceHolderR.findAllMatchIn(request.body.get.toString()).map { m => m.group(1) }.foreach { placeId =>
        val placeJsValueOpt: Option[JsLookupResult] = parametersStack.get(placeId)
        if (placeJsValueOpt.isDefined && !placeJsValueOpt.get.isInstanceOf[JsUndefined]) {
          val placeValue = placeJsValueOpt.get.get.result.get.toString().drop(1).dropRight(1)
          println(placeValue)
          println(requestBufOpt)
          requestBufOpt = requestBufOpt.map(requestBuf => requestBuf.replaceAll("\\$" + placeId + "\\$", placeValue))
          println(requestBufOpt)
        }
      }
    }

    PlaceHolderWithoutQuoR.findAllMatchIn(request.path).map { m => m.group(1) }.foreach { placeId =>
      val placeJsValueOpt = parametersStack.get(placeId)
      if (placeJsValueOpt.isDefined && !placeJsValueOpt.get.isInstanceOf[JsUndefined]) {
        val placeValue = placeJsValueOpt.get.get.result.get.toString().drop(1).dropRight(1)
        url = url.replaceAll("\\$" + placeId + "\\$", placeValue)
      }
    }

    request.copy(path = url, body = requestBufOpt.map(requestBuf => Json.parse(requestBuf)))

  }

}
