package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.impl.{HighlightInfo, HighlightInfoPostFilter}
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.scala.incremental.Highlighting.enabledIn

class Filter extends HighlightInfoPostFilter {
  override def accept(highlightInfo: HighlightInfo): Boolean = {
    val editor = Listener.currentEditor

    if (editor == null) return true

    if (!enabledIn(editor.getProject)) return true

    val visibleRange = VisibleRange.in(editor)
    if (visibleRange == null) return true

    val highlightRange = TextRange.create(highlightInfo.startOffset, highlightInfo.endOffset)

    highlightRange.intersects(visibleRange)
  }
}
