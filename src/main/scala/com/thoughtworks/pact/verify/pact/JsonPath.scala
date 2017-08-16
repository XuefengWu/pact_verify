package com.thoughtworks.pact.verify.pact

import org.apache.commons.logging.LogFactory
import play.api.libs.json.{JsDefined, JsLookupResult, JsValue}

/**
  * Created by xfwu on 16/08/2017.
  */
object JsonPath {

  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  def select(body: JsValue,selection: String): JsLookupResult = {
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
