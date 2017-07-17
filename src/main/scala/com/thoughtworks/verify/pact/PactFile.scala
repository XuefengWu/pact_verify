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
      val before = beforeInteraction(files.head.getParentFile)
      files.filter(_.getName.endsWith(".json"))
        .filterNot(_.getName.startsWith("_"))
        .map(parsePactFile)
        .map(p => p.copy(interactions = before ::: p.interactions.toList))
    } else {
      Nil
    }
  }

  private def beforeInteraction(dir: File): List[Interaction] = {
    listFiles(dir).find(_.getName.equalsIgnoreCase("_before.json")) match {
      case Some(f) => parsePactFile(f).interactions.toList
      case None => Nil
    }
  }

  private def listFiles(dir: File): Seq[File] = {
    dir.listFiles().toSeq
  }

  private def parsePactFile(f: File): Pact = {
    val s = Source.fromFile(f).getLines().mkString("\n")
    parsePact(s)
  }
  private def parsePact(s: String): Pact = {
    Json.parse(s).as[Pact]
  }

}
