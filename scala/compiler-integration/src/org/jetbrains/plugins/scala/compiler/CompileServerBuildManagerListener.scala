package org.jetbrains.plugins.scala.compiler

import com.intellij.compiler.server.BuildManagerListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.extensions.LoggerExt
import org.jetbrains.plugins.scala.project.ProjectExt
import org.jetbrains.plugins.scala.settings.ScalaCompileServerSettings

import java.util.UUID

private final class CompileServerBuildManagerListener extends BuildManagerListener {
  import CompileServerBuildManagerListener.Log

  override def buildStarted(project: Project, sessionId: UUID, isAutomake: Boolean): Unit = {
    if (!project.isDisposed)
      ensureCompileServerRunning(project)
    if (ScalaCompileServerSettings.getInstance().COMPILE_SERVER_ENABLED)
      CompileServerNotificationsService.get(project).warnIfCompileServerJdkMayLeadToCompilationProblems()
  }

  private def ensureCompileServerRunning(project: Project): Unit = {
    val settings = ScalaCompileServerSettings.getInstance

    val compileServerRequired = settings.COMPILE_SERVER_ENABLED && project.hasScala
    Log.traceWithDebugInDev(s"CompileServerBuildManagerListener.compileServerRequired: $compileServerRequired")
    if (compileServerRequired) {
      CompileServerLauncher.ensureServerRunning(project)
    }
  }
}

private object CompileServerBuildManagerListener {
  val Log: Logger = Logger.getInstance(classOf[CompileServerBuildManagerListener])
}
