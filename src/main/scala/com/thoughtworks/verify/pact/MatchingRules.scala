package com.thoughtworks.verify.pact

import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

import org.apache.commons.logging.LogFactory
import play.api.libs.json._

import scala.util.Try

/**
  * Created by xfwu on 12/07/2017.
  */

case class Mx(typ: String, value: Int)

case class MatchingRule(selection: String, matcherType: String, expression: String, mx: Option[Mx]) {

  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  def isBodyMatch(body: JsValue, expectedBody: JsValue): Option[String] = {
    select(body) match {
      case JsDefined(o) => isMatchExpress(o, expectedBody)
      case _ if mx.isDefined && mx.get.typ == "min" && mx.get.value == 0 => None
      case err => Some(err.toString)
    }
  }

  def isMatchExpress(value: JsValue, expectedBody: JsValue = JsNull): Option[String] = matcherType.toLowerCase match {
    case "type" => isRawTypeMatch(value)
    case "date" | "timestamp" => isDateFormatMatch(value)
    case "regex" => isRegexMatch(value)
    case "match" if expression == "type" => isCustomerTypeMatch(value, expectedBody)
    case "match" if expression == "integer" => isInteger(value)
    case "match" => ???
  }

  private def isInteger(value: JsValue) = {
    Try(value.asInstanceOf[JsNumber]).fold[Option[String]](e => Some(e.toString), _ => None)
  }

  private def isRegexMatch(actual: JsValue): Option[String] = {
    actual.isInstanceOf[JsString] match {
      case true =>
        val pattern = Pattern.compile(expression)
        val actualStr = actual.asInstanceOf[JsString].value
        logger.debug(s"expression=[$expression], actual=[${actualStr}]")
        Try(pattern.matcher(actualStr)).fold[Option[String]](e => Some(e.getMessage), _ => None)
      case false => None
    }
  }

  private def isDateFormatMatch(actual: JsValue): Option[String] = {
    actual.isInstanceOf[JsString] match {
      case true =>
        val df = new SimpleDateFormat(expression)
        val actualStr = actual.asInstanceOf[JsString].value
        logger.debug(s"expression=[$expression], actual=[${actualStr}], expect=[${df.format(new Date())}]")
        Try(df.parse(actualStr)).fold[Option[String]](e => Some(e.getMessage), _ => None)
      case false => None
    }
  }

  private def isCustomerTypeMatch(actual: JsValue, expectedBody: JsValue): Option[String] = {
    select(expectedBody) match {
      case JsDefined(expectedFieldExpectedValue) => isCustomerTypeFieldMath(actual, expectedFieldExpectedValue)
      case _ => None
    }
  }

  private def isCustomerTypeFieldMath(actualField: JsValue, expectedField: JsValue): Option[String] = {
    actualField.getClass.eq(expectedField.getClass) match {
      case true if ("play.api.libs.json.JsObject".eq(actualField.getClass.getCanonicalName)) =>
        isObjectTypeMath(actualField, expectedField)
      case true if ("play.api.libs.json.JsArray".eq(actualField.getClass.getCanonicalName)) =>
        isArrayTypeMath(actualField, expectedField)
      case true => None
      case false => Some(s"${actualField.toString()} is not ${expectedField.getClass}")
    }
  }

  private def isArrayTypeMath(actual: JsValue, expectedFieldExpectedValue: JsValue): Option[String] = {
    val actualArray = actual.asInstanceOf[JsArray]
    val expectedFieldExpectedArray = expectedFieldExpectedValue.asInstanceOf[JsArray]
    actualArray \ 0 match {
      case JsDefined(array) => isCustomerTypeFieldMath(array, (expectedFieldExpectedArray \ 0).get)
      case err => None
    }
  }

  private def isObjectTypeMath(actual: JsValue, expectedFieldExpectedValue: JsValue): Option[String] = {
    val actualObj = actual.asInstanceOf[JsObject]
    val expectedFieldExpectedValueObj = expectedFieldExpectedValue.asInstanceOf[JsObject]
    expectedFieldExpectedValueObj.value.foldLeft[Option[String]](None)((acc, v) => {
      if (acc.isEmpty) {
        val key = v._1
        val value = v._2
        logger.debug(s"expected key:[$key], value:[$actual], isContains:[${actualObj.value.contains(key)}],acc=[$acc]")
        if (actualObj.value.contains(key)) {
          val actualValue = actualObj.value(key)
          val res = isCustomerTypeFieldMath(actualValue, value)
          logger.debug(s"matched: $res")
          res
        } else {
          Some(s"match rule[$this] failed; expected field:[$key] is not exists")
        }
      } else {
        acc
      }
    })
  }

  def isRawTypeMatch(value: JsValue): Option[String] = expression.toLowerCase match {
    case "number" => Try(value.asInstanceOf[JsNumber]).fold[Option[String]](e => Some(e.toString), _ => None)
    case "array" => Try(value.asInstanceOf[JsArray]).fold[Option[String]](e => Some(e.toString), _ => None)
    case "string" => Try(value.asInstanceOf[JsString]).fold[Option[String]](e => Some(e.toString), _ => None)
    case "boolean" => Try(value.asInstanceOf[JsBoolean]).fold[Option[String]](e => Some(e.toString), _ => None)
  }

  def select(body: JsValue): JsLookupResult = {
    val path = selection.drop(6).dropWhile(_ == '.') //drop [$.body.]
    logger.debug(s"select path=[$path]")
    path.split("\\.").foldLeft[JsLookupResult](JsDefined(body))((acc, v) => {
      acc match {
        case JsDefined(o) => doSelect(o, v)
        case f => f
      }
    })
  }

  private def doSelect(node: JsValue, path: String): JsLookupResult = {
    logger.debug(s"doSelect path=[$path]")
    val res = parseFields(path).foldLeft[JsLookupResult](JsDefined(node))((acc, v) => {
      acc match {
        case JsDefined(o) =>
          v match {
            case f: String if f.isEmpty => acc
            case f: String => acc \ f
            case i: Int => acc \ i
          }
        case f => f
      }
    })
    logger.debug(s"doSelect result=[$res]")
    res
  }

  private def parseFields(path: String) = {
    if (path.contains("[")) {
      val fields = path.split("\\[").toSeq match {
        case Seq(field, first) => Seq(field, first.dropRight(1).toInt)
        case Seq(field, first, second) => Seq(field, first.dropRight(1).toInt, second.dropRight(1).toInt)
      }
      fields.filterNot({
        case v: String => v.isEmpty
        case _ => false
      })
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
    val rules = str.drop(1).dropRight(2).split("\\},").toSeq
    rules.map(v => {
      v match {
        case RulePat2(s, mt, mv, m, e) => new MatchingRule(s, m, e, Some(Mx(mt, mv.toInt)))
        case RulePat(s, m, e) => new MatchingRule(s, m, e, None)
      }
    })
  }
}