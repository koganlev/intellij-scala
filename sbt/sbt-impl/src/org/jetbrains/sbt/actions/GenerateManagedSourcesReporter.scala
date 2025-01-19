package org.jetbrains.sbt.actions

import com.intellij.build.FilePosition
import com.intellij.build.events.EventResult
import org.jetbrains.plugins.scala.build.{BuildMessages, BuildReporter}

import java.io.File
import scala.collection.mutable

private final class GenerateManagedSourcesReporter extends BuildReporter {
  private val outputLinesBuffer: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty

  override def start(): Unit = {}

  override def finish(messages: BuildMessages): Unit = {}

  override def finishWithFailure(err: Throwable): Unit = {}

  override def finishCanceled(): Unit = {}

  override def warning(message: String, position: Option[FilePosition]): Unit = {}

  override def error(message: String, position: Option[FilePosition]): Unit = {}

  override def info(message: String, position: Option[FilePosition]): Unit = {}

  override def clear(file: File): Unit = {}

  override def log(message: String): Unit = {
    outputLinesBuffer += message
  }

  override def startTask(eventId: BuildMessages.EventId, parent: Option[BuildMessages.EventId], message: String, time: Long): Unit = {}

  override def progressTask(eventId: BuildMessages.EventId, total: Long, progress: Long, unit: String, message: String, time: Long): Unit = {}

  override def finishTask(eventId: BuildMessages.EventId, message: String, result: EventResult, time: Long): Unit = {}

  def outputLines: Seq[String] = outputLinesBuffer.toSeq
}
