package com.thoughtworks.pact.verify.pact

import play.api.libs.json.{JsUndefined, JsValue, Json}

import scala.util.Try

/**
  * Created by xfwu on 12/07/2017.
  */
object PlaceHolder {


  private val PlaceHolderR = """"\$([a-zA-Z]+)\$"""".r
  private val PlaceHolderWithoutQuoR = """\$([a-zA-Z]+)\$""".r


   def replacePlaceHolderParameter(request: PactRequest, responseBodyOpt: Option[JsValue]): PactRequest = {

    if (responseBodyOpt.isDefined) {
      val responseBody = responseBodyOpt.get
      //parameters
      val body = request.body
      var requestBufOpt = body.map(_.toString())
      var url = request.path
      if (Try(responseBody).isSuccess) {
        if (request.body.isDefined) {
          PlaceHolderR.findAllMatchIn(request.body.get.toString()).map { m => m.group(1) }.foreach { placeId =>
            val placeJsValue = (responseBody \ placeId)
            if (!placeJsValue.isInstanceOf[JsUndefined]) {
              val placeValue = placeJsValue.toString().drop(1).dropRight(1)
              requestBufOpt = requestBufOpt.map(requestBuf => requestBuf.replaceAll("\\$" + placeId + "\\$", placeValue))
            }
          }
        }

        PlaceHolderWithoutQuoR.findAllMatchIn(request.path).map { m => m.group(1) }.foreach { placeId =>
          val placeJsValue = (responseBody \ placeId)
          if (!placeJsValue.isInstanceOf[JsUndefined]) {
            val placeValue = placeJsValue.toString().drop(1).dropRight(1)
            url = url.replaceAll("\\$" + placeId + "\\$", placeValue)
          }
        }

      }

      request.copy(path = url, body = requestBufOpt.map(requestBuf => Json.parse(requestBuf)))
    } else request
  }

}
