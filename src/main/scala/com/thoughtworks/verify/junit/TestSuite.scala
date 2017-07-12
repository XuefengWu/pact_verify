package com.thoughtworks.verify.junit


/**
  * Created by xfwu on 12/07/2017.
  */
case class TestSuite(disabled: String, errors: Int, failures: Int, hostname: String, id: String,
                     name: String, pkg: String, skipped: String, tests: String, time: String,
                     timestamp: String, cases: Seq[TestCase])
