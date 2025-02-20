package org.jetbrains.sbt.project.template.wizard.buildSystem

import com.intellij.ide.JavaUiBundle
import com.intellij.ide.projectWizard.NewProjectWizardCollector
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.observable.properties.{GraphProperty, ObservableProperty, PropertyGraph}
import com.intellij.openapi.observable.util.BindUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.{LibrariesContainer, LibrariesContainerFactory}
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder._
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ValidationInfoBuilder
import kotlin.Unit.{INSTANCE => KUnit}
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.scala.extensions.ToNullSafe
import org.jetbrains.plugins.scala.project.{Version, Versions}
import org.jetbrains.plugins.scala.project.template.ScalaSDKStepLike
import org.jetbrains.plugins.scala.util.ui.extensions.JComboBoxOps
import org.jetbrains.sbt.project.template.wizard.{SbtModuleStepLike, ScalaNewProjectWizardMultiStep}
import org.jetbrains.sbt.project.template.{SbtModuleBuilder, SbtModuleBuilderSelections}

import javax.swing.JLabel
import scala.annotation.nowarn
import scala.collection.immutable.ListSet

//noinspection ApiStatus,UnstableApiUsage
final class SbtScalaNewProjectWizardStep(parent: ScalaNewProjectWizardMultiStep)
  extends AbstractNewProjectWizardStep(parent)
    with SbtScalaNewProjectWizardData
    with ScalaSampleCodeNewProjectWizardData
    with ScalaSDKStepLike
    with SbtModuleStepLike
    with JDKStepLike {

  override protected val librariesContainer: LibrariesContainer =
    LibrariesContainerFactory.createContainer(parent.getContext.getProject)

  override protected lazy val defaultAvailableScalaVersions: Seq[String] = Versions.Scala.allHardcodedVersions.map(_.presentation)

  @inline private def propertyGraph: PropertyGraph = getPropertyGraph

  private val moduleNameProperty: GraphProperty[String] = propertyGraph.lazyProperty(() => parent.getName)

  private val addSampleCodeProperty: GraphProperty[java.lang.Boolean] = propertyGraph.property(java.lang.Boolean.FALSE)
  BindUtil.bindBooleanStorage(addSampleCodeProperty, "NewProjectWizard.addSampleCodeState")
  private def needToAddSampleCode: Boolean = addSampleCodeProperty.get()

  @TestOnly override private[project] def setAddSampleCode(value: java.lang.Boolean): Unit = addSampleCodeProperty.set(value)

  @TestOnly override def setScalaVersion(version: String): Unit = scalaVersionComboBox.setSelectedItemEnsuring(version)
  @TestOnly override private[project] def setSbtVersion(version: String): Unit = sbtVersionComboBox.setSelectedItemEnsuring(Version(version))
  @TestOnly override private[project] def setPackagePrefix(prefix: String): Unit = packagePrefixTextField.setText(prefix)

  private def getModuleName: String = moduleNameProperty.get()

  override protected val selections: SbtModuleBuilderSelections = SbtModuleBuilderSelections.default

  override protected lazy val defaultAvailableSbtVersions: ListSet[Version] = ListSet(Versions.SBT.allHardcodedVersions: _*)
  override protected val defaultAvailableSbtVersionsForScala3: Seq[Version] = Versions.SBT.sbtVersionsForScala3(defaultAvailableSbtVersions.toSeq)

  locally {
    moduleNameProperty.dependsOn(parent.getNameProperty: ObservableProperty[String], (() => parent.getName): kotlin.jvm.functions.Function0[_ <: String])

    getData.putUserData(SbtScalaNewProjectWizardData.KEY, this)
    getData.putUserData(ScalaSampleCodeNewProjectWizardData.KEY, this)
  }

  override def setupProject(project: Project): Unit = {
    val builder = new SbtModuleBuilder(this.selections)
    builder.setName(getModuleName)
    val projectRoot = getContext.getProjectDirectory.toAbsolutePath
    builder.setContentEntryPath(projectRoot.toString)

    setProjectOrModuleSdk(project, parent, builder, sdk)

    ExternalProjectsManagerImpl.setupCreatedProject(project)
    /** NEWLY_CREATED_PROJECT must be set up to prevent the call of markDirtyAllExternalProjects in ExternalProjectsDataStorage#load.
     * As a result, NEWLY_IMPORTED_PROJECT must also be set to keep the same behaviour as before in ExternalSystemStartupActivity.kt:48 (do not call ExternalSystemUtil#refreshProjects).
     * Similar thing is done in AbstractGradleModuleBuilder#setupModule */
    project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, java.lang.Boolean.TRUE)
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, java.lang.Boolean.TRUE)

    if (needToAddSampleCode) {
      val files = addScalaSampleCode(
        project = project,
        path = s"$projectRoot/src/main/scala",
        isScala3 = this.selections.scalaVersion.exists(_.startsWith("3.")),
        packagePrefix = this.selections.packagePrefix,
        withOnboardingTips = true
      )
      builder.openFileEditorAfterProjectOpened = files
    }

    startJdkDownloadIfNeeded(project)
    builder.commit(project)
  }

  override def setupUI(panel: Panel): Unit = {
    setupJavaSdkUI(panel)

    panel.row(sbtLabelText, (row: Row) => {
      row.layout(RowLayout.PARENT_GRID)
      row.cell(sbtVersionComboBox).horizontalAlign(HorizontalAlign.FILL): @nowarn("cat=deprecation")
      row.cell(downloadSbtSourcesCheckbox)
      KUnit
    })

    setUpScalaUI(panel, downloadSourcesCheckbox = true)

    setupPackagePrefixUI(panel)

    panel.row(null: JLabel, (row: Row) => {
      val cb = row.checkBox(UIBundle.message("label.project.wizard.new.project.add.sample.code"))
      ButtonKt.bindSelected(cb, addSampleCodeProperty: com.intellij.openapi.observable.properties.ObservableMutableProperty[java.lang.Boolean])
      ButtonKt.whenStateChangedFromUi(cb, null, value => {
        NewProjectWizardCollector.Base.INSTANCE.logAddSampleCodeChanged(parent, value)
        KUnit
      })
      KUnit
    }).topGap(TopGap.SMALL)

    panel.collapsibleGroup(UIBundle.message("label.project.wizard.new.project.advanced.settings"), true, (panel: Panel) => {
      if (getContext.isCreatingNewProject) {
        panel.row(UIBundle.message("label.project.wizard.new.project.module.name"), (row: Row) => {
          val validator: kotlin.jvm.functions.Function2[ValidationInfoBuilder, JBTextField, ValidationInfo] = (builder, field) => {
            validateModuleName(builder, field)
          }
          TextFieldKt.bindText(row.textField, moduleNameProperty: com.intellij.openapi.observable.properties.ObservableMutableProperty[String])
            .horizontalAlign(HorizontalAlign.FILL)
            .validationOnInput(validator)
            .validationOnApply(validator): @nowarn("cat=deprecation")
          KUnit
        })
      }
      KUnit
    })

    initSelectionsAndUi(getContext.getDisposable)
  }

  private def validateModuleName(builder: ValidationInfoBuilder, field: JBTextField): ValidationInfo = {
    val moduleName = field.getText
    val project = getContext.getProject
    if (moduleName.isEmpty)
      builder.error(JavaUiBundle.message("module.name.location.dialog.message.enter.module.name"))
    else if (project == null)
      null
    else {
      // Name uniqueness
      val model = ProjectStructureConfigurable.getInstance(project)
        .nullSafe
        .map(_.getContext)
        .map(_.getModulesConfigurator)
        .map(_.getModuleModel)
        .orNull

      val module = if (model == null)
        ModuleManager.getInstance(project).findModuleByName(moduleName)
      else
        model.findModuleByName(moduleName)

      if (module != null)
        builder.error(JavaUiBundle.message("module.name.location.dialog.message.module.already.exist.in.project", moduleName))
      else
        null
    }
  }
}
