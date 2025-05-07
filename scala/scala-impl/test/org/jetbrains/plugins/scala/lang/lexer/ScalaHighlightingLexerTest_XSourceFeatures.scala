package org.jetbrains.plugins.scala.lang.lexer

import com.intellij.lexer.Lexer
import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.plugins.scala.highlighter.ScalaSyntaxHighlighterFactory
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerSettingsProfile
import org.jetbrains.plugins.scala.project.{ModuleExt, ScalaFeaturePusher, UserDataKeys}
import org.jetbrains.plugins.scala.{ScalaFileType, ScalaVersion}

import java.nio.file.Path

class ScalaHighlightingLexerTest_XSourceFeatures extends ScalaLexerTestBase {
  override protected def relativeTestDataPath: Path = Path.of("lexer", "highlighting_XSourceFeatures")

  override protected def createLexer: Lexer = {
    val virtualFile = new LightVirtualFile("dummy.scala", ScalaFileType.INSTANCE, "")

    val module = ModuleManager.getInstance(project).getModules()(0)
    addCompilerOptions(module, Seq("-Xsource:3", "-Xsource-features:unicode-escapes-raw"))
    module.putUserData(UserDataKeys.LightTestScalaVersion, ScalaVersion.Latest.Scala_2_13)

    ScalaFeaturePusher.setFeatures(virtualFile, module.features)

    val scalaSyntaxHighlighter = ScalaSyntaxHighlighterFactory.createScalaSyntaxHighlighter(project, virtualFile, language)
    scalaSyntaxHighlighter.getHighlightingLexer
  }

  protected def addCompilerOptions(module: Module, additionalCompilerOptions: Seq[String]): Unit = {
    val profile = ScalaCompilerSettingsProfile.forModule(module)
    val newSettings = profile.getSettings.copy(additionalCompilerOptions = additionalCompilerOptions)
    profile.setSettings(newSettings)
  }
}
