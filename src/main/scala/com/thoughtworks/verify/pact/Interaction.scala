package com.thoughtworks.verify.pact

import com.thoughtworks.verify.junit.Failure
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse

/**
  * Created by xfwu on 12/07/2017.
  */
case class Interaction(description: String,
                       request: PactRequest,
                       response: PactResponse) {

  def assert(request: PactRequest, actual: HttpResponse): Option[Failure] = {
    val expect = this.response
    actual match {
      case _ if expect.status != actual.status =>
        Some(Failure(actual.statusText, generateStatuesFailureMessage(request, actual, expect)))
      case _ if expect.getBody().isDefined  =>
        expect.isMatch(ResponseBodyJson.hardParseJsValue(Json.parse(actual.body))) match {
          case Some(err) => Some(Failure(actual.statusText, generateBodyFailureMessage(err,request, actual, expect)))
          case None => None
        }
      case _ => None
    }
  }

  private def generateBodyFailureMessage(err:String,request: PactRequest, actual: HttpResponse, expect: PactResponse) = {
    s"错误:$err \n 期望:${expect.getBody().get}\n 实际返回:${actual.body}\n " +
      s"request url: ${request.path}\n request body: ${request.body.map(_.toString())}"
  }

  private def generateStatuesFailureMessage(request: PactRequest, actual: HttpResponse, expect: PactResponse) = {
    s"Status: ${expect.status} != ${actual.status} \n${actual.body}\n " +
      s"request url: ${request.path}\n request body: ${request.body.map(_.toString())}"
  }



}


