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
  implicit val providerFormat = Json.format[Provider]
  implicit val consumerFormat = Json.format[Consumer]
  implicit val pactFormat = Json.format[Pact]
  implicit val pactsFormat = Json.format[Pacts]

  def loadPacts(dir: File): List[Pacts] = {
    val (subDirs, files) = listFiles(dir).partition(_.isDirectory)
    val pacts: Seq[Pact] = parsePacts(files)
    val subPacts: List[Pacts] = loadParsePacts(subDirs)
    if (pacts.isEmpty) {
      subPacts
    } else {
      subPacts :+ Pacts(dir.getName, pacts)
    }
  }

  private def loadParsePacts(subDirs: Seq[File]): List[Pacts] = {
    if(subDirs != null && !subDirs.isEmpty) {
      subDirs.flatMap(subDir => loadPacts(subDir)).toList
    } else {
      Nil
    }
  }

  private def parsePacts(files: Seq[File]): Seq[Pact] = {
    if(files != null && !files.isEmpty) {
      files.filter(_.getName.endsWith(".json")).map(f => Source.fromFile(f).getLines().mkString("\n")).map(parsePact)
    } else {
      Nil
    }
  }

  private def listFiles(dir: File): Seq[File] = {
    dir.listFiles().toSeq
  }

  private def parsePact(s: String): Pact = {
    Json.parse(s).as[Pact]
  }

}
