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

  def assert(request: PactRequest, actual: WSResponse): Option[Failure] = {
    val expect = this.response
    actual match {
      case _ if expect.status != actual.status => Some(Failure(actual.statusText, s"Status: ${expect.status} != ${actual.status} \n${actual.body}\n request url: ${request.path}\n request body: ${request.body.map(_.toString())}"))
      case _ if expect.body.isDefined && !isEqual(expect.body.get, Json.parse(actual.body)) => Some(Failure(actual.statusText, s"期望:${expect.body.get}\n 实际返回:${actual.body}\n request url: ${request.path}\n request body: ${request.body.map(_.toString())}"))
      case _ => None
    }

  }


  private def isEqual(expect: JsValue, actual: JsValue): Boolean = {
    if (expect.isInstanceOf[JsObject] && actual.isInstanceOf[JsObject]) {
      isEqualObject(expect.asInstanceOf[JsObject], actual.asInstanceOf[JsObject])
    } else if (expect.isInstanceOf[JsArray] && actual.isInstanceOf[JsArray]) {
      isEqualArray(expect.asInstanceOf[JsArray], actual.asInstanceOf[JsArray])
    } else {
      expect == actual
    }
  }



  private def isEqualObject(expect: JsObject, actual: JsObject): Boolean = {
    val asserts = expect.asInstanceOf[JsObject].fields.map { case (field, value) =>
      value == actual \ field
    }
    if (!asserts.isEmpty) {
      asserts.reduce(_ && _)
    } else false
  }

  private def isEqualArray(expect: JsArray, actual: JsArray): Boolean = {
    if (expect.value.size == actual.value.size) {
      val actualValues = actual.value
      val asserts = expect.value.zipWithIndex.map { case (v, i) => isEqual(v, actualValues(i)) }
      asserts.reduce(_ && _)
    } else false
  }



}


