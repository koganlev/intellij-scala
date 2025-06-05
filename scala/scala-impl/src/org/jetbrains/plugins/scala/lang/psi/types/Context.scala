package org.jetbrains.plugins.scala.lang.psi.types

import com.intellij.psi.{PsiDirectory, PsiElement}
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition

import scala.annotation.tailrec

trait Context {
  def isInScopeOf(opaqueTypeAlias: ScTypeAliasDefinition): Boolean
}

object Context {
  def apply(place: PsiElement): Context = new Context() {
    override def isInScopeOf(opaqueTypeAlias: ScTypeAliasDefinition): Boolean = {
      if (!opaqueTypeAlias.isOpaque)
        throw new IllegalArgumentException("Opaque type alias expected")

      containingFileOf(opaqueTypeAlias) == containingFileOf(place) &&
        place.contexts.takeWhile(!_.is[PsiDirectory]).contains(opaqueTypeAlias.getParent)
    }

    override def toString: String = place.toString
  }

  @tailrec
  private def containingFileOf(e: PsiElement): PsiElement = {
    val file = e.getContainingFile
    val fileContext = file.getContext
    if (fileContext == null) file else containingFileOf(fileContext)
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
