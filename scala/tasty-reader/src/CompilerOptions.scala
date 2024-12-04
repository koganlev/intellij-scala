package org.jetbrains.plugins.scala.tasty.reader

/* TODO Combine `decompiler` and `tasty-reader` modules or add a third module as a dependency.
        Use a "result data" class instead of `sourceNameAndText` and tuples.
        Move `compilerOptions` to `ScFile`, next to `isCompiled`. */
case class CompilerOptions(kindProjector: Boolean)

object CompilerOptions {
  val Default = CompilerOptions(kindProjector = false)
}
