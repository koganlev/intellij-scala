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
  private val timer = new Timer(200, _ => doUpdate())
  timer.setRepeats(false)

  private var previousVisibleRange: TextRange = _

  def update(): Unit = {
    timer.restart()
  }

  private def doUpdate(): Unit = {
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
  }
}

private object Updater {
  private val ErrorStripeMarkColorKey = Key.create[Color]("error_stripe_mark_color")
}
