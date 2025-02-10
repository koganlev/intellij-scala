package org.jetbrains.plugins.scala.compiler

import com.intellij.openapi.projectRoots.JavaSdkVersion

import java.io.File

final case class JDK(executable: File, tools: Option[File], name: String, version: Option[JavaSdkVersion])
