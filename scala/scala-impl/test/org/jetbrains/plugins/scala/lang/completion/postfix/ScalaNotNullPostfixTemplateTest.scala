package org.jetbrains.plugins.scala.lang.completion.postfix

import com.intellij.testFramework.TestIndexingModeSupporter.IndexingMode
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.util.runners.WithIndexingMode

import java.nio.file.Path

@WithIndexingMode(mode = IndexingMode.SMART, reason = "`notnull` needs type inference to check conformance with AnyRef")
class ScalaNotNullPostfixTemplateTest extends PostfixTemplateTest {
  override def testPath(): Path = super.testPath() / "notnull"

  def testChain(): Unit = doTest()

  def testInfix(): Unit = doTest()

  def testMethodCall(): Unit = doTest()

  def testNotApplicableBoolean(): Unit = doNotApplicableTest()

  def testNotApplicableInt(): Unit = doNotApplicableTest()

  def testParenthesized(): Unit = doTest()

  def testSimple(): Unit = doTest()
}
