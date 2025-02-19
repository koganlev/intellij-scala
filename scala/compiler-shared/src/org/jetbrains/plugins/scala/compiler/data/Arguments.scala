package org.jetbrains.plugins.scala.compiler.data

import org.jetbrains.plugins.scala.compiler.data.serialization.{SerializationUtils, WorksheetArgsSerializer}
import org.jetbrains.plugins.scala.compiler.data.worksheet.WorksheetArgs

case class Arguments(sbtData: SbtData,
                     compilerData: CompilerData,
                     compilationData: CompilationData,
                     // TODO: separate different kinds of requests: Compile / Run worksheet / Run Repl worksheet / (potentially run tests)
                     worksheetArgs: Option[WorksheetArgs]) {

  import SerializationUtils.{pathToString, pathsToString, sequenceToString}

  /** @see `org.jetbrains.jps.incremental.scala.data.ArgumentsParser.parse` */
  def asStrings: Seq[String] = {
    val (outputs, caches) = compilationData.outputToCacheMap.toSeq.unzip
    val (sourceRoots, outputDirs) = compilationData.outputGroups.unzip

    SbtData.serialize(sbtData) ++
      CompilerData.serialize(compilerData) ++
      Seq(
        pathsToString(compilationData.sources),
        pathsToString(compilationData.classpath),
        pathToString(compilationData.output),
        sequenceToString(compilationData.scalaOptions),
        sequenceToString(compilationData.javaOptions),
        compilationData.order.toString,
        pathToString(compilationData.cacheFile),
        pathsToString(outputs),
        pathsToString(caches),
        pathsToString(sourceRoots),
        pathsToString(outputDirs),
        sequenceToString(worksheetArgs.map(WorksheetArgsSerializer.serialize).getOrElse(Nil)),
        //sbtIncOptions
        pathsToString(compilationData.zincData.allSources),
        compilationData.zincData.compilationStartDate.toString,
        compilationData.zincData.isCompile.toString
      )
  }
}