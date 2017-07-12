package com.thoughtworks.verify.junit

/**
  * Created by xfwu on 12/07/2017.
  */
case class TestCase(assertions: String, className: String, name: String, status: String, time: String,
                    error: Option[Error], failure: Option[Failure])
