package org.jetbrains.plugins.scala.compiler.highlighting

import junitparams.naming.TestCaseName
import junitparams.{JUnitParamsRunner, Parameters}
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import scala.annotation.unused

@RunWith(classOf[JUnitParamsRunner])
class CompilerMessagesTest {

  private case class TestCase(displayName: String, originalMessage: String, expectedDescription: String)

  @unused("used reflectively by the @Parameters annotation")
  private val testParameters: Array[AnyRef] = Array(
    TestCase(
      displayName = "sbtMultilineMessage",
      originalMessage = "Found:    Conversion.DoubleWrapper\nRequired: Int\n    override def apply(i: IntWrapper): Int = DoubleWrapper(i.a.toDouble)",
      expectedDescription = "Found:    Conversion.DoubleWrapper\nRequired: Int"),
    TestCase(
      displayName = "bspMultilineMessage",
      originalMessage = "Found:    Conversion.DoubleWrapper\nRequired: Int [14:46]",
      expectedDescription = "Found:    Conversion.DoubleWrapper\nRequired: Int"),
    TestCase(
      displayName = "deprecationWarningsMessage",
      originalMessage = "there were 4 deprecation warnings; re-run with -deprecation for details\n\n",
      expectedDescription = "there were 4 deprecation warnings; re-run with -deprecation for details"),
    TestCase(
      displayName = "oneLineMessage",
      originalMessage = "This is a one line error message",
      expectedDescription = "This is a one line error message"),
    TestCase(
      displayName = "blankAfterProcessing",
      originalMessage = "\n\nSome message  \n  ",
      expectedDescription = "Some message")
  ).map { case TestCase(displayName, originalMessage, expectedDescription) =>
    Array(displayName, originalMessage, expectedDescription)
  }

  @Test
  @Parameters(method = "testParameters")
  @TestCaseName(value = "{0}")
  def compilerMessageTest(
    @unused("used reflectively by the @TestCaseName annotation") testName: String,
    originalMessage: String,
    expectedDescription: String
  ): Unit = {
    assertMessageDescription(originalMessage, expectedDescription)
  }

  private def assertMessageDescription(originalMessage: String, expectedDescription: String): Unit = {
    val actualDescription = CompilerMessages.description(originalMessage)
    assertEquals(expectedDescription, actualDescription)
  }
}
