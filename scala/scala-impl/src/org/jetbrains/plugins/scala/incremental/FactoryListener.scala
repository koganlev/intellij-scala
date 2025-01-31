package org.jetbrains.plugins.scala.incremental

import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener, VisibleAreaEvent, VisibleAreaListener}
import com.intellij.openapi.editor.{Editor, LogicalPosition}
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.plugins.scala.incremental.FactoryListener._

import java.awt.Point

class FactoryListener extends EditorFactoryListener {
  private var updaters = Map.empty[Editor, Updater]

  private val visibleAreaListener = new VisibleAreaListener {
    override def visibleAreaChanged(e: VisibleAreaEvent): Unit = {
      val editor = e.getEditor

      val visibleRange = visibleRangeIn(editor, lookaround)
      editor.putUserData(EditorArea.VISIBLE_RANGE_KEY, visibleRange)

      EditorArea.currentEditor = editor

      updaters(editor).update()
    }
  }

  override def editorCreated(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!EditorArea.isIncrementalHighlightingEnabledIn(editor.getProject) || !isScalaIn(editor)) return

    updaters += editor -> new Updater(editor)
    editor.getScrollingModel.addVisibleAreaListener(visibleAreaListener)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {
    val editor = event.getEditor

    if (!EditorArea.isIncrementalHighlightingEnabledIn(editor.getProject) || !isScalaIn(editor)) return

    editor.getScrollingModel.removeVisibleAreaListener(visibleAreaListener)
    updaters -= editor
  }
}

private object FactoryListener {
  private def lookaround: Int = Registry.intValue("scala.incremental.highlighting.lookaround")

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

  private def isScalaIn(editor: Editor): Boolean = {
    val file = editor.getVirtualFile
    file != null && (file.getExtension == "scala" || file.getExtension == "sc" || file.getExtension == "sbt")
  }
}