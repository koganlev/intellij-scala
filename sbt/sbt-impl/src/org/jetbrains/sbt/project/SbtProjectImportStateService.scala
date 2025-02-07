package org.jetbrains.sbt.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.{DependencyScope, LibraryOrderEntry, ModuleRootManager, OrderRootType}
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.project.{ModuleExt, ProjectPsiFileExt}
import org.jetbrains.sbt.settings.SbtSettings

import java.util.concurrent.ConcurrentHashMap

@Service(Array(Service.Level.PROJECT))
private[sbt] final class SbtProjectImportStateService(project: Project) {
  import SbtProjectImportStateService.State

  private val state: ConcurrentHashMap[String, State] = new ConcurrentHashMap()

  /**
   * Checks if the project has been fully imported as an sbt project using the built-in sbt support. In any other case,
   * it returns `false`.
   *
   * @note This method also returns `false` for sbt projects imported via BSP.
   * @see [[org.jetbrains.sbt.SbtUtil.couldBeSbtProject]] which should most likely be called before this method
   */
  def isImported(file: PsiFile): Boolean = {
    val relevantPath = file.module.flatMap(m => Option(ExternalSystemApiUtil.getExternalRootProjectPath(m)))
    relevantPath match {
      case Some(path) =>
        val s = state.computeIfAbsent(path, computeImportState)
        s == State.Imported
      case None =>
        // The project has not even been linked to the external system machinery. It is therefore not imported.
        false
    }
  }

  def reset(): Unit = {
    state.clear()
  }

  private def computeImportState(relevantProjectPath: String): State = {
    val allBuildModules = ModuleManager.getInstance(project).getModules.filter(_.hasBuildModuleType)
    val relevantBuildModules = allBuildModules.filter { module =>
      val projectRootPath = Option(ExternalSystemApiUtil.getExternalRootProjectPath(module))
      projectRootPath.contains(relevantProjectPath)
    }
    val imported = relevantBuildModules.nonEmpty && relevantBuildModules.forall(hasSbtLibrary)
    val newState = if (imported) State.Imported else State.NotImported
    newState
  }

  private def hasSbtLibrary(buildModule: Module): Boolean =
    SbtSettings.getInstance(project)
      .getLinkedProjectSettings(buildModule)
      .flatMap(s => Option(s.sbtVersion))
      .flatMap { version =>
        ModuleRootManager.getInstance(buildModule)
          .getOrderEntries
          .iterator
          .collectFirst {
            case library: LibraryOrderEntry if isSbtLibrary(library, version) => library
          }
      }
      .isDefined

  private def isSbtLibrary(library: LibraryOrderEntry, version: String): Boolean = {
    def expectedName = library.getLibraryName == s"sbt: sbt-$version"
    def expectedDependencyScope = library.getScope == DependencyScope.PROVIDED
    def expectedJars = library.getRootFiles(OrderRootType.CLASSES).nonEmpty
    expectedName && expectedDependencyScope && expectedJars
  }
}

private[sbt] object SbtProjectImportStateService {

  def instance(project: Project): SbtProjectImportStateService =
    project.getService(classOf[SbtProjectImportStateService])

  private sealed trait State extends Product with Serializable
  private object State {
    case object Imported extends State
    case object NotImported extends State
  }
}
