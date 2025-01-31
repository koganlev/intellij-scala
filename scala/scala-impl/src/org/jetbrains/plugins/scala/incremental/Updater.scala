package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.scala.incremental.Updater._

import java.awt.Color
import javax.swing.Timer

private class Updater(editor: Editor) {
  private val updateTimer = {
    val timer = new Timer(200, _ => doUpdate())
    timer.setRepeats(false)
    timer
  }

  private var previousVisibleRange: TextRange = _

  def update(visibleRange: TextRange, delta: Boolean): Unit = {
    editor.putUserData(EditorArea.VISIBLE_RANGE_KEY, visibleRange)

    if (!delta) {
      previousVisibleRange = null
    }

    updateTimer.restart()
  }

  private def doUpdate(): Unit = {
    val visibleRange = editor.getUserData(EditorArea.VISIBLE_RANGE_KEY)

    concealErrorStripeMarksOutside(visibleRange, editor)
    revealErrorStripeMarksInside(visibleRange, editor)

    val newlyVisibleRange = if (previousVisibleRange == null) visibleRange else visibleRange.diff(previousVisibleRange)

    val document = PsiManager.getInstance(editor.getProject).findFile(editor.getVirtualFile).getViewProvider.getDocument
    val daemon = DaemonCodeAnalyzer.getInstance(editor.getProject)
    daemon.combineDirtyScopes(document, newlyVisibleRange)
    daemon.stopProcess(/* restart = */ true)

    previousVisibleRange = visibleRange
  }
}

private object Updater {
  private val ERROR_STRIPE_MARK_COLOR_KEY = Key.create[Color]("error_stripe_mark_color")

  private def concealErrorStripeMarksOutside(visibleRange: TextRange, editor: Editor) = {
    val markupModel = editor.asInstanceOf[EditorEx].getFilteredDocumentMarkupModel

    markupModel.processRangeHighlightersOutside(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => {
      val actualColor = highlighter.getErrorStripeMarkColor(editor.getColorsScheme)
      if (!highlighter.isThinErrorStripeMark && actualColor != null) {
        highlighter.putUserData(ERROR_STRIPE_MARK_COLOR_KEY, actualColor)
        highlighter.setErrorStripeMarkColor(null)
      }
      true
    })
  }

  private def revealErrorStripeMarksInside(visibleRange: TextRange, editor: Editor) = {
    val markupModel = editor.asInstanceOf[EditorEx].getFilteredDocumentMarkupModel

    markupModel.processRangeHighlightersOverlappingWith(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => {
      val savedColor = highlighter.getUserData(ERROR_STRIPE_MARK_COLOR_KEY)
      if (savedColor != null) {
        highlighter.setErrorStripeMarkColor(savedColor)
        highlighter.putUserData(ERROR_STRIPE_MARK_COLOR_KEY, null)
      }
      true
    })
  }
}
