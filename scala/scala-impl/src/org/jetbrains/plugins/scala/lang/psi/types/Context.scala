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
    override def isInScopeOf(alias: ScTypeAlias): Boolean =
      alias.getContainingFile == place.getContainingFile && place.parentsInFile.contains(alias.getParent)
  }

  implicit object Empty extends Context {
    override def isInScopeOf(alias: ScTypeAlias): Boolean = true
  }

  class TrackingContext(implicit context: Context) extends Context {
    var locations: Map[ScTypeAlias, Boolean] = SeqMap.empty

    override def isInScopeOf(alias: ScTypeAlias): Boolean = {
      val isInScope = context.isInScopeOf(alias)
      locations += alias -> isInScope
      isInScope
    }
  }
}
