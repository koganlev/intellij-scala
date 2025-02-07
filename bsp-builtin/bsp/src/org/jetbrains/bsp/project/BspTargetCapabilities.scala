package org.jetbrains.bsp.project

case class BspTargetCapabilities(
  canCompile: Boolean,
  canTest: Boolean,
  canRun: Boolean,
  canDebug: Boolean
)
