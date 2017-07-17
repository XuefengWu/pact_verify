package com.thoughtworks.verify.pact

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws._
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.util.Try

trait PactWS {
  def send(request: PactRequest): Try[HttpResponse]
  def close(): Unit
}
class PactWSImpl (urlRoot: String) extends PactWS{
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

  override def send(request: PactRequest): Try[HttpResponse] = {
    val responseF = chooseRequest(request.path, buildRequestBody(request),
      request.method.toString(), request.contentType, request.cookies, request.form)
    val responseTry = Try(Await.result(responseF, Duration(30, SECONDS)))
    responseTry.map(response => new HttpResponse {
      override def headers: Map[String, Seq[String]] = response.headers

      override def body: String = response.body

      override def status: Int = response.status

      override def statusText: String = response.statusText

      override def cookies: Seq[HttpCookie] = response.cookies.map(c => HttpCookie(c.name,c.value))
    })
  }

  def close(): Unit = {
    ws.close()
  }

}