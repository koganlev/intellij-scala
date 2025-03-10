package org.jetbrains.plugins.scala.project

import com.intellij.openapi.module.Module

trait SyntheticModule { self: Module =>
  def underlying: Module
}
