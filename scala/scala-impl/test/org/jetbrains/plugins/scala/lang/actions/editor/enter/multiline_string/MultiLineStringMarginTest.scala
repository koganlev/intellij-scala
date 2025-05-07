package org.jetbrains.plugins.scala.lang.actions.editor.enter.multiline_string

import org.jetbrains.plugins.scala.lang.actions.editor.enter.AbstractEnterActionTestBase

import java.nio.file.Path

class MultiLineStringMarginTest extends AbstractEnterActionTestBase {
  override protected def relativeTestDataPath: Path = Path.of("actions", "editor", "enter", "multiLineStringData", "marginOnly")

  override protected def setUp(): Unit = {
    super.setUp()
    scalaCodeStyleSettings.MULTILINE_STRING_CLOSING_QUOTES_ON_NEW_LINE = false
    scalaCodeStyleSettings.MULTILINE_STRING_INSERT_MARGIN_ON_ENTER = true
    scalaCodeStyleSettings.MULTILINE_STRING_MARGIN_INDENT = 3
  }
}
