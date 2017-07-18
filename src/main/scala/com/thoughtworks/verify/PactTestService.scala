package com.thoughtworks.verify

import java.util.Date

import com.thoughtworks.verify.Main.urlRoot
import com.thoughtworks.verify.junit._
import com.thoughtworks.verify.pact._

import scala.util.Success

/**
  * Created by xfwu on 12/07/2017.
  */
object PactTestService {

  private def testPact(pactWS: PactWS,  pact: Pact): TestSuite = {
    val startPact = System.currentTimeMillis()

    var preResponseOpt: Option[HttpResponse] = None
    var preCookiesOpt: Option[Seq[String]] = None

    def setForNextRequest(actual: HttpResponse) = {
      if (actual.status < 400) {
        preResponseOpt = Some(actual)
        if (actual.cookies.size > 0) {
          val cookies: Seq[String] = actual.cookies.map(c => s"${c.name}=${c.value}")
          //println(s"SetCookies: ${cookies.mkString(";")}")
          preCookiesOpt = Some(cookies)
        }
      } else {
        preResponseOpt = None
      }
    }

    val result: Seq[TestCase] = for {
      i <- 1 to pact.repeat.getOrElse(1)
      interaction <- pact.interactions
    } yield {
        val start = System.currentTimeMillis()
        //创建参数，参数的连续使用
        val request: PactRequest = PlaceHolder.replacePlaceHolderParameter(interaction.request, preResponseOpt)
        val mergedRequest = mergeCookie(request, preCookiesOpt, pact.cookies)
        val actualTry = pactWS.send(mergedRequest)

        val (error, failure) = actualTry match {
          case Success(actual) =>
            setForNextRequest(actual)
            val error = if (actual.status >= 500) Some(Error(actual.statusText, actual.body)) else None
            val failure = interaction.assert(request, actual)
            (error, failure)
          case scala.util.Failure(e) => (Some(Error(e.getMessage, e.getStackTrace.map(_.toString).mkString("\n"))), None)
        }

        val spend = (System.currentTimeMillis() - start)
        TestCase("assertions", pact.source.getOrElse(""),
          interaction.description, "status", spend.toString, error, failure)
      }
    generateTestSuite(pact, startPact, result)
  }

  private def generateTestSuite(pact: Pact, startPact: Long, result: Seq[TestCase]) = {
    val errorsCount = result.count(_.error.isDefined)
    val failuresCount = result.count(_.failure.isDefined)
    val spendPact = (System.currentTimeMillis() - startPact) / 1000
    val name = pact.provider.map(_.name).getOrElse("target provider")
    TestSuite("disabled", errorsCount, failuresCount, "hostname", name, name, "pkg", "skipped",
      "tests", spendPact.toString, System.currentTimeMillis().toString, result)
  }

  private def mergeCookie(request: PactRequest, cookiesOpt: Option[Seq[String]], cookie: Option[String]): PactRequest = {
    val requestCookies: Seq[String] = request.cookies.map(_.split(";").toSeq).getOrElse(Nil)
    val responseCookies: Seq[String] = cookiesOpt.getOrElse(Nil)
    val cookies: Seq[String] = cookie.map(_.split(";").toSeq).getOrElse(Nil)
    val mergedCookies: Seq[String] = requestCookies ++ responseCookies ++ cookies
    request.copy(cookies = Some(mergedCookies.distinct.mkString(";")))
  }

  def testPacts(pacts: Pacts): TestSuites = {
    val pactWS = new PactWSImpl(urlRoot)
    val start = System.currentTimeMillis()
    val (successSeq, failurePactSeq) = pacts.pacts.partition(_.isSuccess)
    failurePactSeq.foreach(v => {
      println(s"pares failed: ${pacts.name}\n")
      v.failed.get.printStackTrace()}
    )
    val throwables = failurePactSeq.map(_.failed.get)
    val pactSeq = successSeq.map(_.get)
    val testSuites: Seq[TestSuite] =
      parseFailures(pacts.name,throwables) :: parseSuccesses(pactWS,pactSeq).toList

    pactWS.close()
    val spend = (System.currentTimeMillis() - start)
    TestSuites("disabled", testSuites.map(_.errors).reduce(_ + _),
      testSuites.map(_.failures).reduce(_ + _), pacts.name, "", spend.toString, testSuites)
  }

  private def parseSuccesses(pactWS: PactWS,pactSeq: Seq[Pact]): Seq[TestSuite] =
    pactSeq.map(v => testPact(pactWS, v))

  private def parseFailures(name: String, fails: Seq[Throwable]): TestSuite = {
    val assertions = "parse json file"
    val status = "fail"
    val time = "0"
    val tcs = fails.map(f => TestCase(assertions, f.getSuppressed.toSeq(0).getMessage,"",
      status,time,Some(Error("parse fail",f.getStackTrace.mkString("/n"))),None))
    TestSuite("false",fails.size,0,"","",
      name,name,"false","","0",new Date().toString,tcs)
  }

}
