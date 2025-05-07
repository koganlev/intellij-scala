package org.jetbrains.plugins.scala.lang.actions.editor.enter

import com.intellij.codeInsight.CodeInsightSettings

import java.nio.file.Path

class EnterActionTest extends AbstractEnterActionTestBase {
  override protected def relativeTestDataPath: Path = Path.of("actions", "editor", "enter", "data")

  override protected def setUp(): Unit = {
    super.setUp()
    scalaCodeStyleSettings.USE_SCALADOC2_FORMATTING = true
    CodeInsightSettings.getInstance().JAVADOC_STUB_ON_ENTER = false // No, we don't need it.
  }

  override protected def tearDown(): Unit = {
    CodeInsightSettings.getInstance().JAVADOC_STUB_ON_ENTER = true
    super.tearDown()
  }
}
