package com.thoughtworks.verify.pact

import play.api.libs.json.JsValue

/**
  * Created by xfwu on 12/07/2017.
  */

case class Mx(typ: String,value: Int)
case class MatchingRule(selection: String, matcher:String, expression: String,mx: Option[Mx]) {

  def isBodyMatch(body: JsValue):Boolean = true

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
        case RulePat2(s,mt,mv,m,e) => new MatchingRule(s, m, e,Some(Mx(mt,mv.toInt)))
        case RulePat(s,m,e) => new MatchingRule(s, m, e,None)
      }
      })
  }
}