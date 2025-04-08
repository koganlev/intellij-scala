package org.jetbrains.plugins.scala.compiler

import org.jetbrains.plugins.scala.compiler.CompilerMessagesUtil.{assertCompilingScalaSources, assertNoErrorsOrWarnings}
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettingsProfile
import org.jetbrains.plugins.scala.{CompilationTests_IDEA, CompilationTests_Zinc, ScalaVersion}
import org.junit.experimental.categories.Category

import scala.jdk.CollectionConverters._

abstract class EncodingCompilationTestBase(override val incrementalityType: IncrementalityType) extends ScalaCompilerTestBase with JdkVersionDiscovery {

  override protected def supportedIn(version: ScalaVersion): Boolean = version == ScalaVersion.Latest.Scala_3

  def testEncoding1(): Unit = {
    runEncodingTest(Seq("-encoding", "UTF-8"))
  }

  def testEncoding2(): Unit = {
    runEncodingTest(Seq("--encoding", "UTF-8"))
  }

  def testEncoding3(): Unit = {
    runEncodingTest(Seq("-encoding:UTF-8"))
  }

  def testEncoding4(): Unit = {
    runEncodingTest(Seq("--encoding:UTF-8"))
  }

  private def runEncodingTest(encodingSettings: Seq[String]): Unit = {
    addFileToProjectSources("Foo.scala", "class Foo")
    val profile = ScalaCompilerSettingsProfile.forModule(getModule)
    val newSettings = profile.getSettings.copy(
      additionalCompilerOptions = encodingSettings
    )
    profile.setSettings(newSettings)

    val messages = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages)

    incrementalityType match {
      case IncrementalityType.SBT => assertCompilingScalaSources(messages, 1)
      case IncrementalityType.IDEA =>
    }
  }
}

@Category(Array(classOf[CompilationTests_Zinc]))
class EncodingCompilationTest_Zinc extends EncodingCompilationTestBase(IncrementalityType.SBT)

@Category(Array(classOf[CompilationTests_IDEA]))
class EncodingCompilationTest_IDEA extends EncodingCompilationTestBase(IncrementalityType.IDEA)
