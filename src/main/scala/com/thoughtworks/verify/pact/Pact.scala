package com.thoughtworks.verify.pact

/**
  * Created by xfwu on 12/07/2017.
  */
case class Provider(name:String)
case class Consumer(name:String)
case class Pact(provider: Option[Provider],consumer:Option[Consumer], repeat: Option[Int],
                cookies: Option[String], interactions: Seq[Interaction], source:Option[String] = None)
