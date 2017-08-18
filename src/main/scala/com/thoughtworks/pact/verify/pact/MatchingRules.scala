package com.thoughtworks.pact.verify.pact

import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

import com.thoughtworks.pact.verify.json.JsonPath
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
    JsonPath.select(body,selection) match {
      case JsDefined(o) => isMatchExpress(o, expectedBody)
      case _ if mx.isDefined && mx.get.typ == "min" && mx.get.value == 0 => None
      case err => Some(err.toString)
    }
  }

  def isMatchExpress(fieldValue: JsValue, expectedBody: JsValue = JsNull): Option[String] = matcherType.toLowerCase match {
    case "type" => isRawTypeMatch(fieldValue)
    case "date" | "timestamp" => isDateFormatMatch(fieldValue)
    case "regex" => isRegexMatch(fieldValue)
    case "match" if expression == "type" => isCustomerTypeMatch(fieldValue, expectedBody)
    case "match" if expression == "integer" => isInteger(fieldValue)
    case "match" => ???
  }

  private def isInteger(actualFieldValue: JsValue) = {
    Try(actualFieldValue.asInstanceOf[JsNumber]).fold[Option[String]](e => Some(e.toString), _ => None)
  }

  private def isRegexMatch(actualFieldValue: JsValue): Option[String] = {
    actualFieldValue.isInstanceOf[JsString] match {
      case true =>
        val pattern = Pattern.compile(expression)
        val actualStr = actualFieldValue.asInstanceOf[JsString].value
        logger.debug(s"expression=[$expression], actualFieldValue=[${actualStr}]")
        Try(pattern.matcher(actualStr)).fold[Option[String]](e => Some(e.getMessage), _ => None)
      case false => None
    }
  }

  private def isDateFormatMatch(actualFieldValue: JsValue): Option[String] = {
    actualFieldValue.isInstanceOf[JsString] match {
      case true =>
        val df = new SimpleDateFormat(expression)
        val actualStr = actualFieldValue.asInstanceOf[JsString].value
        logger.debug(s"expression=[$expression], actualFieldValue=[${actualStr}], expect=[${df.format(new Date())}]")
        Try(df.parse(actualStr)).fold[Option[String]](e => Some(e.getMessage), _ => None)
      case false => None
    }
  }

  private def isCustomerTypeMatch(actualFieldValue: JsValue, expectedBody: JsValue): Option[String] = {
    JsonPath.select(expectedBody,selection) match {
      case JsDefined(expectedFieldExpectedValue) =>
        val res = isCustomerTypeFieldMath(actualFieldValue, expectedFieldExpectedValue)
        logger.debug(s"isCustomerTypeMatch:\nactualFieldValue:${Json.stringify(actualFieldValue)}\n" +
          s"expected:${Json.stringify(expectedFieldExpectedValue)}\n MatchResult$res")
        res
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
      case JsDefined(array) => isCustomerTypeFieldMath(array, (expectedFieldExpectedArray \ 0).getOrElse(JsNull))
      case err => None
    }
  }

  private def isObjectTypeMath(actual: JsValue, expectedFieldExpectedValue: JsValue): Option[String] = {
    val actualObj = actual.asInstanceOf[JsObject]
    val expectedFieldExpectedValueObj = expectedFieldExpectedValue.asInstanceOf[JsObject]
    logger.debug(s"expectedFieldExpectedObj:${Json.stringify(expectedFieldExpectedValueObj)} \n" +
      s"expectedFieldExpectedKeys: [${expectedFieldExpectedValueObj.value.map(_._1).mkString(",")}")

    expectedFieldExpectedValueObj.value.foldLeft[Option[String]](None)((acc, v) => {
      val key = v._1
      val value = v._2
      logger.debug(s"expected key:[$key], actualFieldValue:[${Json.stringify(actual)}], isContains:[${actualObj.value.contains(key)}],acc=[$acc]")
      val res = if (actualObj.value.contains(key)) {
        val actualValue = actualObj.value(key)
        val _res = isCustomerTypeFieldMath(actualValue, value)
        logger.debug(s"matched: ${_res}")
        _res
      } else {
        Some(s"match rule[$this] failed; expected field:[$key] is not exists")
      }
      if (acc.isEmpty) {
        res
      } else {
        acc.map(err => s"$err${res.map(v => s"\n$v").getOrElse("")}")
      }
    })
  }

  def isRawTypeMatch(fieldValue: JsValue): Option[String] = expression.toLowerCase match {
    case "number" => Try(fieldValue.asInstanceOf[JsNumber]).fold[Option[String]](e => Some(e.toString), _ => None)
    case "array" => Try(fieldValue.asInstanceOf[JsArray]).fold[Option[String]](e => Some(e.toString), _ => None)
    case "string" => Try(fieldValue.asInstanceOf[JsString]).fold[Option[String]](e => Some(e.toString), _ => None)
    case "boolean" => Try(fieldValue.asInstanceOf[JsBoolean]).fold[Option[String]](e => Some(e.toString), _ => None)
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