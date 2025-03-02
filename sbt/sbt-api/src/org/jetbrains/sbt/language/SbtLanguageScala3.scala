package org.jetbrains.sbt.language

import com.intellij.lang.{DependentLanguage, Language}
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.plugins.scala.ScalaLanguage

final class SbtLanguageScala3 private
  extends Language(ScalaLanguage.INSTANCE, "sbt (scala 3)")
    with DependentLanguage {

  override def getAssociatedFileType: LanguageFileType = SbtFileType
}

object SbtLanguageScala3 {
  val INSTANCE = new SbtLanguageScala3
}
