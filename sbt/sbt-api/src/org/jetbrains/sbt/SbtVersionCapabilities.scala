package org.jetbrains.sbt

object SbtVersionCapabilities {

  /**
   * Since version 1.2.0, sbt supports injecting additional plugins to the sbt shell with a command.
   * This allows injecting plugins without messing with the user's global directory.
   * https://github.com/sbt/sbt/pull/4211
   */
  val AddPluginCommandVersion_1: SbtVersion = SbtVersion("1.2.0")
  val AddPluginCommandVersion_013: SbtVersion = SbtVersion("0.13.18")

  val SinceSbtVersion: SbtVersion = SbtVersion("0.13.0")

  val SinceSbtVersionShell: SbtVersion = SbtVersion("0.13.5")

  /**
   * Scala3 is only supported since sbt 1.5.0
   */
  val MinSbtVersionForScala3: SbtVersion = SbtVersion("1.5.0")

  private val SinceSlashSyntax: SbtVersion = SbtVersion("1.1.0")

  /**
   * Minimum project sbt version that is allowed version override
   */
  val MayUpgradeSbtVersion: SbtVersion = SbtVersion("0.13.0")

  def isSlashSyntaxSupported(version: SbtVersion): Boolean =
    version >= SinceSlashSyntax

  def shellImportSupported(sbtVersion: SbtVersion): Boolean =
    sbtVersion >= SbtVersionCapabilities.SinceSbtVersionShell

  def importSupported(sbtVersion: SbtVersion): Boolean =
    sbtVersion >= SbtVersionCapabilities.SinceSbtVersion

  def collectionsSeqClassFqn(sbtVersion: SbtVersion): String =
    if (sbtVersion.isSbt2)
      "scala.collection.immutable.Seq"
    else
      "scala.collection.Seq"
}
