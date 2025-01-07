package org.jetbrains.plugins.scala.debugger.evaluation.evaluator.compiling

import java.net.URI
import java.nio.file.{Files, Path}

class OutputFileObject(file: Path, val origName: String) {
  private def getUri(name: String): URI = {
    URI.create("memo:///" + name.replace('.', '/') + ".class")
  }

  def getName: String = getUri(origName).getPath
  def toByteArray: Array[Byte] = Files.readAllBytes(file)
}
