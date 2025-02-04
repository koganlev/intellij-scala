package org.jetbrains.sbt.project.template.wizard.buildSystem

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.{ApiStatus, Nullable, TestOnly}

trait ScalaSampleCodeNewProjectWizardData {
  @TestOnly
  private[project] def setAddSampleCode(value: java.lang.Boolean): Unit

  @TestOnly
  @ApiStatus.ScheduledForRemoval(inVersion = "2025.2")
  @deprecated("The onboarding tips are generated unconditionally with the sample code")
  private[project] def setGenerateOnboardingTips(value: java.lang.Boolean): Unit = {}
}

object ScalaSampleCodeNewProjectWizardData {
  val KEY: Key[ScalaSampleCodeNewProjectWizardData] = Key.create(classOf[ScalaSampleCodeNewProjectWizardData].getName)

  @Nullable
  def scalaSampleCodeData(step: NewProjectWizardStep): ScalaSampleCodeNewProjectWizardData =
    step.getData.getUserData(KEY)
}
