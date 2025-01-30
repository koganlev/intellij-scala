package org.jetbrains.plugins.scala.incremental

import com.intellij.codeInsight.daemon.impl.{HighlightInfo, HighlightInfoPostFilter}
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.scala.incremental.EditorArea.{VISIBLE_RANGE_KEY, isIncrementalHighlightingEnabledIn}

class PostFilter extends HighlightInfoPostFilter {
  import EditorArea.editor

  override def accept(highlightInfo: HighlightInfo): Boolean = {
    if (editor == null) return true

    if (!isIncrementalHighlightingEnabledIn(editor.getProject)) return true

    val visibleRange = editor.getUserData(VISIBLE_RANGE_KEY)
    if (visibleRange == null) return true

    val highlightRange = TextRange.create(highlightInfo.startOffset, highlightInfo.endOffset)

    highlightRange.intersects(visibleRange)
  }
}
