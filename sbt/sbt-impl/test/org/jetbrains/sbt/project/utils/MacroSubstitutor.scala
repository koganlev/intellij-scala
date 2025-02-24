package org.jetbrains.sbt.project.utils

/**
 * The class is intended to be able to use variables in the expected test data, like: {{{
 *   compileOutputPath := "%PROJECT_ROOT%/target/out/jvm/scala-3.3.3/subProject1/classes"
 * }}}
 *
 * ATTENTION: try to not overuse this entity too much.
 * It's better to keep expected test data as clear as possible and not use too many different macro keys in test data.
 */
class MacroSubstitutor(val substitutions: Map[String, String]) {

  def replaceValuesWithMacro(actual: String, expected: String): String = {
    substitutions.foldLeft(actual) { case (actualCurrent, (key, value)) =>
      // for some reason, some expected test data can be null =/
      if (expected != null && expected.contains(key))
        actualCurrent.replace(value, key)
      else
        actualCurrent
    }
  }

  def replaceValuesWithMacro(actualItems: Seq[String], expectedItems: Seq[String]): Seq[String] =
    actualItems.zipWithIndex.map { case (actual, idx) =>
      val adopted = expectedItems.lift(idx).map(expected => replaceValuesWithMacro(actual, expected))
      adopted.getOrElse(actual)
    }

  def containsSomeMacro(value: String): Boolean =
    value != null && substitutions.exists { case (key, _) => value.contains(key) }
}

object MacroSubstitutor {
  object Keys {
    val ProjectRoot = "%PROJECT_ROOT%"
  }
}