package org.jetbrains.sbt.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.{LibraryOrderEntry, ModuleRootManager, OrderRootType}
import org.jetbrains.plugins.scala.project.ModuleExt
import org.jetbrains.sbt.settings.SbtSettings

@Service(Array(Service.Level.PROJECT))
private[sbt] final class SbtProjectImportStateService(project: Project) {
  import SbtProjectImportStateService.State

  /**
   * Shared mutable state. Needs to be queried and updated in a `synchronized` block.
   *
   * Essentially, this is a way to avoid querying the project modules and libraries on every sbt project file open. The
   * project is queried once and the result is cached. Project reimports and library changes reset this state.
   * It will be recomputed again the next time.
   */
  private var state: State = State.NotInitialized

  def isImported: Boolean = synchronized {
    state match {
      case State.NotInitialized =>
        updateProjectState()
        state == State.Imported
      case State.Imported => true
      case State.NotImported => false
    }
  }

  def reset(): Unit = synchronized {
    state = State.NotInitialized
  }

  private def updateProjectState(): Unit = {
    val buildModules = ModuleManager.getInstance(project).getModules.filter(_.hasBuildModuleType)
    val imported = buildModules.nonEmpty && buildModules.forall(hasSbtLibrary)
    state = if (imported) State.Imported else State.NotImported
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
    def expectedJar = library.getRootFiles(OrderRootType.CLASSES).exists { vf =>
      val canonicalPath = vf.getCanonicalPath
      (canonicalPath ne null) && canonicalPath.contains(s"org.scala-sbt/sbt/$version/sbt-$version.jar")
    }
    expectedName && expectedJar
  }
}

private[sbt] object SbtProjectImportStateService {

  def instance(project: Project): SbtProjectImportStateService =
    project.getService(classOf[SbtProjectImportStateService])

  private sealed trait State extends Product with Serializable
  private object State {
    case object NotInitialized extends State
    case object Imported extends State
    case object NotImported extends State
  }
}
