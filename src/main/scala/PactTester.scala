import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.Await
import scala.concurrent.duration._

object PactTester {

  private val PlaceHolderR = """"\$([a-zA-Z]+)\$"""".r

  private def testPact(urlRoot: String, pact: Pact): TestSuite = {
    val startPact = System.currentTimeMillis()
    val pactWS = new PactWS(urlRoot)

    var response: Option[JsValue] = None

    val result: Seq[TestCase] = for {
      i <- 1 to pact.repeat.getOrElse(1)
      interaction <- pact.interactions
    } yield {
        val start = System.currentTimeMillis()

        //TODO: 创建参数，参数的连续使用
        val request = rebuildRequest(interaction.request, response)
        val responseF = pactWS.send(request)
        val actual: WSResponse = Await.result(responseF, Duration(30, SECONDS))
        val expect: PactResponse = interaction.response
        val error = if (actual.status >= 500) Some(Error(actual.statusText, actual.body)) else None
        val failure = assert(expect, actual)
        val spend = (System.currentTimeMillis() - start) / 1000

        if (actual.status < 300) {
          response = Some(Json.parse(actual.body))
        } else {
          response = None
        }
        TestCase("assertions", interaction.description, interaction.description, "status", spend.toString, error, failure)
      }
    val errorsCount = result.count(_.error.isDefined)
    val failuresCount = result.count(_.failure.isDefined)
    val spendPact = (System.currentTimeMillis() - startPact) / 1000
    pactWS.close()
    TestSuite("disabled", errorsCount, failuresCount, "hostname", pact.name, pact.name, "pkg", "skipped", "tests",
      spendPact.toString, System.currentTimeMillis().toString, result)

  }

  private def rebuildRequest(request: PactRequest, responseOpt: Option[JsValue]): PactRequest = {
    val body = request.body
    if (body.isDefined && responseOpt.isDefined) {
      val response = responseOpt.get
      var requestBuf = request.body.get.toString()
      var url = request.path
      PlaceHolderR.findAllMatchIn(request.body.get.toString()).map { m => m.group(1) }.foreach { placeId =>
        val placeJsValue = (response \ placeId)
        if (!placeJsValue.isInstanceOf[JsUndefined]) {
          val placeValue = placeJsValue.toString().drop(1).dropRight(1)
          requestBuf = requestBuf.replaceAll("\\$" + placeId + "\\$", placeValue)
          url = url.replaceAll("\\$" + placeId + "\\$", placeValue)
        }
      }
      request.copy(path = url, body = Some(Json.parse(requestBuf)))
    } else request
  }

  private def assert(expect: PactResponse, actual: WSResponse): Option[Failure] = {
    actual match {
      case _ if expect.status != actual.status => Some(Failure(actual.statusText, s"Status: ${expect.status} != ${actual.status} \n${actual.body}"))
      case _ if expect.body.isDefined && !isEqual(expect.body.get, Json.parse(actual.body)) => Some(Failure(actual.statusText, s"期望:${expect.body.get}\n 实际返回:${actual.body}"))
      case _ => None
    }

  }

  private def isEqualObject(expect: JsObject, actual: JsObject): Boolean = {
    val asserts = expect.asInstanceOf[JsObject].fields.map { case (field, value) =>
      value == actual \ field
    }
    asserts.reduce(_ && _)
  }

  private def isEqualArray(expect: JsArray, actual: JsArray): Boolean = {
    if (expect.value.size == actual.value.size) {
      val actualValues = actual.value
      val asserts = expect.value.zipWithIndex.map { case (v, i) => isEqual(v, actualValues(i)) }
      asserts.reduce(_ && _)
    } else false
  }

  private def isEqual(expect: JsValue, actual: JsValue): Boolean = {
    if (expect.isInstanceOf[JsObject] && actual.isInstanceOf[JsObject]) {
      isEqualObject(expect.asInstanceOf[JsObject], actual.asInstanceOf[JsObject])
    } else if (expect.isInstanceOf[JsArray] && actual.isInstanceOf[JsArray]) {
      isEqualArray(expect.asInstanceOf[JsArray], actual.asInstanceOf[JsArray])
    } else {
      expect == actual
    }
  }

  def testPacts(urlRoot: String, pacts: Pacts): TestSuites = {
    val start = System.currentTimeMillis()
    val testSuites: Seq[TestSuite] = pacts.pacts.map(testPact(urlRoot, _))
    val spend = (System.currentTimeMillis() - start) / 1000
    TestSuites("disabled", testSuites.map(_.errors).reduce(_ + _), testSuites.map(_.failures).reduce(_ + _), pacts.name, "", spend.toString, testSuites)
  }


}
