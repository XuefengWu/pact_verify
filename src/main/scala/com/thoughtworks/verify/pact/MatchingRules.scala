package com.thoughtworks.verify.pact

import play.api.libs.json.JsValue

/**
  * Created by xfwu on 12/07/2017.
  */

case class MatchingRules(selection: String, matcher:String, expression: String)
object MatchingRules {

  val RulePat = """"(.*)"\:\{"(.*)"\:"(.*)"\}.*""".r
  def apply(json: JsValue): Seq[MatchingRules] = {
    RulePat.findAllMatchIn(""""$.body[0].id":{"match":"type"}""")
    val jsValue = json("matchingRules")
    val rules = jsValue.toString().drop(1).dropRight(1).split(",").toSeq
    rules.map(v => {
      v match {
        case RulePat(s,m,e) => new MatchingRules(s, m, e)
      }
      })
  }
}