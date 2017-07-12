package com.thoughtworks.verify.pact

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable.Stack

/**
  * Created by xfwu on 12/07/2017.
  */
class PactFileSpec extends FlatSpec with Matchers {


  "Pact File" should "parse pact json" in {
    val dir = new File("src/test/resources/pacts/account")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.name should be("login Service")
  }

  it should "parse pact json with matching" in {
    val dir = new File("src/test/resources/pacts/matching")
    val pacts = PactFile.loadPacts(dir)
    pacts.size should be(1)
    val pact = pacts.head.pacts.head
    pact.name should be("login Service")
  }

}
