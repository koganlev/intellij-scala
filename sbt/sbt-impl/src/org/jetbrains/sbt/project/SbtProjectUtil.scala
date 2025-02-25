package org.jetbrains.sbt.project

import com.intellij.openapi.module.{Module, ModuleUtilCore}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.caches.cachedInUserData
import org.jetbrains.plugins.scala.extensions.inReadAction
import org.jetbrains.plugins.scala.project.ProjectPsiFileExt
import org.jetbrains.sbt.SbtUtil

import java.nio.file.Path

/**
 * Utility methods for recognizing if a file or module is a part of an sbt project
 * (imported using the native sbt build tool support).
 */
object SbtProjectUtil {

  def isInSbtProject(file: PsiFile): Boolean = {
    cachedModuleForPsiFile(file) match {
      case Some(m) => isInSbtProject(m)
      case None => false
    }
  }

  def isInSbtProject(module: Module): Boolean =
    isSbtModule(module) || !isHandledByOtherBuildTool(module) && couldBeImportedAsSbtProject(module.getProject)

  private def isSbtModule(module: Module): Boolean = SbtUtil.isSbtModule(module)

  private def isHandledByOtherBuildTool(module: Module): Boolean =
    BuildToolModuleHandler.isHandledByBuildTool(module)

  private def couldBeImportedAsSbtProject(project: Project): Boolean = {
    val projectDir =
      try Path.of(SbtUtil.getWorkingDirPath(project))
      catch {
        case _: IllegalStateException => return false
      }
    val projectDirVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(projectDir)
    SbtProjectImportProvider.canImport(projectDirVirtualFile)
  }

  def cachedModuleForPsiFile(file: PsiFile): Option[Module] =
    file.module.orElse {
      cachedInUserData("moduleForAnyTypeOfFile", file, ProjectRootManager.getInstance(file.getProject)) {
        // assuming that most of the time, the cached value will be read instead of recomputed
        inReadAction(Option(ModuleUtilCore.findModuleForPsiElement(file)))
      }
    }
}
