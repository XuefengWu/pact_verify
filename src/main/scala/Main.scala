

import java.io.File

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

case class PactRequest(method: String, path: String, contentType: Option[String], body: Option[JsValue])

case class PactResponse(status: Int, body: Option[JsValue])

case class Interaction(description: String,
                       request: PactRequest,
                       response: PactResponse)

case class Pact(name: String, repeat: Option[Int], interactions: Seq[Interaction])

case class Pacts(name: String, pacts: Seq[Pact])

case class Error(typ: String, message: String)

case class Failure(typ: String, message: String)

case class TestCase(assertions: String, className: String, name: String, status: String, time: String,
                    error: Option[Error], failure: Option[Failure])

case class TestSuite(disabled: String, errors: Int, failures: Int, hostname: String, id: String,
                     name: String, pkg: String, skipped: String, tests: String, time: String,
                     timestamp: String, cases: Seq[TestCase])

case class TestSuites(disabled: String, errors: Int, failures: Int, name: String, tests: String, time: String, testSuites: Seq[TestSuite])

object Main extends App {

  println("welcome play pact v0.2.0")

  val root = if (args.length < 1) {
    println("Usage: java -jar pact-xx.jar pact_dir url_root")
    val _currentPath = new File("").getAbsolutePath
    println("Use current directory as pact_dir: " + _currentPath)
    _currentPath
  } else args(0)

  val urlRoot = if (args.length < 2) {
    println("Usage: java -jar pact-xx.jar pact_dir url_root")
    val defaultRoot = "http://localhost:8080"
    println("Use default: " + defaultRoot)
    defaultRoot
  } else args(1)
  //一个文件夹 - Pacts - TestSuites
  //一个文件 - Pact - TestSuite
  val now = new org.joda.time.DateTime().toString("yyyyMMDD_HH_mm")
  val reportDirPath = s"$root${File.separator}report_$now"
  val pactDir = new File(s"$root${File.separator}pacts")
  if (!pactDir.exists()) {
    println(s"${pactDir.getAbsolutePath} do not exists.")
    System.exit(-1)
  }
  new File(reportDirPath).mkdirs()
  val pactsList = PactFile.loadPacts(pactDir)
  val pactFs: Seq[Future[TestSuites]] = pactsList.map(pacts => Future(PactTester.testPacts(urlRoot, pacts)))

  for {
    f <- pactFs
    tss <- f
    ts <- tss.testSuites
    tc <- ts.cases if tc.failure.isDefined
    fail <- tc.failure
  } {
    println(s"\n${ts.name}::${tc.name} \n ${fail.message}")
  }
  val junitFs: Seq[Future[Unit]] = pactFs.map(tssF => tssF.map(tss => JunitReport.dumpJUnitReport(reportDirPath, tss)))
  Await.result(Future.sequence(junitFs), Duration(60, SECONDS))

}

