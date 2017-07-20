package com.thoughtworks.pact.verify.junit

/**
  * Created by xfwu on 12/07/2017.
  */
case class TestSuites(disabled: String, errors: Int, failures: Int, name: String, tests: String, time: String, testSuites: Seq[TestSuite])
