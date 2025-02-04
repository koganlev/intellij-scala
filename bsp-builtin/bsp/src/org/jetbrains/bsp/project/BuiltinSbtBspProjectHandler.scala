package org.jetbrains.bsp.project

import com.intellij.openapi.project.Project
import org.jetbrains.bsp.BspUtil
import org.jetbrains.sbt.project.SbtBspProjectHandler

//noinspection ApiStatus
private final class BuiltinSbtBspProjectHandler extends SbtBspProjectHandler {
  override def isHandledByBsp(project: Project): Boolean = BspUtil.isBspProject(project)
}
