package org.jetbrains.plugins.scala.lang.psi.types

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScSimpleTypeElement
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt
import org.jetbrains.plugins.scala.settings.ScalaApplicationSettings.{getInstance => ScalaApplicationSettings}
import org.jetbrains.plugins.scala.util.ScEquivalenceUtil

trait TypePresentationContext {
  def nameResolvesTo(name: String, target: PsiElement): Boolean
  def compoundTypeWithAndToken: Boolean
  // None means we don't know whether we are in Scala2 or Scala3, so be conservative
  def infixTypesConsiderPrecedence: Option[Boolean]

  final def compoundTypeSeparatorText: String =
    if (compoundTypeWithAndToken) {
      // SCL-21195
      if (ScalaApplicationSettings.PRECISE_TEXT) " with "
      else " & "
    } else {
      " with "
    }
}

object TypePresentationContext {
  import scala.language.implicitConversions

  def apply(place: PsiElement): TypePresentationContext = psiElementPresentationContext(place)

  class PsiBased(place: PsiElement) extends TypePresentationContext {
    override def nameResolvesTo(name: String, target: PsiElement): Boolean =
      if (place.isValid) {
        val context = place.getContext
        if (context != null) {
          val element = ScalaPsiElementFactory.createTypeElementFromText(name, context, place)
          element match {
            case ScSimpleTypeElement(ResolvesTo(reference)) =>
              ScEquivalenceUtil.smartEquivalence(reference, target)(Context(context))
            case _ => false
          }
        } else true
      }
      else true //let's just show short version for invalid elements

    override lazy val compoundTypeWithAndToken: Boolean = place.containingFile.exists(_.isScala3OrSource3Enabled)
    override lazy val infixTypesConsiderPrecedence: Option[Boolean] = place.containingFile.map(_.isInScala3File)
  }

  implicit def psiElementPresentationContext(place: PsiElement): TypePresentationContext = new PsiBased(place)

  val emptyContext: TypePresentationContext = new TypePresentationContext {
    override def nameResolvesTo(name: String, target: PsiElement): Boolean = false
    override def compoundTypeWithAndToken: Boolean = false
    override def infixTypesConsiderPrecedence: Option[Boolean] = None
  }

  private val scala2EmptyContext: TypePresentationContext = new TypePresentationContext {
    override def nameResolvesTo(name: String, target: PsiElement): Boolean = false
    override def compoundTypeWithAndToken: Boolean = false
    override def infixTypesConsiderPrecedence: Option[Boolean] = Some(false)
  }

  private val scala3EmptyContext: TypePresentationContext = new TypePresentationContext {
    override def nameResolvesTo(name: String, target: PsiElement): Boolean = false
    override def compoundTypeWithAndToken: Boolean = true
    override def infixTypesConsiderPrecedence: Option[Boolean] = Some(true)
  }

  def emptyContextIn(scala3: Boolean): TypePresentationContext =
    if (scala3) scala3EmptyContext else scala2EmptyContext
}
