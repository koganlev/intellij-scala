package org.jetbrains.sbt

import org.jetbrains.plugins.scala.project.Version

final case class SbtVersion(value: Version) extends MinorVersionGenerator[Version] {
  override def minor: String = value.presentation

  override def generateNewVersion(version: String): Option[Version] = Some(Version(version))

  override def toString: String = minor

  lazy val binaryVersion: Version = SbtVersion.standardBinaryVersion(value)

  def isSbt2: Boolean = minor.startsWith("2")

  def inRange(atLeast: SbtVersion, lessThan: SbtVersion): Boolean =
    value.inRange(atLeast.value, lessThan.value)

  def inRange(atLeast: Version, lessThan: Version): Boolean =
    value.inRange(atLeast, lessThan)
}

object SbtVersion {
  def apply(value: String): SbtVersion = new SbtVersion(Version(value))

  implicit val sbtVersionOrdering: Ordering[SbtVersion] = Ordering.by(_.value)

  object Latest {
    import org.jetbrains.sbt.buildinfo.BuildInfo

    val Sbt_0_13: SbtVersion = SbtVersion(BuildInfo.sbtLatest_0_13)
    val Sbt_1: SbtVersion = SbtVersion(BuildInfo.sbtLatest_1)

    val Sbt_LatestIncludingUnreleased: SbtVersion = SbtVersion("2.0.0-M3")

    val AllSbt1: Seq[SbtVersion] = Seq(
      SbtVersion("1.0.4"),
      SbtVersion("1.1.6"),
      SbtVersion("1.2.8"),
      SbtVersion("1.3.13"),
      SbtVersion("1.4.9"),
      SbtVersion("1.5.8"),
      SbtVersion("1.6.2"),
      SbtVersion("1.7.3"),
      SbtVersion("1.8.3"),
      SbtVersion("1.9.9"),
      SbtVersion("1.10.7")
    )
  }

  /**
   * @return Binary sbt version according to the default sbt behavior:
   *         - 0.13 for all 0.13.x versions
   *         - 1.0 for all 1.x.y versions
   *         - 2.0 for all 2.x.y versions
   */
  private def standardBinaryVersion(sbtVersion: Version): Version = {
    // 1.0.0 milestones are regarded as not binary compatible by sbt
    if ((sbtVersion ~= Version("1.0.0")) && sbtVersion.presentation.contains("-M"))
      sbtVersion
    // sbt uses binary version x.0 for [x.0,x+1.0]
    else if (sbtVersion.major(1) >= Version("1")) {
      val major = sbtVersion.major(1).presentation
      Version(s"$major.0")
    }
    else
      sbtVersion.major(2)
  }
}