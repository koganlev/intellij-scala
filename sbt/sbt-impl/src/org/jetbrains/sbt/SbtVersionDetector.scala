package org.jetbrains.sbt

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.extensions.RichFile
import org.jetbrains.sbt.project.SbtExternalSystemManager

import java.io.{BufferedInputStream, File, FileInputStream}
import java.util.Properties
import java.util.jar.JarFile
import scala.util.Using

object SbtVersionDetector {

  def detectSbtVersion(project: Project): SbtVersion = {
    val settings = SbtExternalSystemManager.executionSettingsFor(project)
    val projectBaseDir = new File(settings.realProjectPath)
    val launcher = settings.customLauncher.getOrElse(SbtUtil.getDefaultLauncher)
    detectSbtVersion(projectBaseDir, launcher)
  }

  /**
   * The version of sbt defined in `project/build.properties` or fallback to the sbt version corresponding to launcher
   */
  def detectSbtVersion(projectDir: File, sbtLauncher: => File): SbtVersion = {
    val fromProject = sbtVersionInProjectPropertiesFile(projectDir)
    val fromProjectOrLauncher = fromProject.orElse(sbtVersionInBootPropertiesOfSbtLauncher(sbtLauncher)).map(SbtVersion(_))
    fromProjectOrLauncher.getOrElse(SbtVersion.Latest.Sbt_1)
  }

  /**
   * Reads sbt version from the `project/build.properties` file.<br>
   * File content example: {{{
   *   sbt.version = 1.10.7
   * }}}
   */
  private def sbtVersionInProjectPropertiesFile(projectDir: File): Option[String] = {
    val projectPropertiesFile = projectDir / Sbt.ProjectDirectory / Sbt.PropertiesFile
    if (projectPropertiesFile.exists())
      readPropertyFrom(projectPropertiesFile, "sbt.version")
    else
      None
  }

  //noinspection SameParameterValue
  private def readPropertyFrom(file: File, name: String): Option[String] = {
    Using.resource(new BufferedInputStream(new FileInputStream(file))) { input =>
      val properties = new Properties()
      properties.load(input)
      Option(properties.getProperty(name))
    }
  }

  // Examples: 1.10.7, 1.10.7-M1, 1.10.7-RC2
  private val BootFileVersionInLineRegex = """\d+(\.\d+)*(-\w+)?""".r

  /**
   * Reads sbt version from the `sbt-launch.jar/sbt/sbt.boot.properties` file.<br>
   * File content example: {{{
   *   [app]
   *     name: sbt
   *     version: ${sbt.version-read(sbt.version)[1.10.7]}
   *   ...
   * }}}
   */
  private def sbtVersionInBootPropertiesOfSbtLauncher(launcherJar: File): Option[String] = {
    val appProperties = readSectionFromBootPropertiesOf(launcherJar, sectionName = "app")
    if (appProperties.get("name").contains("sbt")) {
      for {
        versionStr <- appProperties.get("version")
        version <- BootFileVersionInLineRegex.findFirstIn(versionStr)
      } yield version
    }
    else None
  }

  //noinspection SameParameterValue
  private def readSectionFromBootPropertiesOf(launcherFile: File, sectionName: String): Map[String, String] = {
    val jar = new JarFile(launcherFile)
    try {
      Option(jar.getEntry("sbt/sbt.boot.properties")).fold(Map.empty[String, String]) { entry =>
        val lines = scala.io.Source.fromInputStream(jar.getInputStream(entry)).getLines().toArray.toSeq
        readSectionFromBootPropertiesFileContent(lines, sectionName)
      }
    } finally {
      jar.close()
    }
  }

  private def readSectionFromBootPropertiesFileContent(fileLines: Seq[String], sectionName: String): Map[String, String] = {
    val sectionLines = fileLines
      .dropWhile(_.trim != s"[$sectionName]").drop(1)
      .takeWhile(!_.trim.startsWith("["))
    sectionLines.flatMap(findBootFileProperty).toMap
  }

  private val BootFilePropertyRegex = "^\\s*(\\w+)\\s*:(.+)".r.unanchored

  private def findBootFileProperty(line: String): Option[(String, String)] =
    line match {
      case BootFilePropertyRegex(name, value) => Some((name, value.trim))
      case _ => None
    }
}
