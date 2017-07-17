package com.thoughtworks.verify.pact

import scala.util.Try

/**
  * Created by xfwu on 12/07/2017.
  */
case class Pacts(name: String, pacts: Seq[Try[Pact]])
