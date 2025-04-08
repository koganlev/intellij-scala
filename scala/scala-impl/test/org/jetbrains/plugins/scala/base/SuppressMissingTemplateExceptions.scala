package org.jetbrains.plugins.scala.base

import com.intellij.platform.testFramework.teamCity.GenerifyKt.generifyErrorMessage
import com.intellij.platform.testFramework.teamCity.TeamCityPrinterKt.reportTestFailure
import com.intellij.testFramework.TestLoggerKt.withErrorLog
import com.intellij.testFramework.{ErrorLog, LoggedError, TestLoggerFactory, UsefulTestCase}
import com.intellij.util.ThrowableRunnable
import org.jetbrains.annotations.{NotNull, Nullable}
import org.junit.AssumptionViolatedException
import org.junit.runner.Description

import java.io.{PrintWriter, StringWriter}
import scala.jdk.CollectionConverters._
import scala.util.chaining.scalaUtilChainingOps

/**
 * Contains a bunch of platform copy-pasted and modified code which specifically suppresses logged exceptions about
 * missing file templates during test execution.
 */
//noinspection ApiStatus,UnstableApiUsage
trait SuppressMissingTemplateExceptions extends UsefulTestCase {
  @NotNull
  override protected def wrapTestRunnable(@NotNull testRunnable: ThrowableRunnable[Throwable]): ThrowableRunnable[Throwable] = {
    val testDescription = Description.createTestDescription(getClass, getName)
    () => {
      var success = false
      TestLoggerFactory.onTestStarted()
      try {
        customRecordErrorsLoggedInTheCurrentThreadAndReportThemAsFailures(testRunnable)
        success = true
      } catch {
        case e: AssumptionViolatedException =>
          success = true
          throw e
        case t: Throwable =>
          TestLoggerFactory.logTestFailure(t)
          throw t
      } finally TestLoggerFactory.onTestFinished(success, testDescription)
    }
  }

  private def customRecordErrorsLoggedInTheCurrentThreadAndReportThemAsFailures(testRunnable: ThrowableRunnable[Throwable]): Unit = {
    if (System.getProperty("intellij.testFramework.rethrow.logged.errors") == "true") {
      testRunnable.run()
      return
    }
    val errorLog = new ErrorLog()
    try {
      withErrorLog(errorLog).pipe { _ =>
        testRunnable.run()
      }
    }
    finally {
      customReportAsFailures(errorLog)
    }
  }

  private def customReportAsFailures(errorLog: ErrorLog): Unit = {
    val errors = errorLog.takeLoggedErrors().asScala
    for (error <- errors if shouldLog(error)) {
      logAsTeamcityTestFailure(error)
    }
  }

  private def shouldLog(error: LoggedError): Boolean =
    !error.getMessage.startsWith("Can't find template")

  private def logAsTeamcityTestFailure(error: LoggedError): Unit = {
    val message = findMessage(error)
    val stackTraceContent = stackTraceToString(error)
    val testName = if (message == null) "Error logged without message" else generifyErrorMessage(message)
    reportTestFailure(System.out, testName, Option(message).getOrElse(""), stackTraceContent)
  }

  @Nullable
  private def findMessage(t: Throwable): String = {
    var current: Throwable = t
    while (true) {
      val message = current.getMessage
      if ((message ne null) && !message.isBlank) {
        return message
      }
      val cause = current.getCause
      if (cause == null || cause == current) {
        return null
      }
      current = cause
    }
    null
  }

  private def stackTraceToString(throwable: Throwable): String = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    throwable.printStackTrace(pw)
    pw.flush()
    sw.toString
  }
}
