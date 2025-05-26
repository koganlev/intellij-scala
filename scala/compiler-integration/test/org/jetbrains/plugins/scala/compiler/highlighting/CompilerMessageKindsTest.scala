package org.jetbrains.plugins.scala.compiler.highlighting

import junitparams.naming.TestCaseName
import junitparams.{JUnitParamsRunner, Parameters}
import org.jetbrains.jps.incremental.scala.MessageKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import scala.annotation.unused

@RunWith(classOf[JUnitParamsRunner])
class CompilerMessageKindsTest {

  private case class TestCase(
    displayName: String,
    text: String,
    kind: MessageKind,
    fatalWarningsFlag: Boolean,
    unusedImportsFlag: Boolean,
    expected: HighlightInfoType
  )

  @unused("used reflectively by the @Parameters annotation")
  private def testParameters: Array[AnyRef] = Array(
    TestCase(
      displayName = "wrongRef1",
      text = "Value myValue is not a member of MyType",
      kind = MessageKind.Error,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.WrongRef),
    TestCase(
      displayName = "wrongRef2",
      text = "Not found: myValue",
      kind = MessageKind.Error,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.WrongRef),
    TestCase(
      displayName = "wrongRef3",
      text = "Cannot find symbol myValue",
      kind = MessageKind.Error,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.WrongRef),
    TestCase(
      displayName = "errorUnusedImportFatalWarningsWithFlag",
      text = "Unused import",
      kind = MessageKind.Error,
      fatalWarningsFlag = true,
      unusedImportsFlag = true,
      expected = HighlightInfoType.Error),
    TestCase(
      displayName = "errorUnusedImportNoFlag",
      text = "Unused import",
      kind = MessageKind.Error,
      fatalWarningsFlag = true,
      unusedImportsFlag = false,
      expected = HighlightInfoType.UnusedSymbol),
    TestCase(
      displayName = "errorUnusedImportNoFatalWarningsNoFlag",
      text = "Unused import",
      kind = MessageKind.Error,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.UnusedSymbol),
    TestCase(
      displayName = "regularError",
      text = "Some random compiler error",
      kind = MessageKind.Error,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Error),
    TestCase(
      displayName = "upgradeWarningUnusedImportFatalWarningsWithFlag",
      text = "Unused import",
      kind = MessageKind.Warning,
      fatalWarningsFlag = true,
      unusedImportsFlag = true,
      expected = HighlightInfoType.Error),
    TestCase(
      displayName = "warningUnusedImportNoFatalWarningsWithFlag",
      text = "Unused import",
      kind = MessageKind.Warning,
      fatalWarningsFlag = false,
      unusedImportsFlag = true,
      expected = HighlightInfoType.Warning),
    TestCase(
      displayName = "warningUnusedImportFatalWarningsNoFlag",
      text = "Unused import",
      kind = MessageKind.Warning,
      fatalWarningsFlag = true,
      unusedImportsFlag = false,
      expected = HighlightInfoType.UnusedSymbol),
    TestCase(
      displayName = "warningUnusedImportNoFatalWarningsNoFlag",
      text = "Unused import",
      kind = MessageKind.Warning,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.UnusedSymbol),
    TestCase(
      displayName = "regularWarning",
      text = "Some random compiler warning",
      kind = MessageKind.Warning,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Warning),
    TestCase(
      displayName = "upgradeRegularWarningToErrorFatalWarnings",
      text = "Some random compiler warning",
      kind = MessageKind.Warning,
      fatalWarningsFlag = true,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Error),
    TestCase(
      displayName = "regularInfo",
      text = "Some random compiler info",
      kind = MessageKind.Info,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.WeakWarning),
    TestCase(
      displayName = "internalBuilderError",
      text = "Some random compiler internal builder error",
      kind = MessageKind.InternalBuilderError,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Information),
    TestCase(
      displayName = "jpsInfo",
      text = "Some random jps info",
      kind = MessageKind.JpsInfo,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Information),
    TestCase(
      displayName = "progress",
      text = "Some random progress message",
      kind = MessageKind.Progress,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Information),
    TestCase(
      displayName = "other",
      text = "Some random other message",
      kind = MessageKind.Other,
      fatalWarningsFlag = false,
      unusedImportsFlag = false,
      expected = HighlightInfoType.Information)
  ).map(tc => Array(tc.displayName, tc))

  @Test
  @Parameters(method = "testParameters")
  @TestCaseName(value = "{0}")
  def highlightInfoTypeTest(
    @unused("used reflectively by the @TestCaseName annotation") testName: String,
    testCase: TestCase
  ): Unit = {
    val TestCase(_, text, kind, fatalWarningsFlag, unusedImportsFlag, expected) = testCase
    val actual = CompilerMessageKinds.highlightInfoType(kind, text, fatalWarningsFlag, unusedImportsFlag)
    assertEquals(expected, actual)
  }
}
