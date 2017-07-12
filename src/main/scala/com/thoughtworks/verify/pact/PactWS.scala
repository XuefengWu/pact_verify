package com.thoughtworks.verify.pact

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws._
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

class PactWS (urlRoot: String) {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val ws = AhcWSClient()

  private def fullUrl(path: String, cookies: Option[String]) = {
    val requestHolder = if (!path.startsWith("http")) {
      ws.url(urlRoot + path)
    } else {
      ws.url(path)
    }
    requestHolder.withHttpHeaders("Cookie" -> cookies.getOrElse(""))
  }

  private def fullUrlJson(path: String, contentType: Option[String], cookies: Option[String]) =
    fullUrl(path, cookies).withHttpHeaders("Content-Type" -> contentType.getOrElse("application/json"))

  private def chooseRequest(path: String, input: String, method: String, contentType: Option[String],
                            cookies: Option[String], form: Option[String]) = method.toLowerCase() match {
    case "get" => fullUrl(path, cookies).get()
    case "post" if form.isDefined => fullUrlJson(path, Some("application/x-www-form-urlencoded"), cookies).post(form.get)
    case "post" => fullUrlJson(path, contentType, cookies).post(input)
    case "put" => fullUrlJson(path, contentType, cookies).put(input)
    case "delete" => fullUrl(path, cookies).delete()
  }

  private def buildRequestBody(request: PactRequest): String = {
    if (request.contentType == Some("text/plain")) {
      val body = request.body.fold("")(_.toString())
      body.drop(1).dropRight(1)
    } else {
      request.body.fold("")(_.toString())
    }
  }

  def send(request: PactRequest): Future[WSResponse] = {
    chooseRequest(request.path, buildRequestBody(request),
      request.method.toString(), request.contentType, request.cookies, request.form)
  }

  def close(): Unit = {
    ws.close()
  }

}