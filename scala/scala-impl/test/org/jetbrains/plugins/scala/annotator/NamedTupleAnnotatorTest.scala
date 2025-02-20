package org.jetbrains.plugins.scala.annotator

import org.jetbrains.plugins.scala.ScalaVersion

class NamedTupleAnnotatorTest extends ScalaHighlightingTestBase {
  override protected def supportedIn(version: ScalaVersion): Boolean =
    version >= ScalaVersion.Latest.Scala_3_6

  // SCL-23479
  def test_named_tuple_type_element(): Unit = assertNoErrors(
    """
      |type S = Seq[(w: Double, s: String)]
      |""".stripMargin
  )
}
