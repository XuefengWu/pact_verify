package com.thoughtworks.verify.pact

import com.thoughtworks.verify.junit.Failure
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

/**
  * Created by xfwu on 12/07/2017.
  */
case class Interaction(description: String,
                       request: PactRequest,
                       response: PactResponse) {

  def assert(request: PactRequest, actual: WSResponse): Option[Failure] = {
    val expect = this.response
    actual match {
      case _ if expect.status != actual.status =>
        Some(Failure(actual.statusText, generateStatuesFailureMessage(request, actual, expect)))
      case _ if expect.body.isDefined && !expect.isMatch(Json.parse(actual.body)) =>
        Some(Failure(actual.statusText, generateBodyFailureMessage(request, actual, expect)))
      case _ => None
    }

  }

  private def generateBodyFailureMessage(request: PactRequest, actual: WSResponse, expect: PactResponse) = {
    s"期望:${expect.body.get}\n 实际返回:${actual.body}\n " +
      s"request url: ${request.path}\n request body: ${request.body.map(_.toString())}"
  }

  private def generateStatuesFailureMessage(request: PactRequest, actual: WSResponse, expect: PactResponse) = {
    s"Status: ${expect.status} != ${actual.status} \n${actual.body}\n " +
      s"request url: ${request.path}\n request body: ${request.body.map(_.toString())}"
  }



}


