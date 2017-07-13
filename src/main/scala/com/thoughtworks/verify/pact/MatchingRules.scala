package com.thoughtworks.verify.pact

import play.api.libs.json._

/**
  * Created by xfwu on 12/07/2017.
  */

case class Mx(typ: String, value: Int)

case class MatchingRule(selection: String, matcherType: String, expression: String, mx: Option[Mx]) {

  def isBodyMatch(body: JsValue): Boolean = {
    select(body) match {
      case JsDefined(o) => isMatchExpress(o)
      case _ if mx.isDefined && mx.get.typ == "min" && mx.get.value == 0 => true
      case _ => false
    }
  }

  def isMatchExpress(value: JsValue): Boolean = matcherType.toLowerCase match {
    case "type" => isTypeMatch(value)
    case "date" => ???
    case "regex" => ???
    case "match" => ???
  }

  def isTypeMatch(value: JsValue): Boolean = expression.toLowerCase match {
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