package com.thoughtworks.pact.verify.pact

import java.util

import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.logging.LogFactory
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.{Consts, Header, NameValuePair}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.util.Try

trait PactWS {
  def send(request: PactRequest): Try[HttpResponse]

  def close(): Unit
}

class PactWSImpl(urlRoot: String) extends PactWS {

  private val httpCLients: ListBuffer[CloseableHttpClient] = ListBuffer[CloseableHttpClient]()
  private val logger = LogFactory.getFactory.getInstance(this.getClass)

  private def fullUrl(path: String): String = {
    if (!path.startsWith("http")) {
      urlRoot + path
    } else {
      path
    }
  }

  private def sendRequest(request: PactRequest): (Future[Try[CloseableHttpResponse]],CloseableHttpClient) = {
    val method = buildRequest(request.path, buildRequestBody(request),
      request.method.toString(), request.contentType, request.cookies, request.form)
    logger.trace(s"request cookies: ${request.cookies}")
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    httpCLients += httpClient
    (Future(Try(httpClient.execute(method))),httpClient)
  }

  private def setCookie(request: HttpRequestBase, cookies: Option[String]): HttpRequestBase = {
    cookies.foreach(c => request.setHeader("Set-Cookie", c))
    cookies.foreach(c => request.setHeader("Cookie", c))
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
    val (responseF: Future[Try[CloseableHttpResponse]],client:CloseableHttpClient) = sendRequest(request)
    val triedResponse = Try(Await.result(responseF.map(_.map(res => buildHttpResponse(res, request))),
                                          Duration(request.timeout.getOrElse(120), SECONDS))).flatten
    triedResponse.recover{case error: Throwable => {
                      logger.error(error.getMessage, error)
                      httpCLients -= client
                      client.close()}}
    triedResponse
  }

  private def buildHttpResponse(response: CloseableHttpResponse,request: PactRequest) = new HttpResponse {
    override val headers: Map[String, Seq[String]] = response.getAllHeaders.map(h => (h.getName,Seq(h.getValue))).toMap

    override val body: String = Try(EntityUtils.toString(response.getEntity))
                                .recover({case t:Throwable => s"${fullUrl(request.path)}\n${ExceptionUtils.getStackTrace(t)}"}).get

    override val status: Int = response.getStatusLine.getStatusCode

    override val statusText: String = response.getStatusLine.getReasonPhrase

    override val cookies: Seq[HttpCookie] = {
      val res: Seq[Option[HttpCookie]] = response.getHeaders("Set-Cookie").map(_.getValue).map(buildHttpCookie)
      res.filter(_.isDefined).map(_.get)
    }

    private def buildHttpCookie(v: String): Option[HttpCookie] = {
      logger.trace(s"buildHttpCookie: $v")
      val kv = v.split("=")
      if(kv.length > 1) {
        Some(HttpCookie(kv(0).trim, kv(1).trim))
      } else {
        None
      }
    }
  }


  def close(): Unit = {
    httpCLients.foreach(_.close())
  }

}