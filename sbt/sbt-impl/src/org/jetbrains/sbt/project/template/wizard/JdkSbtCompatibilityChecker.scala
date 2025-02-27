package org.jetbrains.sbt.project.template.wizard

import com.intellij.util.lang.JavaVersion

import scala.math.Ordered.orderingToOrdered
import org.jetbrains.plugins.scala.project.Version

object JdkSbtCompatibilityChecker {

  private val JDK_8 = JavaVersion.compose(8)
  /**
   * It's a hardcoded minimum working versions table from [[https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html#tooling-compatibility-table]] <br>
   * In the future, this table should be automatically updated, for example, by a scheduled CI job.
   */
  private val compatibilityTable: Map[JavaVersion, Version] = Map(
    JavaVersion.compose(23) -> Version("1.9.0"),
    JavaVersion.compose(21) -> Version("1.9.0"),
    JavaVersion.compose(17) -> Version("1.6.0"),
    JavaVersion.compose(11) -> Version("1.1.0"),
    JDK_8  -> Version("1.0.0")
  )

  /**
   * Determines the minimum compatible sbt version required for a specific JDK version.
   * It's done based on hardcoded [[org.jetbrains.sbt.project.template.wizard.JdkSbtCompatibilityChecker.compatibilityTable]].
   * If the JDK is not listed in the table (it is either below 1.8 or greater than or equal to 24), then the versions are considered as compatible.
   *
   * @return <code>Option</code> containing the minimum required sbt version if the given combination of JDK and sbt is incompatible. <br>
   *         <code>None</code> if the provided versions are compatible.
   */
  def getMinimumSbtToJdkCompatibleVersion(jdk: JavaVersion, sbtVersion: Version): Option[Version] = {
    if (jdk >= JavaVersion.compose(24)) return None

    val nearestCompatibleJdk = compatibilityTable.keys.filter(_ <= jdk).maxOption

    nearestCompatibleJdk
      .map(compatibilityTable)
      .filter(requiredSbtVersion => sbtVersion < requiredSbtVersion)
  }

  /**
   * @param strict if set to <code>true</code>, JDK versions below 1.8 or greater than or equal to 24 are treated as incompatible
   */
  def isSbtAndJdkVersionCompatible(jdk: JavaVersion, sbtVersion: Version, strict: Boolean = false): Boolean = {
    val isOutsideOfRange = jdk < JDK_8 || jdk >= JavaVersion.compose(24)
    if (strict && isOutsideOfRange) false
    else {
      getMinimumSbtToJdkCompatibleVersion(jdk, sbtVersion).isEmpty
    }
  }

  /**
   * Determines the lowest JDK version that is incompatible with the given sbt version.
   * It's done based on hardcoded [[org.jetbrains.sbt.project.template.wizard.JdkSbtCompatibilityChecker.compatibilityTable]].
   *
   * For example - for sbt version 1.5.0, this method will return JDK 17, meaning that sbt 1.5.0 is only compatible with
   *    JDK versions lower than 17.
   *
   * @return <code>Option</code> containing the lowest incompatible JDK for the given sbt version <br>
   *         <code>None</code> if the versions are compatible
   */
  def getLowestIncompatibleJdkForSbt(jdk: JavaVersion, sbtVersion: Version): Option[JavaVersion] = {
    val isCompatible = isSbtAndJdkVersionCompatible(jdk, sbtVersion)
    if (isCompatible) None
    else {
      val higherSbtVersions = compatibilityTable.filter { case (_, v) => sbtVersion < v }
      higherSbtVersions.keys.minOption
    }
  }
}
