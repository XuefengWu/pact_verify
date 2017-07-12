package com.thoughtworks.verify.pact

/**
  * Created by xfwu on 12/07/2017.
  */
case class Pact(name: String, repeat: Option[Int], cookies: Option[String], interactions: Seq[Interaction])
