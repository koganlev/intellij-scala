package org.jetbrains.plugins.scala.project.bsp

import com.intellij.openapi.project.Project
import org.jetbrains.sbt.project.SbtBspProjectHandler

//noinspection ApiStatus
private final class PlatformSbtBspProjectHandler extends SbtBspProjectHandler {
  override def isHandledByBsp(project: Project): Boolean =
    org.jetbrains.plugins.bsp.config.BspProjectPropertiesKt.isBspProject(project)
}
