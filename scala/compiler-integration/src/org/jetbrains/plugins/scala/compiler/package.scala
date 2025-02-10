package org.jetbrains.plugins.scala

import com.intellij.compiler.server.BuildManager
import com.intellij.openapi.application.ApplicationManager

package object compiler {
  def executeOnBuildThread(runnable: Runnable): Unit = {
    if (ApplicationManager.getApplication.isUnitTestMode) {
      runnable.run()
    } else {
      BuildManager.getInstance().runCommand(runnable)
    }
  }
}
