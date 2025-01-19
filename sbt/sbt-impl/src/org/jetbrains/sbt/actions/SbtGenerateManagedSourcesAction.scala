package org.jetbrains.sbt.actions

import com.intellij.build.events.impl.{FailureResultImpl, FinishBuildEventImpl, OutputBuildEventImpl, SkippedResultImpl, StartBuildEventImpl, SuccessResultImpl}
import com.intellij.build.{DefaultBuildDescriptor, SyncViewManager}
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.{ActionUpdateThread, AnAction, AnActionEvent}
import com.intellij.openapi.externalSystem.model.task.{ExternalSystemTaskId, ExternalSystemTaskType}
import com.intellij.openapi.progress.{ProgressIndicator, Task}
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.build.BuildMessages
import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.buildinfo.BuildInfo
import org.jetbrains.sbt.project.structure.SbtStructureDump
import org.jetbrains.sbt.project.{SbtExternalSystemManager, SbtProjectSystem}
import org.jetbrains.sbt.{SbtBundle, SbtUtil}

import java.nio.file.{Files, Path}
import scala.util.{Failure, Success}

private final class SbtGenerateManagedSourcesAction extends AnAction(
  SbtBundle.message("sbt.generate.managed.sources.action.title"),
  SbtBundle.message("sbt.generate.managed.sources.action.description"),
  AllIcons.Actions.GeneratedFolder
) {

  override def actionPerformed(e: AnActionEvent): Unit = {
    val project = e.getProject

    val task = new Task.Backgroundable(project, SbtBundle.message("sbt.generate.managed.sources.task.progress.title"), true) {
      override def run(indicator: ProgressIndicator): Unit = {
        val settings = SbtExternalSystemManager.executionSettingsFor(project)
        val projectBasePath = Path.of(settings.realProjectPath)

        val taskId = ExternalSystemTaskId.create(SbtProjectSystem.Id, ExternalSystemTaskType.RESOLVE_PROJECT, project)
        val viewManager = project.getService(classOf[SyncViewManager])
        val descriptor = new DefaultBuildDescriptor(taskId, SbtBundle.message("sbt.generate.managed.sources.action.title"), projectBasePath.toString, System.currentTimeMillis())
        descriptor.setActivateToolWindowWhenAdded(false)
        descriptor.setActivateToolWindowWhenFailed(true)
        viewManager.onEvent(taskId, new StartBuildEventImpl(descriptor, SbtBundle.message("sbt.generate.managed.sources.action.title")))

        val launcher = settings.customLauncher.getOrElse(SbtUtil.getDefaultLauncher)

        val sbtVersion = Version(SbtUtil.detectSbtVersion(projectBasePath.toFile, launcher))
        val sbtStructurePluginBinVersion = SbtUtil.structurePluginBinaryVersion(sbtVersion)
        val addPluginCommandSupported = SbtUtil.isAddPluginCommandSupported(sbtVersion)

        if (!addPluginCommandSupported) {
          val notSupportedWord = SbtBundle.message("sbt.generate.managed.sources.action.not.supported")
          val notSupportedMessage = SbtBundle.message("sbt.generate.managed.sources.action.not.supported.message", sbtVersion.presentation)
          val failureResult = new FailureResultImpl(notSupportedMessage)
          val finishEvent = new FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), notSupportedWord, failureResult)
          viewManager.onEvent(taskId, finishEvent)
          return
        }

        val repoPath = SbtUtil.normalizePath(SbtUtil.getRepoDir)
        val pluginsSbt =
          raw"""resolvers += {
               |  import sbt.Resolver.mavenStyleBasePattern
               |  val artifactPatterns = Vector(mavenStyleBasePattern)
               |  val patterns = Patterns.apply(artifactPatterns, artifactPatterns, true, false, false)
               |  Resolver.file("Scala Plugin Bundled Repository", file(raw"$repoPath"))(patterns)
               |}
               |
               |addSbtPlugin("org.jetbrains.scala" % "sbt-structure-extractor" % "${BuildInfo.sbtStructureVersion}", "$sbtStructurePluginBinVersion")
               |""".stripMargin

        val tmpPluginsSbtFile = Files.createTempFile("idea-gen-managed-sources", ".sbt")
        Files.writeString(tmpPluginsSbtFile, pluginsSbt)
        val setupOptions = Seq(s"-addPluginSbtFile=${tmpPluginsSbtFile.toRealPath()}")
        tmpPluginsSbtFile.toFile.deleteOnExit()

        //noinspection ScalaExtractStringToBundle,ReferencePassedToNls
        viewManager.onEvent(taskId, new OutputBuildEventImpl(taskId, null, SbtBundle.message("sbt.generate.managed.sources.task.progress.title") + System.lineSeparator(), true))

        val reporter = new GenerateManagedSourcesReporter()
        val sbtResult = new SbtStructureDump().runSbt(
          projectBasePath.toFile,
          settings.vmExecutable,
          settings.vmOptions,
          settings.userSetEnvironment,
          launcher,
          settings.sbtOptions,
          setupOptions,
          "show */*:ideaGenerateAllManagedSources",
          SbtBundle.message("sbt.generate.managed.sources.task.progress.title"),
          settings.passParentEnvironment
        )(indicator)(using reporter)

        def reportFailure(@Nullable exception: Throwable): Unit = {
          val sbtOutput = reporter.outputLines.mkString(start = "", sep = System.lineSeparator(), end = System.lineSeparator())
          viewManager.onEvent(taskId, new OutputBuildEventImpl(taskId, null, sbtOutput, true))
          val failureWord = SbtBundle.message("sbt.generate.managed.sources.task.result.failure")
          val failureMessage = SbtBundle.message("sbt.generate.managed.sources.task.result.failure.message")
          val failureResult = new FailureResultImpl(failureMessage, exception)
          val finishEvent = new FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), failureWord, failureResult)
          viewManager.onEvent(taskId, finishEvent)
        }

        sbtResult match {
          case Success(buildMessages) if buildMessages.status == BuildMessages.Error => reportFailure(null)

          case Success(buildMessages) if buildMessages.status == BuildMessages.Canceled =>
            val canceledWord = SbtBundle.message("sbt.generate.managed.sources.task.result.canceled")
            val canceledMessage = SbtBundle.message("sbt.generate.managed.sources.task.result.canceled.message")
            viewManager.onEvent(taskId, new OutputBuildEventImpl(taskId, null, canceledMessage, true))
            val finishEvent = new FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), canceledWord, new SkippedResultImpl())
            viewManager.onEvent(taskId, finishEvent)

          case Success(_) =>
            val lines = reporter.outputLines
            val containsErrors = lines.exists(_.startsWith("[error]"))

            if (containsErrors) {
              reportFailure(null)
            } else {
              val generatedSources = lines.collect { case s"[info] * $path" => Path.of(path).toRealPath() }
              val fileManager = VirtualFileManager.getInstance()
              generatedSources.foreach(fileManager.refreshAndFindFileByNioPath)
              //noinspection ReferencePassedToNls
              val output =
                s"""${SbtBundle.message("sbt.generate.managed.sources.task.result.success.message")}
                   |${generatedSources.map(path => s"  - $path").mkString(System.lineSeparator())}
                   |""".stripMargin
              viewManager.onEvent(taskId, new OutputBuildEventImpl(taskId, null, output, true))
              val successWord = SbtBundle.message("sbt.generate.managed.sources.task.result.success")
              val successResult = new SuccessResultImpl()
              val successEvent = new FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), successWord, successResult)
              viewManager.onEvent(taskId, successEvent)
            }

          case Failure(exception) => reportFailure(exception)
        }
      }
    }

    task.queue()
  }

  override def update(e: AnActionEvent): Unit = {
    val project = e.getProject
    if (project eq null) return
    val enabled = SbtUtil.isSbtProject(project)
    e.getPresentation.setEnabledAndVisible(enabled)
  }

  override def getActionUpdateThread: ActionUpdateThread = ActionUpdateThread.BGT
}
