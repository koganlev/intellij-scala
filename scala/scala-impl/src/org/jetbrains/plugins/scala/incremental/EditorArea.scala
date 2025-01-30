package org.jetbrains.plugins.scala.incremental

import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.editor.{Editor, EditorFactory, LogicalPosition}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.psi.{PsiDocumentManager, PsiElement}
import com.intellij.ui.{Gray, JBColor}
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings

import java.awt.{Color, Point}

object EditorArea {
  private[incremental] var editor: Editor = _

  private[incremental] val VISIBLE_RANGE_KEY: Key[TextRange] = Key.create[TextRange]("editor_visible_range")

  private[incremental] val ErrorStripeMarkColorKey = Key.create[Color]("error_stripe_mark_color")

  private[incremental] def isNativeHighlightingTracingEnabled: Boolean = Registry.is("scala.native.highlighting.tracing")

  def isIncrementalHighlightingEnabledIn(project: Project): Boolean = project != null && ScalaProjectSettings.in(project).isIncrementalHighlighting

  private[incremental] def incrementalHighlightingLookaround: Int = Registry.intValue("scala.incremental.highlighting.lookaround")

  def isVisible(e: PsiElement): Boolean = {
    if (!isIncrementalHighlightingEnabledIn(e.getProject)) return true

    val editor = editorFor(e)
    if (editor == null) return false

    val elementRange = e.getTextRange

    val visibleRange = editor.getUserData(VISIBLE_RANGE_KEY)
    if (visibleRange == null) return true // Not yet computed (a safeguard, shouldn't normally happen)
    if (!elementRange.intersects(visibleRange)) return false

    val foldingModel = editor.getFoldingModel
    val region1 = foldingModel.getCollapsedRegionAtOffset(elementRange.getStartOffset)
    val region2 = foldingModel.getCollapsedRegionAtOffset(elementRange.getEndOffset - 1)
    region1 == null || region1 != region2
  }

  private def editorFor(e: PsiElement): Editor = {
    val psiFile = e.getContainingFile
    if (psiFile == null) return null

    val document = PsiDocumentManager.getInstance(e.getProject).getDocument(psiFile)
    if (document == null) return null

    val editors = EditorFactory.getInstance.getEditors(document)
    if (editors.isEmpty) return null

    editors.head
  }

  private[incremental] def visibleRangeIn(editor: Editor, lookaround: Int): TextRange = {
    val visibleRectangle = editor.getScrollingModel.getVisibleArea

    val startOffset = {
      val position = editor.xyToLogicalPosition(visibleRectangle.getLocation)
      val adjustedPosition = new LogicalPosition((position.line - lookaround).max(0), 0)
      editor.logicalPositionToOffset(adjustedPosition)
    }

    val endOffset = {
      val position = editor.xyToLogicalPosition(new Point(visibleRectangle.x + visibleRectangle.width, visibleRectangle.y + visibleRectangle.height))
      val adjustedPosition = new LogicalPosition(position.line + lookaround + 1, 0)
      editor.logicalPositionToOffset(adjustedPosition)
    }

    TextRange.create(startOffset, startOffset.max(endOffset))
  }

  def trace(e: PsiElement, reason: String): Unit = if (isNativeHighlightingTracingEnabled) {
    val editor = editorFor(e)
    if (editor == null) return

    val text = reason + ": " + {
      val s = e.getText.replace('\n', '↵').replaceAll(" {2,}", " ")
      if (s.length > 120) s.substring(0, 120) + "…" else s
    }

    val highlighter = editor.getMarkupModel.addRangeHighlighter(
      e.getTextRange.getStartOffset, e.getTextRange.getEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.EXACT_RANGE)

    highlighter.setErrorStripeMarkColor(new JBColor(Gray._170, Gray._80))
    highlighter.setThinErrorStripeMark(true)
    highlighter.setErrorStripeTooltip(text)

    println(text)
  }
}
