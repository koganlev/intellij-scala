package org.jetbrains.plugins.scala

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener
import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.editor.{Editor, EditorFactory, LogicalPosition}
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.{PsiDocumentManager, PsiElement, PsiManager}
import com.intellij.ui.{Gray, JBColor}
import org.jetbrains.plugins.scala.EditorArea._
import org.jetbrains.plugins.scala.project.ProjectExt

import java.awt.Point
import java.util

class EditorArea extends EditorFactoryListener with StartupActivity {
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

  override def runActivity(project: Project): Unit = {
    val connection = project.getMessageBus.connect(project.unloadAwareDisposable)
    connection.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new HighlightingListener(project))
  }
}

object EditorArea {
  private val VISIBLE_RANGE_KEY: Key[TextRange] = Key.create[TextRange]("editor_visible_range")

  def isNativeHighlightingEnabled: Boolean = Registry.is("scala.native.highlighting")

  private def isNativeHighlightingSynchronized: Boolean = Registry.is("scala.native.highlighting.synchronized")

  private def isNativeHighlightingTracingEnabled: Boolean = Registry.is("scala.native.highlighting.tracing")

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

  def synchronizedOn[T](e: PsiElement)(f: => T): T = {
    if (isNativeHighlightingSynchronized) {
      e.synchronized {
        f
      }
    } else {
      f
    }
  }

  private class HighlightingListener(project: Project) extends DaemonListener {
    private var startTime = 0L

    override def daemonStarting(fileEditors: util.Collection[_ <: FileEditor]): Unit = if (EditorArea.isNativeHighlightingTracingEnabled) {
      startTime = System.nanoTime()
      statusBar.setInfo("Highlighting...")
    }

    override def daemonFinished(fileEditors: util.Collection[_ <: FileEditor]): Unit = if (EditorArea.isNativeHighlightingTracingEnabled) {
      statusBar.setInfo("Highlighted: " + (System.nanoTime() - startTime) / 1000000 + " ms")
    }

    private def statusBar = WindowManager.getInstance.getStatusBar(project)
  }
}
