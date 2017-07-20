package com.thoughtworks.pact.verify.pact

import com.thoughtworks.pact.verify.junit.Failure
import play.api.libs.json.{JsValue, Json}

import scala.util.Success

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
        ResponseBodyJson.tryHardParseJsValue(actual.body) match {
          case Success(jsValue) => expect.isMatch(jsValue) match {
            case Some(err) => Some(Failure(actual.statusText, generateBodyMatchFailureMessage(err,request, jsValue, expect)))
            case None => None
          }
          case scala.util.Failure(f) => Some(Failure(actual.statusText,
            generateBodyParseFailureMessage(f.getStackTrace.mkString("/n"),request, actual)))
        }
      case _ => None
    }
  }

  private def generateBodyParseFailureMessage(err:String, request: PactRequest, actual: HttpResponse) = {
    s"request url: ${request.path}\n Parse failure:$err \n actual:${actual.body}\n "
  }

  private def generateBodyMatchFailureMessage(err:String, request: PactRequest, actual: JsValue, expect: PactResponse) = {
    s"request url: ${request.path}\n Match failure:$err \n expect:${expect.getBody().get.map(Json.stringify).getOrElse("")}\n " +
      s"actual:${Json.stringify(actual)}\n " +
      s"request body: ${request.body.map(_.toString())}"
  }

  private def generateStatuesFailureMessage(request: PactRequest, actual: HttpResponse, expect: PactResponse) = {
    s"request url: ${request.path}\n Status Do not match: ${expect.status} != ${actual.status}"
  }



}


