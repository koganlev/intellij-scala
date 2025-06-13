package org.jetbrains.plugins.scala.lang.completion3

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.lang.completion3.base.ScalaCompletionTestBase
import org.jetbrains.plugins.scala.lang.completion3.base.ScalaCompletionTestBase.hasItemText

class Scala3OpaqueTypeAliasTest extends ScalaCompletionTestBase {
  override protected def supportedIn(version: ScalaVersion): Boolean = version >= ScalaVersion.Latest.Scala_3_0

  def testRenderingInside(): Unit = doRawCompletionTest(
    fileText =
      s"""object Inside:
         |  opaque type Foo = Int
         |  val x: Fo$CARET""".stripMargin,
    resultText =
      s"""object Inside:
         |  opaque type Foo = Int
         |  val x: Foo$CARET""".stripMargin,
  ) {
    hasItemText(_, "Foo")(typeText = "Int", itemTextBold = true)
  }

  def testRenderingOutside(): Unit = doRawCompletionTest(
    fileText =
      s"""object Inside:
         |  opaque type Foo = Int
         |object Outside:
         |  val x: Inside.Fo$CARET""".stripMargin,
    resultText =
      s"""object Inside:
         |  opaque type Foo = Int
         |object Outside:
         |  val x: Inside.Foo$CARET""".stripMargin,
  ) {
    hasItemText(_, "Foo")(typeText = "", itemTextBold = true)
  }

  // TODO Use correct Context, SCL-24006
//  def testSmartInside(): Unit = doCompletionTest(
//    completionType = CompletionType.SMART,
//    fileText =
//      s"""object Inside:
//         |  opaque type Foo = Int
//         |  val foo: Foo = ???
//         |  val x: Int = fo$CARET""".stripMargin,
//    resultText =
//      s"""object Inside:
//         |  opaque type Foo = Int
//         |  val foo: Foo = ???
//         |  val x: Int = foo$CARET""".stripMargin,
//    item = "foo"
//  )

  // TODO checkNoCompletion(SMART) and checkNoSmartCompletion don't actually test the condition
  def testSmartOutside(): Unit = checkNoCompletion(
    `type` = CompletionType.SMART,
    fileText =
      s"""object Inside:
         |  opaque type Foo = Int
         |  val foo: Foo = ???
         |object Outside:
         |  val x: Int = Inside.fo$CARET""".stripMargin,
  )()
}
