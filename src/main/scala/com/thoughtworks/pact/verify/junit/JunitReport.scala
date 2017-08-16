package com.thoughtworks.pact.verify.junit


import java.io.File

import scala.xml.{Elem, PCData}

/**
  * Created by xfwu on 12/07/2017.
  */
object JunitReport {

  def dumpJUnitReport(reportDirPath: String, testSuitesSeq: Seq[TestSuites]): Seq[String] = {
    testSuitesSeq.map(tss => dumpJUnitReport(reportDirPath, tss))
  }

  private def dumpJUnitReport(dir: String, testSuites: TestSuites): String = {
    val report = generateJUnitTestSuitesReport(testSuites)
    new File(dir).mkdirs()
    val file = s"$dir/${testSuites.name}.xml"
    xml.XML.save(file, report, "UTF-8", true)
    file
  }

  private def generateJUnitTestSuitesReport(testSuites: TestSuites): Elem = {
    <testsuites disabled=" " errors={testSuites.errors.toString} failures={testSuites.failures.toString} name={testSuites.name} tests=" " time={testSuites.time}>
      {testSuites.testSuites.map(generateJUnitTestSuiteReport)}
    </testsuites>
  }

  private def generateJUnitTestSuiteReport(testSuite: TestSuite): Elem = {
    <testsuite disabled=" " errors={testSuite.errors.toString} failures={testSuite.failures.toString} hostname=" " id=" " name={testSuite.name} package=" " skipped=" " tests={testSuite.tests.toString} time={testSuite.time.toString} timestamp={testSuite.timestamp}>
      {testSuite.cases.map(generateJUnitTestCaseReport)}
    </testsuite>
  }

  private def generateJUnitTestCaseReport(testCase: TestCase): Elem = {
    <testcase assertions={testCase.assertions} classname={testCase.className} name={testCase.name} status={testCase.status} time={testCase.time.toString}>
      {testCase.error.map(err => <error type={err.typ} message={err.message}>
      {err.message}

      {err.detail.map(v => new PCData(s"\n${v}\n")).getOrElse("")}
    </error>).getOrElse(xml.Null)}{testCase.failure.map(fail => <failure type={fail.typ} message={fail.message}>
      {fail.message}


      {fail.detail.map(v => new PCData(s"\n${v}\n")).getOrElse("")}
    </failure>).getOrElse(xml.Null)}
    </testcase>
  }
}
