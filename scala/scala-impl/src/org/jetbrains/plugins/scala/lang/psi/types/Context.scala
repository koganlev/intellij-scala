package org.jetbrains.plugins.scala.lang.psi.types

import com.intellij.psi.{PsiDirectory, PsiElement}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition

trait Context {
  def isInScopeOf(opaqueTypeAlias: ScTypeAliasDefinition): Boolean
}

object Context {
  def apply(place: PsiElement): Context = new Context() {
    override def isInScopeOf(opaqueTypeAlias: ScTypeAliasDefinition): Boolean = {
      if (!opaqueTypeAlias.isOpaque)
        throw new IllegalArgumentException("Opaque type alias expected")

      opaqueTypeAlias.getContainingFile == place.getContainingFile &&
        place.parents.takeWhile(!_.is[PsiDirectory]).contains(opaqueTypeAlias.getParent)
    }

    override def toString: String = place.toString
  }

  object Empty extends Context {
    override def isInScopeOf(opaqueTypeAlias: ScTypeAliasDefinition): Boolean = true

    override def toString: String = "<empty>"
  }

//  @deprecated("Provide Context(element) or use EmptyContext")
  implicit val Default: Context = new Context {
    override def isInScopeOf(opaqueTypeAlias: ScTypeAliasDefinition): Boolean = true

    override def toString: String = "<default>"
  }
}
