package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.{DaemonCodeAnalyzerEx, DaemonCodeAnalyzerImpl, FileStatusMap}
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.scala.incremental.EditorArea.{ErrorStripeMarkColorKey, VISIBLE_RANGE_KEY, incrementalHighlightingLookaround, isIncrementalHighlightingEnabledIn, visibleRangeIn}

import javax.swing.Timer

class FactoryListener extends EditorFactoryListener {
  import EditorArea.editor

  private var previousVisibleRange: TextRange = _

  private val timer = new Timer(200, _ => {
    val visibleRange = editor.getUserData(VISIBLE_RANGE_KEY)

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
    val daemon = DaemonCodeAnalyzer.getInstance(editor.getProject).asInstanceOf[DaemonCodeAnalyzerEx]
    combineDirtyScopesMethod.invoke(daemon.getFileStatusMap, document, visibleRangeDelta, "Incremental highlighting")
    stopProcessMethod.invoke(daemon, true, "Incremental highlighting")

    previousVisibleRange = visibleRange
  })

  timer.setRepeats(false)

  private val visibleAreaListener = new VisibleAreaListener {
    override def visibleAreaChanged(e: VisibleAreaEvent): Unit = {
      editor = e.getEditor
      val visibleRange = visibleRangeIn(editor, incrementalHighlightingLookaround)
      editor.putUserData(VISIBLE_RANGE_KEY, visibleRange)
      timer.restart()
    }
  }

  private lazy val combineDirtyScopesMethod = {
    val m = classOf[FileStatusMap].getDeclaredMethod("combineDirtyScopes", classOf[Document], classOf[TextRange], classOf[Object])
    m.setAccessible(true)
    m
  }

  private lazy val stopProcessMethod = {
    val m = classOf[DaemonCodeAnalyzerImpl].getDeclaredMethod("stopProcess", classOf[Boolean], classOf[String])
    m.setAccessible(true)
    m
  }

  override def editorCreated(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!isIncrementalHighlightingEnabledIn(editor.getProject)) return

    val file = editor.getVirtualFile
    if (file == null || file.getExtension != "scala" && file.getExtension != "sc" && file.getExtension != "sbt") return

    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!isIncrementalHighlightingEnabledIn(editor.getProject)) return

    editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
  }
}
