package org.jetbrains.sbt.project.template.wizard.buildSystem;

import com.intellij.ide.JavaUiBundle
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.projectWizard.ProjectWizardJdkComboBoxKt.projectWizardJdkComboBox
import com.intellij.ide.projectWizard.generators.JdkDownloadService
import com.intellij.openapi.observable.properties.{GraphProperty, PropertyGraph}
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.{JavaSdkVersion, Sdk}
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadTask
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.ui.dsl.builder._
import kotlin.Unit.{INSTANCE => KUnit}

trait JDKStepLike { this: AbstractNewProjectWizardStep =>

  @inline private def propertyGraph: PropertyGraph = getPropertyGraph

  protected val sdkProperty: GraphProperty[Sdk] = propertyGraph.property(null)
  private val sdkDownloadTaskProperty: GraphProperty[SdkDownloadTask] = propertyGraph.property[SdkDownloadTask](null)

  private def sdkDownloadTask: Option[SdkDownloadTask] = Option(sdkDownloadTaskProperty.get())

  protected def sdk: Option[Sdk] = Option(sdkProperty.get())

  protected def startJdkDownloadIfNeeded(project: Project): Unit =
    sdkDownloadTask.collect { case task: JdkDownloadTask =>
      val service = project.getService(classOf[JdkDownloadService])
      service.scheduleDownloadJdkForNewProject(task)
    }

  protected def setupJavaSdkUI(builder: Panel): Unit =
    builder.row(JavaUiBundle.message("label.project.wizard.new.project.jdk"), (row: Row) => {
      projectWizardJdkComboBox(this, row, sdkProperty, sdkDownloadTaskProperty)
      KUnit
    }).bottomGap(BottomGap.SMALL)

  protected def getExpectedJavaSdkVersion: Option[JavaSdkVersion] = {
    val versionString = sdk.map(_.getVersionString)
    val plannedVersion = sdkDownloadTask.map(_.getPlannedVersion)
    val expectedJdkVersion = versionString.orElse(plannedVersion)
    expectedJdkVersion.map(JavaSdkVersion.fromVersionString)
  }
}
