package org.jetbrains.plugins.scala.compiler.data

import org.jetbrains.jps.incremental.scala.Extractor
import org.jetbrains.plugins.scala.compiler.data.Extractors.{StringToPath, StringToPaths}

import java.nio.file.Path

case class CompilerData(compilerJars: Option[CompilerJars],
                        javaHome: Option[Path],
                        incrementalType: IncrementalityType)

object CompilerData {
  import serialization.SerializationUtils.{optionToString, pathToString, pathsToString}

  def serialize(data: CompilerData): Seq[String] = {
    val compilerJarPaths = data.compilerJars.map(jars => pathsToString(jars.allJars))
    val customCompilerBridgeJarPath = data.compilerJars.flatMap(_.customCompilerBridgeJar.map(pathToString))
    val javaHomePath = data.javaHome.map(pathToString)

    Seq(
      optionToString(compilerJarPaths),
      optionToString(customCompilerBridgeJarPath),
      optionToString(javaHomePath),
      data.incrementalType.name()
    )
  }

  def deserialize(strings: Seq[String]): Either[String, (CompilerData, Seq[String])] = strings match {
    case StringToOption(compilerJarPaths) +:
      StringToOption(customCompilerBridgeJarPath) +:
      StringToOption(javaHomePath) +:
      incrementalTypeName +:
      tail =>
      val compilerJars = compilerJarPaths.map {
        case StringToPaths(files) =>
          val compilerBridgeJar = customCompilerBridgeJarPath.map(StringToPath)
          CompilerJarsFactory.fromFiles(files, compilerBridgeJar) match {
            case Left(resolveError) => return Left(s"Couldn't extract compiler jars from: ${files.mkString(";")}\n$resolveError")
            case Right(jars) => jars
          }
      }
      val javaHome = javaHomePath.map {
        case StringToPath(file) => file
      }
      val incrementalType = IncrementalityType.valueOf(incrementalTypeName)
      Right(CompilerData(compilerJars, javaHome, incrementalType) -> tail)

    case args => Left(s"The arguments don't match the expected shape of CompilerData: ${args.mkString("[", ",", "]")}")
  }

  private val StringToOption: Extractor[String, Option[String]] = { s =>
    if (s.isEmpty) None else Some(s)
  }
}
