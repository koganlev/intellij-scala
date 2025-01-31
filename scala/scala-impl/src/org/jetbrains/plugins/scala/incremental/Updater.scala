package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.{EditorEx, MarkupModelEx}
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

    val markupModel = editor.asInstanceOf[EditorEx].getFilteredDocumentMarkupModel
    concealMarksOutside(visibleRange, markupModel)
    revealMarksInside(visibleRange, markupModel)

    val newlyVisibleRange = if (previousVisibleRange == null) visibleRange else visibleRange.diff(previousVisibleRange)

    val document = PsiManager.getInstance(editor.getProject).findFile(editor.getVirtualFile).getViewProvider.getDocument
    val daemon = DaemonCodeAnalyzer.getInstance(editor.getProject)
    daemon.combineDirtyScopes(document, newlyVisibleRange)
    daemon.stopProcess(true)

    previousVisibleRange = visibleRange
  }

  private def revealMarksInside(visibleRange: TextRange, markupModel: MarkupModelEx) = {
    markupModel.processRangeHighlightersOverlappingWith(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => {
      val savedColor = highlighter.getUserData(ErrorStripeMarkColorKey)
      if (savedColor != null) {
        highlighter.setErrorStripeMarkColor(savedColor)
        highlighter.putUserData(ErrorStripeMarkColorKey, null)
      }
      true
    })
  }

  private def concealMarksOutside(visibleRange: TextRange, markupModel: MarkupModelEx) = {
    markupModel.processRangeHighlightersOutside(visibleRange.getStartOffset, visibleRange.getEndOffset, highlighter => {
      val actualColor = highlighter.getErrorStripeMarkColor(editor.getColorsScheme)
      if (!highlighter.isThinErrorStripeMark && actualColor != null) {
        highlighter.putUserData(ErrorStripeMarkColorKey, actualColor)
        highlighter.setErrorStripeMarkColor(null)
      }
      true
    })
  }
}

private object Updater {
  private val ErrorStripeMarkColorKey = Key.create[Color]("error_stripe_mark_color")
}
