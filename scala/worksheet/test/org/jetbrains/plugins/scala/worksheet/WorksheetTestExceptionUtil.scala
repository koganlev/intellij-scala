package org.jetbrains.plugins.scala.worksheet

import com.intellij.testFramework.TestLoggerFactory

// TODO: Remove as soon as we receive some ml fixes in the platform.
object WorksheetTestExceptionUtil {
  //noinspection ApiStatus,UnstableApiUsage,InstanceOf
  def isBoxedLoggedNpe(e: Exception): Boolean = e match {
    case ee: java.util.concurrent.ExecutionException =>
      ee.getCause match {
        case ae: TestLoggerFactory.TestLoggerAssertionError => isNpe(ae.getCause)
      }
    case _ => false
  }

  private def isNpe(t: Throwable): Boolean = t match {
    case npe: NullPointerException => npe.getMessage == "getVirtualFile(...) must not be null"
    case _ => false
  }

  def catchAndIgnore(block: => Unit): Unit =
    try block
    catch {
      case e: Exception if isBoxedLoggedNpe(e) =>
        // Ignore exception
    }
}
