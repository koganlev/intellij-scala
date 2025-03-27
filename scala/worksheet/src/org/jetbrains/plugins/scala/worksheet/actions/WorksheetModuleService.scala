package org.jetbrains.plugins.scala.worksheet.actions

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import org.jetbrains.plugins.scala.project.{ModuleExt, UserDataKeys}
import org.jetbrains.plugins.scala.worksheet.settings.WorksheetFileSettings

import scala.collection.mutable
import scala.ref.Reference
import scala.util.Using

@Service(Array(Service.Level.PROJECT))
final class WorksheetModuleService(project: Project) {

  private val modulesMap = mutable.HashMap[VirtualFile, Module]()

  def ensureModuleAttached(virtualFile: VirtualFile): Unit = {
    for {
      wrapperModule <- moduleForFile(virtualFile)
    } {
      val moduleReferenceRef = moduleReference(wrapperModule)
      virtualFile.putUserData(UserDataKeys.SCALA_ATTACHED_MODULE, moduleReferenceRef)
    }
  }

  private def moduleForFile(virtualFile: VirtualFile): Option[Module] = {
    //noinspection ApiStatus
    val cpModule = Using.resource(SlowOperations.knownIssue("SCL-22095, SCL-22097")) { _ =>
      WorksheetFileSettings(project, virtualFile).getModule
    }
    cpModule.map(moduleForFile(virtualFile, _))
  }

  private def moduleForFile(virtualFile: VirtualFile, currentCpModule: Module): Module = {
    val maybeCached = modulesMap.get(virtualFile)
    val representative = currentCpModule.findRepresentativeModuleForSharedSourceModuleOrSelf
    val result = maybeCached match {
      case Some(cached) =>
        if (cached != representative) {
          modulesMap.put(virtualFile, representative)
          representative
        } else
          cached
      case _ =>
        modulesMap.put(virtualFile, representative)
        representative
    }
    result
  }

  private def moduleReference(module: Module): Reference[Module] =
    new Reference[Module] {
      override def apply(): Module = module
      override def get: Option[Module] = Some(apply())
      override def clear(): Unit = ()
      override def enqueue(): Boolean = false
      override def isEnqueued: Boolean = false
    }
}

object WorksheetModuleService {

  def apply(project: Project): WorksheetModuleService =
    project.getService(classOf[WorksheetModuleService])
}
