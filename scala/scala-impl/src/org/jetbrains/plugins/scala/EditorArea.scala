package org.jetbrains.plugins.scala

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener
import com.intellij.codeInsight.daemon.impl.{DaemonCodeAnalyzerEx, DaemonCodeAnalyzerImpl, FileStatusMap, HighlightInfo, HighlightInfoPostFilter}
import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.editor.{Document, Editor, EditorFactory, LogicalPosition}
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.{Key, TextRange}
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.{PsiDocumentManager, PsiElement, PsiManager}
import com.intellij.ui.{Gray, JBColor}
import org.jetbrains.plugins.scala.EditorArea._
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.settings.ScalaProjectSettings
import org.jetbrains.plugins.scala.startup.ProjectActivity

import java.awt.{Color, Point}
import java.util
import javax.swing.Timer

class EditorArea extends EditorFactoryListener with ProjectActivity with HighlightInfoPostFilter {
  private var previousVisibleRange: TextRange = _

  override def accept(highlightInfo: HighlightInfo): Boolean = {
    if (editor == null) return true

    if (!isIncrementalHighlightingEnabledIn(editor.getProject)) return true

    val visibleRange = editor.getUserData(VISIBLE_RANGE_KEY)
    if (visibleRange == null) return true

    val highlightRange = TextRange.create(highlightInfo.startOffset, highlightInfo.endOffset)

    highlightRange.intersects(visibleRange)
  }

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

  override def execute(project: Project): Unit = {
    val connection = project.getMessageBus.connect(project.unloadAwareDisposable)
    connection.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new HighlightingListener(project))
  }
}

object EditorArea {
  private var editor: Editor = _

  private val VISIBLE_RANGE_KEY: Key[TextRange] = Key.create[TextRange]("editor_visible_range")

  private val ErrorStripeMarkColorKey = Key.create[Color]("error_stripe_mark_color")

  def isNativeHighlightingEnabled: Boolean = Registry.is("scala.native.highlighting")

  private def isNativeHighlightingSynchronized: Boolean = Registry.is("scala.native.highlighting.synchronized")

  private def isNativeHighlightingTracingEnabled: Boolean = Registry.is("scala.native.highlighting.tracing")

  def isIncrementalHighlightingEnabledIn(project: Project): Boolean = project != null && ScalaProjectSettings.in(project).isIncrementalHighlighting

  private def incrementalHighlightingLookaround: Int = Registry.intValue("scala.incremental.highlighting.lookaround")

  def isVisible(e: PsiElement): Boolean = {
    if (!isNativeHighlightingEnabled) return false

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
