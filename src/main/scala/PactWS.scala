import play.api.libs.ws.{WSResponse, WS}

import scala.concurrent.Future

class PactWS(urlRoot: String) {

  import com.ning.http.client.AsyncHttpClientConfig
  import play.api.libs.ws.ning._

  private val clientConfig = new DefaultNingWSClientConfig()
  private val secureDefaults: AsyncHttpClientConfig = new NingAsyncHttpClientConfigBuilder(clientConfig).build()
  // You can directly use the builder for specific options once you have secure TLS defaults...
  private val builder = new AsyncHttpClientConfig.Builder(secureDefaults)
  private val secureDefaultsWithSpecificOptions: AsyncHttpClientConfig = builder.build()
  private implicit val sslClient = new NingWSClient(secureDefaultsWithSpecificOptions)

  private def fullUrl(path: String) = WS.clientUrl(urlRoot + path)

  private def fullUrlJson(path: String, contentType: Option[String]) = fullUrl(path).withHeaders("Content-Type" -> contentType.getOrElse("application/json"))

  private def chooseRequest(path: String, input: String, method: String, contentType: Option[String]) = method.toLowerCase() match {
    case "get" => fullUrl(path).get()
    case "post" => fullUrlJson(path, contentType).post(input)
    case "put" => fullUrlJson(path, contentType).put(input)
    case "delete" => fullUrl(path).delete()
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
    chooseRequest(request.path, buildRequestBody(request), request.method.toString(), request.contentType)
  }

  def close(): Unit = {
    sslClient.close()
  }
}