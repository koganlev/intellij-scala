package org.jetbrains.plugins.scala
package incremental

import project.ProjectExt

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.ex.{FoldingListener, FoldingModelEx}
import com.intellij.openapi.editor.{Editor, FoldRegion}

import java.awt.event.{KeyAdapter, KeyEvent}

class Listener extends EditorFactoryListener {
  private val MaxDoubleKeyPressDuration = 500L * 1000000L // ns (0.5 s)

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
    Highlighting.editor = editor
    VisibleRange.saveIn(editor)
    updaters(editor).scheduleUpdate(delta)
  }

  private var previousKeyPressInstant = 0L

  private val keyListener = new KeyAdapter() {
    override def keyPressed(e: KeyEvent): Unit = {
      if (e.getKeyCode == KeyEvent.VK_ESCAPE) {
        if (System.nanoTime() - previousKeyPressInstant < MaxDoubleKeyPressDuration) {
          Highlighting.suppress = true
          DaemonCodeAnalyzer.getInstance(Highlighting.editor.getProject).restart()
        }
        previousKeyPressInstant = System.nanoTime()
      }
    }
  }

  override def editorCreated(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!incremental.Highlighting.enabledIn(editor.getProject) || !isScalaIn(editor.getVirtualFile)) return

    updaters += editor -> new Updater(editor)
    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
    editor.getFoldingModel.asInstanceOf[FoldingModelEx].addListener(foldingListener, editor.getProject.unloadAwareDisposable)
    editor.getContentComponent.addKeyListener(keyListener)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!incremental.Highlighting.enabledIn(editor.getProject) || !isScalaIn(editor.getVirtualFile)) return

    updaters -= editor
    editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
    editor.getContentComponent.removeKeyListener(keyListener)
  }
}
