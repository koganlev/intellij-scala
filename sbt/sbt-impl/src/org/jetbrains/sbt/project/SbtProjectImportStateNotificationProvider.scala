package org.jetbrains.sbt.project

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.{PsiFile, PsiManager}
import com.intellij.ui.{EditorNotificationPanel, EditorNotificationProvider}
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.scala.project.ScalaProjectConfigurationService
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.codeInsight.daemon.SbtProblemHighlightFilter
import org.jetbrains.sbt.language.SbtFile

import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

private final class SbtProjectImportStateNotificationProvider extends EditorNotificationProvider {

  @RequiresReadLock
  @Nullable
  override def collectNotificationData(
    project: Project,
    file: VirtualFile
  ): java.util.function.Function[_ >: FileEditor, _ <: JComponent] = {
    val psiFile = PsiManager.getInstance(project).findFile(file)
    val sbtFile = psiFile match {
      case f: SbtFile => f
      case _ =>
        // Not an sbt file, do not show any notifications.
        return null
    }
    // The sbt file is not a part of the current project, do not show any notifications.
    if (!SbtProblemHighlightFilter.shouldHighlightSbtFile(sbtFile)) return null
    // Project reload is already in progress, do not show any notifications.
    if (ScalaProjectConfigurationService.getInstance(project).isSyncInProgress) return null
    // The project is fully imported, do not show any notifications.
    if (SbtProjectImportStateService.instance(project).isImported) return null

    (fileEditor: FileEditor) => {
      val panel = new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Warning)
      panel.setText(SbtBundle.message("sbt.project.not.loaded.notification.panel.text"))
      val fixHandler = new EditorNotificationPanel.ActionHandler {
        override def handlePanelActionClick(panel: EditorNotificationPanel, event: HyperlinkEvent): Unit = {
          reloadSbtProject()
        }

        override def handleQuickFixClick(editor: Editor, psiFile: PsiFile): Unit = {
          reloadSbtProject()
        }

        private def reloadSbtProject(): Unit = {
          val builder = new ImportSpecBuilder(project, SbtProjectSystem.Id).use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
          ExternalSystemUtil.refreshProjects(builder)
        }
      }
      panel.createActionLabel(SbtBundle.message("sbt.project.not.loaded.notification.panel.action.label"), fixHandler, true)
      panel
    }
  }
}
