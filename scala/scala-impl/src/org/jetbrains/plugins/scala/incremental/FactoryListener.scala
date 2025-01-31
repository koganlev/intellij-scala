package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.scala.incremental.FactoryListener._

import java.awt.{Color, Point}
import javax.swing.Timer

class FactoryListener extends EditorFactoryListener {
  import EditorArea.editor

  private var previousVisibleRange: TextRange = _

  private val timer = new Timer(200, _ => {
    val visibleRange = editor.getUserData(EditorArea.VISIBLE_RANGE_KEY)

    val markupModel = editor.asInstanceOf[EditorEx].getFilteredDocumentMarkupModel
    markupModel.processRangeHighlightersOutside(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => {
      val actualColor = highlighter.getErrorStripeMarkColor(editor.getColorsScheme)
      if (!highlighter.isThinErrorStripeMark && actualColor != null) {
        highlighter.putUserData(ErrorStripeMarkColorKey, actualColor)
        highlighter.setErrorStripeMarkColor(null)
      }
      true
    })
    markupModel.processRangeHighlightersOverlappingWith(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => {
      val savedColor = highlighter.getUserData(ErrorStripeMarkColorKey)
      if (savedColor != null) {
        highlighter.setErrorStripeMarkColor(savedColor)
        highlighter.putUserData(ErrorStripeMarkColorKey, null)
      }
      true
    })

    val visibleRangeDelta: TextRange = if (previousVisibleRange == null) visibleRange else {
      // TODO optimize
      val r = Range(visibleRange.getStartOffset, visibleRange.getEndOffset).toSet.diff(Range(previousVisibleRange.getStartOffset, previousVisibleRange.getEndOffset).toSet)
      if (r.isEmpty) TextRange.EMPTY_RANGE else new TextRange(r.min, r.max + 1)
    }

    val document = PsiManager.getInstance(editor.getProject).findFile(editor.getVirtualFile).getViewProvider.getDocument
    val daemon = DaemonCodeAnalyzer.getInstance(editor.getProject)
    daemon.combineDirtyScopes(document, visibleRangeDelta)
    daemon.stopProcess(true)

    previousVisibleRange = visibleRange
  })

  timer.setRepeats(false)

  private val visibleAreaListener = new VisibleAreaListener {
    override def visibleAreaChanged(e: VisibleAreaEvent): Unit = {
      editor = e.getEditor
      val visibleRange = visibleRangeIn(editor, lookaround)
      editor.putUserData(EditorArea.VISIBLE_RANGE_KEY, visibleRange)
      timer.restart()
    }
  }

  override def editorCreated(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!EditorArea.isIncrementalHighlightingEnabledIn(editor.getProject) || !isScalaIn(editor)) return

    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!EditorArea.isIncrementalHighlightingEnabledIn(editor.getProject) || !isScalaIn(editor)) return

    editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
  }
}

object FactoryListener {
  private val ErrorStripeMarkColorKey = Key.create[Color]("error_stripe_mark_color")

  private def lookaround: Int = Registry.intValue("scala.incremental.highlighting.lookaround")

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

  private def isScalaIn(editor: Editor): Boolean = {
    val file = editor.getVirtualFile
    file != null && (file.getExtension == "scala" || file.getExtension == "sc" || file.getExtension == "sbt")
  }
}