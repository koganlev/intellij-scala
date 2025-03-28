package org.jetbrains.plugins.scala.worksheet.actions

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import org.jetbrains.plugins.scala.project.{ModuleExt, UserDataKeys}
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings

import scala.ref.WeakReference
import scala.util.Using

@Service(Array(Service.Level.PROJECT))
final class WorksheetModuleService(project: Project) {

  def ensureModuleAttached(virtualFile: VirtualFile): Unit = {
    for {
      wrapperModule <- moduleForFile(virtualFile)
    } {
      val moduleReferenceRef = WeakReference(wrapperModule)
      virtualFile.putUserData(UserDataKeys.SCALA_ATTACHED_MODULE, moduleReferenceRef)
    }
  }

  private def moduleForFile(virtualFile: VirtualFile): Option[Module] = {
    //noinspection ApiStatus
    val cpModule = Using.resource(SlowOperations.knownIssue("SCL-22095, SCL-22097")) { _ =>
      WorksheetFileSettings(project, virtualFile).getModule
    }
    cpModule.map(_.findRepresentativeModuleForSharedSourceModuleOrSelf)
  }
}

object WorksheetModuleService {

  def apply(project: Project): WorksheetModuleService =
    project.getService(classOf[WorksheetModuleService])
}
