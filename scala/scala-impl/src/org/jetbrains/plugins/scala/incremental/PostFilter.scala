package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.impl.{HighlightInfo, HighlightInfoPostFilter}
import com.intellij.openapi.util.TextRange

class PostFilter extends HighlightInfoPostFilter {
  import EditorArea.editor

  override def accept(highlightInfo: HighlightInfo): Boolean = {
    if (editor == null) return true

    if (!EditorArea.isIncrementalHighlightingEnabledIn(editor.getProject)) return true

    val visibleRange = editor.getUserData(EditorArea.VISIBLE_RANGE_KEY)
    if (visibleRange == null) return true

    val highlightRange = TextRange.create(highlightInfo.startOffset, highlightInfo.endOffset)

    highlightRange.intersects(visibleRange)
  }
}
