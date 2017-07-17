package com.thoughtworks.verify.pact

import com.thoughtworks.verify.junit.Failure

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
            case Some(err) => Some(Failure(actual.statusText, generateBodyFailureMessage(err,request, actual, expect)))
            case None => None
          }
          case scala.util.Failure(f) => Some(Failure(actual.statusText,
                        generateBodyFailureMessage(f.getStackTrace.mkString("/n"),request, actual, expect)))
        }
      case _ => None
    }
  }

  private def generateBodyFailureMessage(err:String,request: PactRequest, actual: HttpResponse, expect: PactResponse) = {
    s"request url: ${request.path}\n 错误:$err \n 期望:${expect.getBody().get}\n 实际返回:${actual.body}\n " +
      s"request body: ${request.body.map(_.toString())}"
  }

  private def generateStatuesFailureMessage(request: PactRequest, actual: HttpResponse, expect: PactResponse) = {
    s"request url: ${request.path}\n Status: ${expect.status} != ${actual.status} \n${actual.body}\n " +
      s"request body: ${request.body.map(_.toString())}"
  }



}


