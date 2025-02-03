package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.{HighlighterLayer, HighlighterTargetArea}
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiElement
import com.intellij.ui.{Gray, JBColor}
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.startup.ProjectActivity

import java.util
import scala.jdk.CollectionConverters.CollectionHasAsScala

object Tracing {
  class StartupActivity extends ProjectActivity {
    override def execute(project: Project): Unit = {
      val connection = project.getMessageBus.connect(project.unloadAwareDisposable)
      connection.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new HighlightingListener(project))
    }
  }

  private val TRACING_HIGHLIGHTER_KEY = Key.create[AnyRef]("tracing_highlighter_key")

  private def isHighlightingTracingEnabled: Boolean = Registry.is("scala.highlighting.tracing")

  private class HighlightingListener(project: Project) extends DaemonListener {
    private var startInstants = Map.empty[FileEditor, Long]
    private var durations = Seq.empty[Long]

    override def daemonStarting(fileEditors: util.Collection[_ <: FileEditor]): Unit = if (isHighlightingTracingEnabled) {
      startInstants ++= fileEditors.asScala.map(editor => editor -> System.nanoTime())
      durations = Seq.empty
      statusBar.setInfo("Highlighting...")
    }

    override def daemonFinished(fileEditors: util.Collection[_ <: FileEditor]): Unit = if (isHighlightingTracingEnabled) {
      val editors = fileEditors.asScala
      val ds = editors.map(editor => (System.nanoTime() - startInstants(editor)) / 1000000)
      statusBar.setInfo("Highlighted: " + (durations ++ ds).map(x => s"$x ms").mkString(", "))
      startInstants --= editors.toSet
      durations ++= ds
    }

    private def statusBar = WindowManager.getInstance.getStatusBar(project)
  }

  def trace(e: PsiElement, reason: String): Unit = if (isHighlightingTracingEnabled) {
    VisibleRange.editorsFor(e).foreach { editor =>
      val text = reason + ": " + {
        val s = e.getText.replace('\n', '↵').replaceAll(" {2,}", " ")
        if (s.length > 120) s.substring(0, 120) + "…" else s
      }

      val highlighter = editor.getMarkupModel.addRangeHighlighter(
        e.getTextRange.getStartOffset, e.getTextRange.getEndOffset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.EXACT_RANGE)

      highlighter.setErrorStripeMarkColor(new JBColor(Gray._170, Gray._80))
      highlighter.setThinErrorStripeMark(true)
      highlighter.setErrorStripeTooltip(text)
      highlighter.putUserData(TRACING_HIGHLIGHTER_KEY, "")

//      println(text)
    }
  }

  def clean(): Unit = if (isHighlightingTracingEnabled) {
    EditorFactory.getInstance.getAllEditors.foreach { editor =>
      editor.getMarkupModel.getAllHighlighters.filter(_.getUserData(TRACING_HIGHLIGHTER_KEY) != null).foreach(_.dispose())
    }
  }
}
