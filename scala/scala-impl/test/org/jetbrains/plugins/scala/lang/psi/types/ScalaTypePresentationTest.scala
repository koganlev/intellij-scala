package org.jetbrains.plugins.scala.lang.psi.types

import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestCase
import org.jetbrains.plugins.scala.extensions.{IterableOnceExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.project.ScalaFeatures
import org.jetbrains.plugins.scala.{LatestScalaVersions, ScalaVersion}
import org.junit.Assert._

class ScalaTypePresentationTest extends ScalaLightCodeInsightFixtureTestCase {
  override protected def supportedIn(version: ScalaVersion): Boolean = version >= LatestScalaVersions.Scala_3_0

  override protected def additionalCompilerOptions = Seq("-Ykind-projector")

  def testPolymorphicFunction(): Unit = assertPresentationIs(
    "[X] => Any => Nothing")

  def testLambda(): Unit = assertPresentationIs(
    "[X] =>> Any")

  def testMatchSingleCase(): Unit = assertPresentationIs(
    "A match { case Int => Char }")

  def testMatchMultipleCases(): Unit = assertPresentationIs(
    "A match { case Int => Char; case Long => String }")

  def testMatchrParenthesesInner(): Unit = {
    assertPresentationIs(
      "(A match { case Int => Char }) match { case Long => String }")

    assertPresentationIs(
      "(A => Any) match { case Int => Char }")
  }

  def testMatchrParenthesesOuter(): Unit = {
    assertPresentationIs(
      "(A match { case Int => Char }) => Any")

    assertPresentationIs(
      "[X] => (A match { case Int => Char }) => Any")
  }

  def testKindProjector(): Unit = assertPresentationIs(
    header =   "class HCT[A[X]]; class TC2[A, B]; ",
    tpe =      "HCT[TC2[Int, *]]",
    expected = "HCT[TC2[Int, *]]"
  )

  def testKindProjectorTuple(): Unit = assertPresentationIs(
    header =   "class HCT[A[X]]; class TC2[A, B]; ",
    tpe =      "HCT[(Int, *)]",
    expected = "HCT[(Int, *)]"
  )

  def testKindProjectorFunction(): Unit = assertPresentationIs(
    header =   "class HCT[A[X]]; class TC2[A, B]; ",
    tpe =      "HCT[Int => *]",
    expected = "HCT[Int => *]"
  )

  def testOpaqueTypeInt(): Unit = {
    assertPresentationIs(
      "Inside.T", "Inside.T", "object Inside { opaque type T = Int }")
  }

  def testOpaqueTypeTuple(): Unit = {
    assertPresentationIs(
      "Inside.T", "Inside.T", "object Inside { opaque type T = (Int, Int) }")
  }

  private def assertPresentationIs(tpe: String): Unit = assertPresentationIs(tpe, tpe)

  private def assertPresentationIs(tpe: String, expected: String, header: String = ""): Unit = {
    val file =
      ScalaPsiElementFactory.createScalaFileFromText(header + "type T[A] = " + tpe, ScalaFeatures.onlyByVersion(version))(
        getProject
      )

    val typeElement = file.elements.collectFirst { case e: ScTypeElement if e.getTextOffset > header.length => e }.get
    val actual = typeElement.`type`().get.presentableText(TypePresentationContext(typeElement), Context(typeElement))
    assertEquals(expected, actual)
  }
}
