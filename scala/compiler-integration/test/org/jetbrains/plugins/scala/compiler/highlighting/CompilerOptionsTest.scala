package org.jetbrains.plugins.scala.compiler.highlighting

import junitparams.naming.TestCaseName
import junitparams.{JUnitParamsRunner, Parameters}
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import scala.annotation.unused

@RunWith(classOf[JUnitParamsRunner])
class CompilerOptionsTest {

  @unused("used reflectively by the @Parameters annotation")
  private def fatalWarningsTestParameters: Array[AnyRef] = {
    case class FatalWarningsTestCase(displayName: String, scalacOptions: Seq[String], expectedFlag: Boolean)

    Array(
      FatalWarningsTestCase(
        displayName = "fatalWarningsFlag",
        scalacOptions = Seq("-Wunused:patvars,imports,privates", "-Wunused:locals,explicits", "-Xfatal-warnings", "-Wunused:implicits", "-Wnumeric-widen"),
        expectedFlag = true
      ),
      FatalWarningsTestCase(
        displayName = "werrorFlag",
        scalacOptions = Seq("-Wunused:patvars,imports,privates", "-Wunused:locals,explicits", "-Werror", "-Wunused:implicits", "-Wnumeric-widen"),
        expectedFlag = true
      ),
      FatalWarningsTestCase(
        displayName = "noFlag",
        scalacOptions = Seq("-Wunused:patvars,imports,privates", "-Wunused:locals,explicits", "-Wunused:implicits", "-Wnumeric-widen"),
        expectedFlag = false
      )
    ).map { case FatalWarningsTestCase(displayName, scalacOptions, expectedFlag) =>
      Array(displayName, scalacOptions, expectedFlag)
    }
  }

  @Test
  @Parameters(method = "fatalWarningsTestParameters")
  @TestCaseName(value = "{0}")
  def fatalWarningsTest(
    @unused("used reflectively by the @TestCaseName annotation") testName: String,
    scalacOptions: Seq[String],
    expectedFlag: Boolean
  ): Unit = {
    val actualFlag = CompilerOptions.containsFatalWarnings(scalacOptions)
    assertEquals(s"Fatal warnings flag should be $expectedFlag for scalacOptions: $scalacOptions", expectedFlag, actualFlag)
  }
  
  @unused("used reflectively by the @Parameters annotation")
  private def unusedImportsTestParameters: Array[AnyRef] = {
    case class UnusedImportsTestCase(displayName: String, scalacOptions: Seq[String], expectedFlag: Boolean)

    Array(
      UnusedImportsTestCase(
        displayName = "manyUnusedFlagsIncludingImports",
        scalacOptions = Seq("-Wunused:locals,explicits", "-Xfatal-warnings", "-Wunused:patvars,imports,privates", "-Wnumeric-widen", "-Wunused:implicits"),
        expectedFlag = true
      ),
      UnusedImportsTestCase(
        displayName = "unusedAllFlag",
        scalacOptions = Seq("-Wnumeric-widen", "-Wunused:all", "-Werror"),
        expectedFlag = true
      ),
      UnusedImportsTestCase(
        displayName = "unusedAllAndImports",
        scalacOptions = Seq("-Wunused:all,privates,imports,explicits", "-Werror"),
        expectedFlag = true
      ),
      UnusedImportsTestCase(
        displayName = "noUnusedImportsFlag",
        scalacOptions = Seq("-Wunused:patvars,privates", "-Wunused:locals,explicits", "-Wunused:implicits", "-Wnumeric-widen"),
        expectedFlag = false
      ),
      UnusedImportsTestCase(
        displayName = "repeateadUnusedImports",
        scalacOptions = Seq("-Wunused:imports,patvars,imports", "-Wunused:imports,explicits", "-Wunused:imports", "-Wnumeric-widen"),
        expectedFlag = true
      )
    ).map { case UnusedImportsTestCase(displayName, scalacOptions, expectedFlag) =>
      Array(displayName, scalacOptions, expectedFlag)
    }
  }
  
  @Test
  @Parameters(method = "unusedImportsTestParameters")
  @TestCaseName(value = "{0}")
  def unusedImportsTest(
    @unused("used reflectively by the @TestCaseName annotation") testName: String,
    scalacOptions: Seq[String],
    expectedFlag: Boolean
  ): Unit = {
    val actualFlag = CompilerOptions.containsUnusedImports(scalacOptions)
    assertEquals(s"Unused imports flag should be $expectedFlag for scalacOptions: $scalacOptions", expectedFlag, actualFlag)
  }
}
