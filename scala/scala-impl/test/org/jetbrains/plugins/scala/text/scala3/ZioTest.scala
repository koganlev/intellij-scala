package org.jetbrains.plugins.scala.text.scala3

import org.jetbrains.plugins.scala.DependencyManagerBase.RichStr
import org.jetbrains.plugins.scala.text.TextToTextTestBase

class ZioTest extends TextToTextTestBase(
  Seq(
    "dev.zio" %% "zio" % "2.0.22",
    "dev.zio" %% "zio-streams" % "2.0.22",
  ),
  Seq("zio"), Set.empty, 225,
  Set(
    "zio.Experimental", // Cannot resolve scala.runtime.$throws
    "zio.internal.stacktracer.SourceLocation", // Given without a name
    "zio.metrics.jvm.BufferPools", // External library reference
    "zio.metrics.jvm.GarbageCollector", // External library reference
    "zio.metrics.jvm.MemoryAllocation", // External library reference
    "zio.metrics.jvm.MemoryPools", // External library reference
    "zio.stream.ZStreamPlatformSpecificConstructors", // .type.Emit
  ),
  withSources = true,
  Set(
    "zio.Config", // extends Exception, Error
    "zio.Fiber", // case class extends Product & Serializable
    "zio.IsReloadableMacros", // private[zio]
    "zio.RuntimeFlag", // reference constants
    "zio.Scope", // private type alias
    "zio.TagMacros", // private[zio]
    "zio.ZEnvironment", // izumi.reflect.macrortti
    "zio.ZIO", // implicit conversion for implicit class
    "zio.ZLayer", // implicit conversion for implicit class
    "zio.ZLogger", // izumi.reflect.macrortti
    "zio.internal.BoundedHubArb", // private[internal]
    "zio.internal.BoundedHubPow2", // private[internal]
    "zio.internal.BoundedHubSingle", // private[internal]
    "zio.internal.FastList", // implicit conversion for implicit class
    "zio.internal.FiberRuntime", // x * y constant
    "zio.internal.LinkedQueue", // Int.MaxValue constant
    "zio.internal.Stack", // empty () constructor
    "zio.internal.TerminalRendering", // implicit conversion for implicit class
    "zio.internal.UnboundedHub", // private[internal]
    "zio.internal.ZScheduler", // private[internal]
    "zio.internal.ansi", // no final, implicit conversion for implicit class
    "zio.internal.macros.LayerBuilder", // scala.List
    "zio.internal.macros.LayerTree", // implicit conversion for implicit class
    "zio.internal.macros.StringUtils", // no final, implicit conversion for implicit class
    "zio.internal.macros.ZLayerDerivationMacros", // Expr[...]
    "zio.metrics.MetricPair", // private type alias
    "zio.stm.STM", // zio.BuildFrom vs BuildFromCompat.this.BuildFrom
    "zio.stm.ZSTM", // protected vs private[this]
    "zio.stream.Deflate", // private[stream]
    "zio.stream.Gunzip", // private[stream]
    "zio.stream.Gzip", // private[stream]
    "zio.stream.Inflate", // private[stream]
    "zio.stream.Take", // no final for case class
    "zio.stream.ZChannel", // zio.EnvironmentTag vs VersionSpecific.this.EnvironmentTag
    "zio.stream.ZStream", // R is Nothing
  )
)