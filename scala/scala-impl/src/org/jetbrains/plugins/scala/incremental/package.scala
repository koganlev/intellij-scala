package org.jetbrains.plugins.scala

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.{DaemonCodeAnalyzerEx, DaemonCodeAnalyzerImpl, FileStatusMap}
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

package object incremental {
  private final val Reason = "Incremental highlighting"

  private val combineDirtyScopesMethod = {
    val method = classOf[FileStatusMap].getDeclaredMethod("combineDirtyScopes", classOf[Document], classOf[TextRange], classOf[Object])
    method.setAccessible(true)
    method
  }

  private val stopProcessMethod = {
    val method = classOf[DaemonCodeAnalyzerImpl].getDeclaredMethod("stopProcess", classOf[Boolean], classOf[String])
    method.setAccessible(true)
    method
  }

  private[incremental] implicit class DaemonCodeAnalyzerExt(private val daemon: DaemonCodeAnalyzer) extends AnyVal {
    // com.intellij.codeInsight.daemon.impl.FileStatusMap.combineDirtyScopes
    def combineDirtyScopes(document: Document, scope: TextRange): Unit = {
      combineDirtyScopesMethod.invoke(daemon.asInstanceOf[DaemonCodeAnalyzerEx].getFileStatusMap, document, scope, Reason)
    }

    // com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl.stopProcess
    def stopProcess(toRestartAlarm: Boolean): Unit = {
      stopProcessMethod.invoke(daemon, toRestartAlarm, Reason)
    }
  }
}
