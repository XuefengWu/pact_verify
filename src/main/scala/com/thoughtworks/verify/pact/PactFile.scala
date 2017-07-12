package com.thoughtworks.verify.pact

import java.io.File

import play.api.libs.json.Json

import scala.io.Source

/**
  * Created by xfwu on 12/07/2017.
  */
object PactFile {
  implicit val pactRequestFormat = Json.format[PactRequest]
  implicit val pactResponseFormat = Json.format[PactResponse]
  implicit val interactionFormat = Json.format[Interaction]
  implicit val pactFormat = Json.format[Pact]
  implicit val pactsFormat = Json.format[Pacts]

  def loadPacts(dir: File): List[Pacts] = {
    val (subDirs, files) = listFiles(dir).partition(_.isDirectory)
    val pacts: Seq[Pact] = files.filter(_.getName.endsWith(".json")).map(f => Source.fromFile(f).getLines().mkString("\n")).map(parsePact)
    val subPacts: List[Pacts] = subDirs.flatMap(subDir => loadPacts(subDir)).toList
    if (pacts.isEmpty) {
      subPacts
    } else {
      subPacts :+ Pacts(dir.getName, pacts)
    }

  }

  private def listFiles(dir: File): Seq[File] = {
    dir.listFiles().toSeq
  }

  private def parsePact(s: String): Pact = {
    Json.parse(s).as[Pact]
  }

}
