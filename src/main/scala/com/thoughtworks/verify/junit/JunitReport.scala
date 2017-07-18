package com.thoughtworks.verify.junit

import scala.xml.Elem

/**
  * Created by xfwu on 12/07/2017.
  */
object JunitReport {

  def dumpJUnitReport(dir: String, testSuites: TestSuites): Unit = {
    val report = generateJUnitTestSuitesReport(testSuites)
    xml.XML.save(s"$dir/${testSuites.name}.xml", report, "UTF-8", true)
  }

  private def generateJUnitTestSuitesReport(testSuites: TestSuites): Elem = {
    <testsuites disabled=" " errors={testSuites.errors.toString} failures={testSuites.failures.toString} name={testSuites.name} tests=" " time={testSuites.time}>
      {testSuites.testSuites.map(generateJUnitTestSuiteReport)}
    </testsuites>
  }

  private def generateJUnitTestSuiteReport(testSuite: TestSuite): Elem = {
    <testsuite disabled=" " errors={testSuite.errors.toString} failures={testSuite.failures.toString} hostname=" " id=" " name={testSuite.name} package=" " skipped=" " tests={testSuite.tests.toString} time={testSuite.time} timestamp={testSuite.timestamp}>
      {testSuite.cases.map(generateJUnitTestCaseReport)}
    </testsuite>
  }

  private def generateJUnitTestCaseReport(testCase: TestCase): Elem = {
    <testcase assertions=" " classname={testCase.className} name={testCase.name} status=" " time={testCase.time}>
      {testCase.error.map(err => <error type={err.typ}>
      {err.message}
    </error>).getOrElse(xml.Null)}{testCase.failure.map(fail => <failure type={fail.typ}>
      {fail.message}
    </failure>).getOrElse(xml.Null)}
    </testcase>
  }
}
