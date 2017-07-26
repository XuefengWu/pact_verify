package com.thoughtworks.pact.verify

import java.util.Date

import com.thoughtworks.pact.verify.junit.{Error, TestCase, TestSuite, TestSuites}
import com.thoughtworks.pact.verify.pact._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{Await, Future}
import scala.util.Success

/**
  * Created by xfwu on 12/07/2017.
  */
object PactTestService {

  private def testPact(pactWS: PactWS, pact: Pact): TestSuite = {
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

      val spend = (System.currentTimeMillis() - start) / 1000
      TestCase("assertions", pact.source.getOrElse(""),
        interaction.description, error.map(_.typ).getOrElse(failure.map(_.typ).getOrElse("OK")), spend, error, failure)
    }
    generateTestSuite(pact, startPact, result.filterNot(_.name.startsWith("_before_")))
  }

  private def generateTestSuite(pact: Pact, startPact: Long, result: Seq[TestCase]) = {
    val errorsCount = result.count(_.error.isDefined)
    val failuresCount = result.count(_.failure.isDefined)
    val spendPact = (System.currentTimeMillis() - startPact) / 1000
    val name = pact.provider.map(_.name).getOrElse("target provider")
    TestSuite("disabled", errorsCount, failuresCount, "hostname", name, name, "pkg", "skipped",
      result.size, spendPact, System.currentTimeMillis().toString, result)
  }

  private def mergeCookie(request: PactRequest, cookiesOpt: Option[Seq[String]], cookie: Option[String]): PactRequest = {
    val requestCookies: Seq[String] = request.cookies.map(_.split(";").toSeq).getOrElse(Nil)
    val responseCookies: Seq[String] = cookiesOpt.getOrElse(Nil)
    val cookies: Seq[String] = cookie.map(_.split(";").toSeq).getOrElse(Nil)
    val mergedCookies: Seq[String] = requestCookies ++ responseCookies ++ cookies
    request.copy(cookies = Some(mergedCookies.distinct.mkString(";")))
  }

  def testPacts(pactsSeq: List[Pacts],urlRoot: String): List[TestSuites] = {
    pactsSeq.map(pacts => PactTestService.testPacts(pacts,urlRoot))
  }

  def testPacts(pacts: Pacts,urlRoot: String): TestSuites = {
    val pactWS = new PactWSImpl(urlRoot)
    val start = System.currentTimeMillis()
    val (successSeq, failurePactSeq) = pacts.pacts.partition(_.isSuccess)
    failurePactSeq.foreach(v => {
      println(s"pares failed: ${pacts.name}\n")
      v.failed.get.printStackTrace()
    }
    )
    val throwables = failurePactSeq.map(_.failed.get)
    val pactSeq = successSeq.map(_.get)
    val testSuites: Seq[TestSuite] = if (!throwables.isEmpty) {
      parseFailures(pacts.name, throwables) :: parseSuccesses(pactWS, pactSeq).toList
    } else {
      parseSuccesses(pactWS, pactSeq)
    }

    pactWS.close()
    val spend = (System.currentTimeMillis() - start)
    TestSuites("disabled", testSuites.map(_.errors).reduce(_ + _),
      testSuites.map(_.failures).reduce(_ + _), pacts.name, "", spend.toString, testSuites)
  }

  private def parseSuccesses(pactWS: PactWS, pactSeq: Seq[Pact]): Seq[TestSuite] = {
    val verifyResults: Seq[Future[TestSuite]] = pactSeq.map(v => Future(testPact(pactWS, v)))
    Await.result(Future.sequence(verifyResults), Duration(15,MINUTES))
  }

  private def parseFailures(name: String, fails: Seq[Throwable]): TestSuite = {
    val assertions = "parse json file"
    val status = "fail"
    val time = 0.01
    val tcs = fails.map(f => TestCase(assertions, f.getSuppressed.toSeq(0).getMessage, "",
      "parse fail", time, Some(Error("parse fail", f.getStackTrace.mkString("/n"))), None))
    TestSuite("false", 0, fails.size, "", "",
      name, name, "false", fails.size, time, new Date().toString, tcs)
  }

}
