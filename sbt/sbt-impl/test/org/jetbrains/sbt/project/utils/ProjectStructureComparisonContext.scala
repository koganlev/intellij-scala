package org.jetbrains.sbt.project.utils

import com.intellij.openapi.project.{Project, ProjectUtil}

class ProjectStructureComparisonContext(
  val options: ProjectComparisonOptions,
  val macroSubstitutor: MacroSubstitutor
) {
  def withOptions(optionsModifier: ProjectComparisonOptions => ProjectComparisonOptions): ProjectStructureComparisonContext = {
    val optionsNew = optionsModifier(options)
    new ProjectStructureComparisonContext(optionsNew, macroSubstitutor)
  }

  def withOptions(optionsNew: ProjectComparisonOptions): ProjectStructureComparisonContext = {
    new ProjectStructureComparisonContext(optionsNew, macroSubstitutor)
  }
}

object ProjectStructureComparisonContext {
  object Implicit {
    implicit def default(implicit project: Project): ProjectStructureComparisonContext = {
      val projectRoot = ProjectUtil.guessProjectDir(project)
      val substitutor = new MacroSubstitutor(Map(
        MacroSubstitutor.Keys.ProjectRoot -> projectRoot.getPath
      ))
      new ProjectStructureComparisonContext(ProjectComparisonOptions.Default, substitutor)
    }
  }
}
