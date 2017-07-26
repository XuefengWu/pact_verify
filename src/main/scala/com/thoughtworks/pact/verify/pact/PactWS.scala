package com.thoughtworks.pact.verify.pact

import java.util

import org.apache.commons.logging.LogFactory
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.{Consts, NameValuePair}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.util.Try

trait PactWS {
  def send(request: PactRequest): Try[HttpResponse]

  def close(): Unit
}

class PactWSImpl(urlRoot: String) extends PactWS {

  private val httpClient: CloseableHttpClient = HttpClients.createDefault()
  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  private def fullUrl(path: String): String = {
    if (!path.startsWith("http")) {
      urlRoot + path
    } else {
      path
    }
  }

  private def sendRequest(request: PactRequest): Future[Try[CloseableHttpResponse]] = {
    val method = buildRequest(request.path, buildRequestBody(request),
      request.method.toString(), request.contentType, request.cookies, request.form)
    Future(Try(httpClient.execute(method)))
  }

  private def setCookie(request: HttpRequestBase, cookies: Option[String]): HttpRequestBase = {
    cookies.foreach(c => request.setHeader("Set-Cookie", c))
    request
  }


  private def buildRequest(path: String, input: String, method: String, contentType: Option[String],
                           cookies: Option[String], form: Option[String]): HttpRequestBase = {
    val url: String = fullUrl(path)
    method.toLowerCase() match {
      case "get" => get(url, cookies)
      case "post" if form.isDefined => postForm(url, cookies, form)
      case "post" => postJson(url, cookies, input)
      case "put" => postJson(url, cookies, input)
      case "delete" => delete(url, cookies)
    }
  }

  private def delete(url: String, cookies: Option[String]): HttpRequestBase = {
    val request = new HttpDelete(url)
    setCookie(request, cookies)
  }

  private def putJson(url: String, cookies: Option[String], input: String): HttpRequestBase = {
    val request: HttpPut = new HttpPut(url)
    request.setHeader("Content-Type", "application/json")
    val entity = new StringEntity(input, Consts.UTF_8)
    entity.setContentEncoding("UTF-8")
    entity.setContentType("application/json")
    request.setEntity(entity)
    setCookie(request, cookies)
  }

  private def postJson(url: String, cookies: Option[String], input: String): HttpRequestBase = {
    val request: HttpPost = new HttpPost(url)
    request.setHeader("Content-Type", "application/json")
    val entity = new StringEntity(input, Consts.UTF_8)
    entity.setContentEncoding("UTF-8")
    entity.setContentType("application/json")
    request.setEntity(entity)
    setCookie(request, cookies)
  }

  private def postForm(url: String, cookies: Option[String], form: Option[String]): HttpRequestBase = {
    val request: HttpPost = new HttpPost(url)
    request.setHeader("Content-Type", "application/x-www-form-urlencoded")
    val formparams = new util.ArrayList[NameValuePair]()
    form.get.split("&").foreach(v => {
      val vs = v.split("=")
      formparams.add(new BasicNameValuePair(vs(0), vs(1)))
    })
    val entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8)
    request.setEntity(entity)
    setCookie(request, cookies)
  }


  private def get(url: String, cookies: Option[String]): HttpRequestBase = {
    val request: HttpGet = new HttpGet(url)
    setCookie(request, cookies)
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
    val responseF: Future[Try[CloseableHttpResponse]] = sendRequest(request)
    val triedResponse = Await.result(responseF.map(_.map(res => buildHttpResponse(res, request))), Duration(90, SECONDS))
    val result = Try(triedResponse).flatten
    result.recover{case error: Throwable => logger.error(error.getMessage, error)}
    result
  }

  private def buildHttpResponse(response: CloseableHttpResponse,request: PactRequest) = new HttpResponse {
    override val headers: Map[String, Seq[String]] = response.getAllHeaders.map(h => (h.getName,Seq(h.getValue))).toMap

    override val body: String = Try(EntityUtils.toString(response.getEntity))
                                .recover({case t:Throwable => s"${fullUrl(request.path)}\n${t.getStackTrace.mkString("/n")}"}).get

    override val status: Int = response.getStatusLine.getStatusCode

    override val statusText: String = response.getStatusLine.getReasonPhrase

    override val cookies: Seq[HttpCookie] = response.getHeaders("Set-Cookie").toSeq.map(h => HttpCookie(h.getName,h.getValue))
  }

  def close(): Unit = {
    httpClient.close()
  }

}