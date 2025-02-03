package org.jetbrains.plugins.scala.incremental

import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.ex.{FoldingListener, FoldingModelEx}
import com.intellij.openapi.editor.{Editor, FoldRegion}
import org.jetbrains.plugins.scala.incremental.Highlighting.enabledIn
import org.jetbrains.plugins.scala.incremental.Listener._
import org.jetbrains.plugins.scala.project.ProjectExt

class Listener extends EditorFactoryListener {
  private var updaters = Map.empty[Editor, Updater]

  private val visibleAreaListener = new VisibleAreaListener {
    override def visibleAreaChanged(e: VisibleAreaEvent): Unit = {
      handleEvent(e.getEditor, delta = true)
    }
  }

  private val foldingListener = new FoldingListener {
    override def onFoldRegionStateChange(r: FoldRegion): Unit = {
      handleEvent(r.getEditor, delta = false)
    }
  }

  private def handleEvent(editor: Editor, delta: Boolean): Unit = {
    currentEditor = editor
    VisibleRange.saveIn(editor)
    updaters(editor).scheduleUpdate(delta)
  }

  override def editorCreated(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!enabledIn(editor.getProject) || !isScalaIn(editor)) return

    updaters += editor -> new Updater(editor)
    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
    editor.getFoldingModel.asInstanceOf[FoldingModelEx].addListener(foldingListener, editor.getProject.unloadAwareDisposable)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!enabledIn(editor.getProject) || !isScalaIn(editor)) return

    editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
    updaters -= editor
  }
}

private object Listener {
  var currentEditor: Editor = _

  private def isScalaIn(editor: Editor): Boolean = {
    val file = editor.getVirtualFile
    file != null && (file.getExtension == "scala" || file.getExtension == "sc" || file.getExtension == "sbt")
  }
}