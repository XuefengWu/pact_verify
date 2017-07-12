package com.thoughtworks.verify.pact

import play.api.libs.json.JsValue

/**
  * Created by xfwu on 12/07/2017.
  */

case class MatchingRule(selection: String, matcher:String, expression: String)
object MatchingRules {

  val RulePat = """"(.*)"\:\{"(.*)"\:"(.*)"\}.*""".r
  def apply(jsValue: JsValue): Seq[MatchingRule] = {
    val rules = jsValue.toString().drop(1).dropRight(1).split(",").toSeq
    rules.map(v => {
      v match {
        case RulePat(s,m,e) => new MatchingRule(s, m, e)
      }
      })
  }
}