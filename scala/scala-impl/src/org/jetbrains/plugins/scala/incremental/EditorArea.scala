package org.jetbrains.plugins.scala.incremental

import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.psi.{PsiDocumentManager, PsiElement}
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

object EditorArea {
  private[incremental] var editor: Editor = _

  private[incremental] val VISIBLE_RANGE_KEY = Key.create[TextRange]("editor_visible_range")

  def isIncrementalHighlightingEnabledIn(project: Project): Boolean = project != null && ScalaProjectSettings.in(project).isIncrementalHighlighting

  def isVisible(e: PsiElement): Boolean = {
    if (!isIncrementalHighlightingEnabledIn(e.getProject)) return true

    val editor = editorFor(e)
    if (editor == null) return false

    val elementRange = e.getTextRange

    val visibleRange = editor.getUserData(VISIBLE_RANGE_KEY)
    if (visibleRange == null) return true // Not yet computed (a safeguard, shouldn't normally happen)
    if (!elementRange.intersects(visibleRange)) return false

    !isFolded(editor, elementRange)
  }

  private def isFolded(editor: Editor, range: TextRange): Boolean = {
    val foldingModel = editor.getFoldingModel
    val region1 = foldingModel.getCollapsedRegionAtOffset(range.getStartOffset)
    if (region1 == null) return false
    val region2 = foldingModel.getCollapsedRegionAtOffset(range.getEndOffset - 1)
    region1 == region2
  }

  private[incremental] def editorFor(e: PsiElement): Editor = {
    val psiFile = e.getContainingFile
    if (psiFile == null) return null

    val document = PsiDocumentManager.getInstance(e.getProject).getDocument(psiFile)
    if (document == null) return null

    val editors = EditorFactory.getInstance.getEditors(document)
    if (editors.isEmpty) return null

    editors.head
  }
}
