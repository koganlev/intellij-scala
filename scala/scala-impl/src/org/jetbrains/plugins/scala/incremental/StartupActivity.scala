package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import org.jetbrains.plugins.scala.incremental.StartupActivity._
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.startup.ProjectActivity

import java.util

class StartupActivity extends ProjectActivity {
  override def execute(project: Project): Unit = {
    val connection = project.getMessageBus.connect(project.unloadAwareDisposable)
    connection.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new HighlightingListener(project))
  }
}

object StartupActivity {
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
