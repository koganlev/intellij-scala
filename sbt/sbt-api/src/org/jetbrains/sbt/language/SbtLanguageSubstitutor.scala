//package org.jetbrains.sbt.language
//
//import com.intellij.lang.Language
//import com.intellij.openapi.module.ModuleUtilCore
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.psi.LanguageSubstitutor
//import com.intellij.util.SlowOperations
//import org.jetbrains.plugins.scala.project.ModuleExt
//import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings
//import org.jetbrains.plugins.scala.project.ModuleExt
//
//import scala.util.Using
//
//final class SbtLanguageSubstitutor extends LanguageSubstitutor {
//
//  override def getLanguage(file: VirtualFile, project: Project): Language = {
//    val module = Using.resource(SlowOperations.knownIssue("SCL-21147")) { _ =>
//      ModuleUtilCore.findModuleForFile(file, project)
//    }
//    if (module.hasScala3)
//      SbtLanguageScala3.INSTANCE
//    else
//      null
//  }
//}
