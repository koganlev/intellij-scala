package org.jetbrains.plugins.scala
package incremental

import settings.ScalaProjectSettings

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

// SCL-23216
object Highlighting {
  def enabledIn(project: Project): Boolean =
    project != null && ScalaProjectSettings.in(project).isIncrementalHighlighting

  implicit class ElementHighlightingExt(private val e: PsiElement) extends AnyVal {
    def isVisible: Boolean = VisibleRange.isVisible(e)
  }
}
