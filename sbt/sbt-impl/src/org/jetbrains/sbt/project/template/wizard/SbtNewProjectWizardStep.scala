package org.jetbrains.sbt.project.template.wizard

import com.intellij.ide.JavaUiBundle
import com.intellij.ide.projectWizard.ProjectWizardJdkComboBoxKt.projectWizardJdkComboBox
import com.intellij.ide.projectWizard.generators.JdkDownloadService
import com.intellij.ide.wizard.{AbstractNewProjectWizardStep, NewProjectWizardStep}
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.{GraphProperty, PropertyGraph}
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadTask
import com.intellij.openapi.projectRoots.{JavaSdkVersion, Sdk}
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.{BottomGap, Panel, Row, RowLayout}
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.project.{Version, Versions}
import org.jetbrains.plugins.scala.util.AsynchronousVersionsDownloading
import org.jetbrains.sbt.project.template.wizard.kotlin_interop.KotlinInteropUtils
import org.jetbrains.sbt.SbtBundle
import org.jetbrains.sbt.project.template.SComboBox

import java.lang
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Unit.{INSTANCE => KUnit}
import scala.annotation.nowarn
import scala.collection.immutable.ListSet

//TODO add downloading Scala and its combobox to this step so that everything needed for sbt is in one place
abstract class SbtNewProjectWizardStep(parent: NewProjectWizardStep) extends AbstractNewProjectWizardStep(parent)
  with AsynchronousVersionsDownloading {

  protected val defaultAvailableSbtVersions: ListSet[Version]

  @inline private def propertyGraph: PropertyGraph = getPropertyGraph

  protected val sdkProperty: GraphProperty[Sdk] = propertyGraph.property(null)
  private val sdkDownloadTaskProperty: GraphProperty[SdkDownloadTask] = propertyGraph.property[SdkDownloadTask](null)

  protected lazy val sbtVersionProperty: GraphProperty[Version] = propertyGraph.property(defaultAvailableSbtVersions.head)
  protected val downloadSbtSourcesProperty: GraphProperty[lang.Boolean] = propertyGraph.property(java.lang.Boolean.FALSE)

  private def sdkDownloadTask: Option[SdkDownloadTask] = Option(sdkDownloadTaskProperty.get())
  protected def sdk: Option[Sdk] = Option(sdkProperty.get())

  protected final val isSbtVersionManuallySelected: AtomicBoolean = new AtomicBoolean(false)
  private val isSbtLoading = new AtomicBoolean(false)

  protected lazy val sbtVersionComboBox: SComboBox[Version] = createSComboBoxWithSearchingListRenderer(defaultAvailableSbtVersions, None, isSbtLoading)

  protected def loadSbtVersions(indicator: ProgressIndicator): Seq[Version]
  protected def setSbtVersion(versions: Seq[Version]): Unit

  protected val downloadSbtSourcesCheckbox: JBCheckBox = applyTo(new JBCheckBox(SbtBundle.message("sbt.module.step.download.sources")))(
    _.setToolTipText(SbtBundle.message("sbt.download.sbt.sources"))
  )

  protected def setupSbtUI(panel: Panel): Unit =
    panel.row(SbtBundle.message("sbt.settings.sbt"), (row: Row) => {
      row.layout(RowLayout.PARENT_GRID)

      val sbtVersionComboBoxCell = row.cell(sbtVersionComboBox).horizontalAlign(HorizontalAlign.FILL): @nowarn("cat=deprecation")
      val downloadSbtSourcesCheckboxCell = row.cell(downloadSbtSourcesCheckbox)

      KotlinInteropUtils.bindItem(sbtVersionComboBoxCell, sbtVersionProperty)
      KotlinInteropUtils.bind(downloadSbtSourcesCheckboxCell, downloadSbtSourcesProperty)

      KUnit
    })

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

  protected final def downloadSbtVersions(disposable: Disposable): Unit = {
    val sbtDownloadVersions: ProgressIndicator => Seq[Version] = indicator => loadSbtVersions(indicator)
    downloadVersionsAsynchronously(isSbtLoading, disposable, sbtDownloadVersions, Versions.SBT.toString)(setSbtVersion)

    sbtVersionComboBox.addActionListener { _ =>
      isSbtVersionManuallySelected.set(true)
    }
  }
}
