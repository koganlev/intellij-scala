package org.jetbrains.plugins.scala
package incremental

import incremental.Listener._
import project.ProjectExt

import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.ex.{FoldingListener, FoldingModelEx}
import com.intellij.openapi.editor.{Editor, FoldRegion}

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

    if (!incremental.Highlighting.enabledIn(editor.getProject) || !isScalaIn(editor.getVirtualFile)) return

    updaters += editor -> new Updater(editor)
    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
    editor.getFoldingModel.asInstanceOf[FoldingModelEx].addListener(foldingListener, editor.getProject.unloadAwareDisposable)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!incremental.Highlighting.enabledIn(editor.getProject) || !isScalaIn(editor.getVirtualFile)) return

    editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
    updaters -= editor
  }
}

private object Listener {
  var currentEditor: Editor = _
}