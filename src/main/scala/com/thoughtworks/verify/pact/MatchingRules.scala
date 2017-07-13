package com.thoughtworks.verify.pact

import play.api.libs.json._

/**
  * Created by xfwu on 12/07/2017.
  */

case class Mx(typ: String, value: Int)

case class MatchingRule(selection: String, matcherType: String, expression: String, mx: Option[Mx]) {

  def isBodyMatch(body: JsValue, expectedBody: JsValue): Boolean = {
    select(body) match {
      case JsDefined(o) => isMatchExpress(o, expectedBody)
      case _ if mx.isDefined && mx.get.typ == "min" && mx.get.value == 0 => true
      case _ => false
    }
  }

  def isMatchExpress(value: JsValue, expectedBody: JsValue = JsNull): Boolean = matcherType.toLowerCase match {
    case "type" => isRawTypeMatch(value)
    case "date" => ???
    case "regex" => ???
    case "match" if expression == "type" => isCustomerTypeMath(value, expectedBody)
    case "match" => ???
  }

  def isCustomerTypeMath(actual: JsValue, expectedBody: JsValue): Boolean = {
    select(expectedBody) match {
      case JsDefined(expectedFieldExpectedValue) => isCustomerTypeFieldMath(actual, expectedFieldExpectedValue)
      case _ => false
    }
  }

  private def isCustomerTypeFieldMath(actualField: JsValue, expectedField: JsValue) = {
    actualField.getClass.eq(expectedField.getClass) match {
      case true if ("play.api.libs.json.JsObject".eq(actualField.getClass.getCanonicalName)) =>
        isObjectTypeMath(actualField, expectedField)
      case true => true
      case false => false
    }
  }

  private def isObjectTypeMath(actual: JsValue, expectedFieldExpectedValue: JsValue): Boolean = {
    val actualObj = actual.asInstanceOf[JsObject]
    val expectedFieldExpectedValueObj = expectedFieldExpectedValue.asInstanceOf[JsObject]
    actualObj.value.foldLeft(true)((acc, v) => {
      if(acc){
        val key = v._1
        val value = v._2
        if(expectedFieldExpectedValueObj.value.contains(key)) {
          val execptedValue = expectedFieldExpectedValueObj.value(key)
          acc && isCustomerTypeFieldMath(value, execptedValue)
        } else {
          false
        }
      }else{
        false
      }
    })
  }

  def isRawTypeMatch(value: JsValue): Boolean = expression.toLowerCase match {
    case "number" => value.isInstanceOf[JsNumber]
    case "array" => value.isInstanceOf[JsArray]
    case "string" => value.isInstanceOf[JsString]
    case "boolean" => value.isInstanceOf[JsBoolean]
  }

  def select(body: JsValue): JsLookupResult = {
    val path = selection.drop(7) //drop [$.body.]
    path.split("\\.").foldLeft[JsLookupResult](JsDefined(body))((acc, v) => {
      acc match {
        case JsDefined(o) => doSelect(o, v)
        case f => f
      }
    })
  }

  private def doSelect(node: JsValue, path: String) = {
    parseFields(path).foldLeft[JsLookupResult](JsDefined(node))((acc, v) => {
      acc match {
        case JsDefined(o) =>
          v match {
            case f: String => acc \ f
            case i: Int => acc \ i
          }
        case f => f
      }
    })
  }

  private def parseFields(path: String) = {
    if (path.contains("[")) {
      val fields = path.split("\\[").toSeq match {
        case Seq(field, first) => Seq(field, first.dropRight(1).toInt)
        case Seq(field, first, second) => Seq(field, first.dropRight(1).toInt, second.dropRight(1).toInt)
      }
      fields
    } else {
      Seq(path)
    }
  }
}

object MatchingRules {

  val RulePat = """"(.*)"\:\{"(.*)"\:"(.*)".*""".r
  val RulePat2 = """"(.*)"\:\{"(.*)"\:([0-9]*),"(.*)"\:"(.*)".*""".r

  def apply(jsValue: JsValue): Seq[MatchingRule] = {
    val str = jsValue.toString()
    //println(str)
    val rules = str.drop(1).dropRight(2).split("\\},").toSeq
    rules.map(v => {
      v match {
        case RulePat2(s, mt, mv, m, e) => new MatchingRule(s, m, e, Some(Mx(mt, mv.toInt)))
        case RulePat(s, m, e) => new MatchingRule(s, m, e, None)
      }
    })
  }
}