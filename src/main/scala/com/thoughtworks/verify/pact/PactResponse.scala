package com.thoughtworks.verify.pact

import play.api.libs.json.{JsArray, JsObject, JsValue}

/**
  * Created by xfwu on 12/07/2017.
  */
case class PactResponse(status: Int, body: Option[JsValue], matchingRules: Option[JsValue]) {

  def getBody() = body.map(ResponseBodyJson.hardParseJsValue)

  def isMatch(actual: JsValue): Option[String] = {

    isEqual(getBody().get.get, actual) match {
      case true => None
      case false => matchFields(actual)
    }
  }

  def matchFields(actual: JsValue): Option[String] = {
    val expect: JsValue = getBody().get.get
    matchingRules match {
      case Some(r) =>
        val rules = MatchingRules(r)
        rules.foldLeft[Option[String]](None)((acc, matcher) =>
          matcher.isBodyMatch(actual, expect) match {
            case Some(err2) => acc.map(err => s"$err \n $err2")
            case None => acc
          })
      case None => Some(s"no matching rule for body:\nexpect:${expect.toString()}\n${actual.toString()}")
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
