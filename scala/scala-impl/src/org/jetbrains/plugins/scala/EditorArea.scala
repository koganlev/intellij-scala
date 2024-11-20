package org.jetbrains.plugins.scala

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.{Editor, EditorFactory, LogicalPosition}
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.psi.{PsiDocumentManager, PsiElement, PsiManager}
import org.jetbrains.plugins.scala.EditorArea._
import org.jetbrains.plugins.scala.extensions.invokeLater

import java.awt.Point

class EditorArea extends EditorFactoryListener {
  private val visibleAreaListener = new VisibleAreaListener {
    override def visibleAreaChanged(e: VisibleAreaEvent): Unit = {
      val editor = e.getEditor

      val visibleRange = visibleRangeIn(editor, incrementalHighlightingLookaround)
      editor.putUserData(VISIBLE_RANGE_KEY, visibleRange)

      val psiFile = PsiManager.getInstance(editor.getProject).findFile(editor.getVirtualFile)
      DaemonCodeAnalyzer.getInstance(editor.getProject).restart(psiFile)
    }
  }

  override def editorCreated(event: EditorFactoryEvent): Unit = {
    if (!isIncrementalHighlightingEnabled) return

    val editor = event.getEditor
    val file = editor.getVirtualFile
    if (file == null || file.getExtension != "scala" && file.getExtension != "sc" && file.getExtension != "sbt") return

    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    if (!isIncrementalHighlightingEnabled) return

    event.getEditor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
  }
}

object EditorArea {
  private val VISIBLE_RANGE_KEY: Key[TextRange] = Key.create[TextRange]("editor_visible_range")

  def isNativeHighlightingEnabled: Boolean = Registry.is("scala.native.highlighting")

  private def nativeHighlightingTracing: Boolean = Registry.is("scala.native.highlighting.tracing")

  def isIncrementalHighlightingEnabled: Boolean = Registry.is("scala.incremental.highlighting")

  private def incrementalHighlightingLookaround: Int = Registry.intValue("scala.incremental.highlighting.lookaround")

  def isVisible(e: PsiElement): Boolean = {
    if (!isNativeHighlightingEnabled) return false

    if (!isIncrementalHighlightingEnabled) return true

    val editor = editorFor(e)
    if (editor == null) return false

    val elementRange = e.getTextRange

    val visibleRange = editor.getUserData(VISIBLE_RANGE_KEY)
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

  private def visibleRangeIn(editor: Editor, lookaround: Int): TextRange = {
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

  def trace(e: PsiElement, reason: String): Unit = if (nativeHighlightingTracing) {
    val editor = editorFor(e)
    if (editor == null) return

    val text = {
      val s = e.getText.replace('\n', '↵').replaceAll(" {2,}", " ")
      if (s.length > 120) s.substring(0, 120) + "…" else s
    }

    invokeLater {
      val line = editor.offsetToLogicalPosition(e.getTextOffset).line + 1

      println(line.toString + ": " + reason + ": " + text)
    }
  }
}
