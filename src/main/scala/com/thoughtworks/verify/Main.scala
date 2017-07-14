package com.thoughtworks.verify

import java.io.File

import com.thoughtworks.verify.junit.{JunitReport, TestSuites}
import com.thoughtworks.verify.pact.{PactFile, PactWSImpl}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
/**
  * Created by xfwu on 12/07/2017.
  */
object Main extends App {

  println("welcome play pact v0.4.0")

  val root = if (args.length < 1) {
    println("Usage: java -jar pact-xx.jar pact_dir url_root")
    val _currentPath = new File("").getAbsolutePath    
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
  val reportDirPath = s"$root${File.separator}report"
  val pactDir = new File(s"$root")
  println("pact working directory: " + pactDir.getAbsolutePath)
  if (!pactDir.exists()) {
    println(s"${pactDir.getAbsolutePath} do not exists.")
    System.exit(-1)
  }
  new File(reportDirPath).mkdirs()
  val pactsList = PactFile.loadPacts(pactDir)
  if(pactsList.isEmpty){
    println(s"${pactDir.getAbsolutePath} do not contains pact json files")
    System.exit(-1)
  }
  val pactWS = new PactWSImpl(urlRoot)
  val pactFs: Seq[Future[TestSuites]] = pactsList.map(pacts => Future(PactTestService.testPacts(pactWS, pacts)))

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
