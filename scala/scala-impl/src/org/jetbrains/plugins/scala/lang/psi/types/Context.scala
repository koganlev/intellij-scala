package org.jetbrains.plugins.scala.lang.psi.types

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScTypeAlias, ScTypeAliasDefinition}

import scala.collection.immutable.SeqMap

trait Context {
  def isInScopeOf(alias: ScTypeAlias): Boolean
}

object Context {
  def apply(place: PsiElement): Context = new Context() {
    override def isInScopeOf(alias: ScTypeAlias): Boolean = alias match {
      case a: ScTypeAliasDefinition if a.isOpaque =>
        a.getContainingFile == place.getContainingFile && place.parentsInFile.contains(a.getParent)
      case _ =>
        throw new IllegalArgumentException("Opaque type alias expected")
    }
  }

  implicit object Empty extends Context {
    override def isInScopeOf(alias: ScTypeAlias): Boolean = true
  }

  type Location = Map[ScTypeAlias, Boolean]

  class TrackingContext(implicit context: Context) extends Context {
    var usedOpaqueTypeAliases: Map[ScTypeAlias, Boolean] = SeqMap.empty

    override def isInScopeOf(alias: ScTypeAlias): Boolean = {
      val isInScope = context.isInScopeOf(alias)
      usedOpaqueTypeAliases += alias -> isInScope
      isInScope
    }
  }
}
