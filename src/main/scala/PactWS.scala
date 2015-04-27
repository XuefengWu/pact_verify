import com.typesafe.config.ConfigFactory
import play.api.libs.ws.ning._
import play.api.libs.ws.{WS, WSResponse, _}

import scala.concurrent.Future

class PactWS(urlRoot: String) {

  val configuration = play.api.Configuration(ConfigFactory.parseString(""))
  val parser = new DefaultWSConfigParser(configuration, this.getClass.getClassLoader)
  val builder = new NingAsyncHttpClientConfigBuilder(parser.parse())
  implicit val sslClient = new play.api.libs.ws.ning.NingWSClient(builder.build())

  private def fullUrl(path: String, cookies: Option[String]) = WS.clientUrl(urlRoot + path).withHeaders("Cookie" -> cookies.getOrElse(""))

  private def fullUrlJson(path: String, contentType: Option[String], cookies: Option[String]) = fullUrl(path, cookies).withHeaders("Content-Type" -> contentType.getOrElse("application/json"))

  private def chooseRequest(path: String, input: String, method: String, contentType: Option[String], cookies: Option[String]) = method.toLowerCase() match {
    case "get" => fullUrl(path,cookies).get()
    case "post" => fullUrlJson(path, contentType,cookies).post(input)
    case "put" => fullUrlJson(path, contentType,cookies).put(input)
    case "delete" => fullUrl(path,cookies).delete()
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
    chooseRequest(request.path, buildRequestBody(request), request.method.toString(), request.contentType, request.cookies)
  }

  def close(): Unit = {
    sslClient.close()
  }
}