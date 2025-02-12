package org.jetbrains.plugins.scala.lang.psi.implicits

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{PsiElement, PsiNamedElement, ResolveState}
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.isImplicit
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClause
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveState.ResolveStateExt
import org.jetbrains.plugins.scala.project.ProjectPsiElementExt

private[implicits] final class ImplicitParametersProcessor(override protected val getPlace: PsiElement,
                                                           override protected val withoutPrecedence: Boolean)
  extends ImplicitProcessor(getPlace, withoutPrecedence) {

  override protected def execute(
    namedElement: PsiNamedElement
  )(implicit
    state: ResolveState
  ): Boolean = {
    val isDeclaredOrExportedInExtension =
      ImplicitProcessor.isDeclaredOrExportedInExtension(namedElement, state)

    if ((isImplicit(namedElement) || isDeclaredOrExportedInExtension) && isAccessible(namedElement)) {
      addResult(
        new ScalaResolveResult(
          namedElement,
          state.substitutorWithThisType,
          renamed             = state.renamed,
          importsUsed         = state.importsUsed,
          implicitScopeObject = state.implicitScopeObject,
          isExtensionCall     = isDeclaredOrExportedInExtension,
          exportedIn          = state.exportedIn
        )
      )
    }

    true
  }

  override def candidatesS: Set[ScalaResolveResult] =
    super.candidatesS.filterNot { c =>
      !c.isExtensionCall && isContextAncestor(c)
    }

  private def isAccessible(namedElement: PsiNamedElement): Boolean =
    isPredefPriority || ImplicitProcessor.isAccessible(namedElement, getPlace)

  private def isContextAncestor(c: ScalaResolveResult): Boolean = {
    val nameContext = c.element.nameContext
    nameContext match {
      case _: ScCaseClause => !getPlace.betterMonadicForEnabled && !getPlace.isInScala3File
      case _               => PsiTreeUtil.isContextAncestor(nameContext, getPlace, false)
    }
  }
}
