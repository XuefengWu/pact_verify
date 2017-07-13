package com.thoughtworks.verify.pact

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class PactWSSpec extends FlatSpec with Matchers  {


  /*"WS Client" should "visit web site " in {
    val ws = new PactWSImpl("https://passport.baidu.com")
    val resultF = ws.ws.url("https://passport.baidu.com/v2/sapi/authwidgetverify")
    val statusF: Future[Int] = resultF.get().map(_.status)
    val status = Await.result(statusF, Duration(30, SECONDS))
    status should be (200)
  }*/

}
