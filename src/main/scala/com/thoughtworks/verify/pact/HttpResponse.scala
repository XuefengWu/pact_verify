package com.thoughtworks.verify.pact

/**
  * Created by xfwu on 17/07/2017.
  */
case class HttpCookie(name: String,value: String)

trait HttpResponse {
  def status: Int
  //statusText
  def statusText: String
  def headers: Map[String, Seq[String]]
  def body: String
  def cookies:Seq[HttpCookie]
}
