package org.jetbrains.sbt.project

import com.intellij.util.lang.JavaVersion
import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.project.template.wizard.JdkSbtCompatibilityChecker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import scala.jdk.CollectionConverters._

class JdkSbtCompatibilityCheckerTest {
  @ParameterizedTest
  @MethodSource(Array("org.jetbrains.sbt.project.JdkSbtCompatibilityCheckerTest#testDataMinimumSbtToJdkCompatibleVersion"))
  def testMinimumSbtToJdkCompatibleVersion(data: (JavaVersion, Version, Option[Version])): Unit = {
    val (jdk, sbt, expectedResult) = data
    val minimumCompatibleVersion = JdkSbtCompatibilityChecker.getMinimumSbtToJdkCompatibleVersion(jdk, sbt)
    assertEquals(expectedResult, minimumCompatibleVersion)
  }

  @ParameterizedTest
  @MethodSource(Array("org.jetbrains.sbt.project.JdkSbtCompatibilityCheckerTest#testDataLowestIncompatibleJdkForSbt"))
  def testLowestIncompatibleJdkForSbt(data: (JavaVersion, Version, Option[Version])): Unit = {
    val (jdk, sbt, expectedResult) = data
    val minimumCompatibleVersion = JdkSbtCompatibilityChecker.getLowestIncompatibleJdkForSbt(jdk, sbt)
    assertEquals(expectedResult, minimumCompatibleVersion)
  }

}

object JdkSbtCompatibilityCheckerTest {
  def testDataMinimumSbtToJdkCompatibleVersion(): java.util.stream.Stream[(JavaVersion, Version, Option[Version])] = {
    Seq(
      (JavaVersion.compose(6), Version("1.0.0"), None), // not present in the compatibility hardcoded table
      (JavaVersion.compose(8), Version("1.0.0"), None),
      (JavaVersion.compose(11),  Version("1.0.4"), Some(Version("1.1.0"))),
      (JavaVersion.compose(11),  Version("1.1.1"), None),
      (JavaVersion.compose(18),  Version("1.6.5"), None),
      (JavaVersion.compose(18),  Version("1.5.5"), Some(Version("1.6.0"))),
      (JavaVersion.compose(22),  Version("1.8.0"), Some(Version("1.9.0"))),
      (JavaVersion.compose(23),  Version("1.8.5"), Some(Version("1.9.0"))),
      (JavaVersion.compose(23),  Version("1.9.2"), None),
      (JavaVersion.compose(25),  Version("1.9.0"), None), // not present in the compatibility hardcoded table
    ).asJava.stream()
  }

  def testDataLowestIncompatibleJdkForSbt(): java.util.stream.Stream[(JavaVersion, Version, Option[JavaVersion])] = {
    Seq(
      (JavaVersion.compose(6), Version("1.0.0"), None), // not present in the compatibility hardcoded table
      (JavaVersion.compose(8), Version("1.0.0"), None),
      (JavaVersion.compose(11),  Version("1.0.4"), Some(JavaVersion.compose(11))),
      (JavaVersion.compose(11),  Version("1.1.1"), None),
      (JavaVersion.compose(18),  Version("1.6.5"), None),
      (JavaVersion.compose(18),  Version("1.5.5"), Some(JavaVersion.compose(17))),
      (JavaVersion.compose(22),  Version("1.8.0"), Some(JavaVersion.compose(21))),
      (JavaVersion.compose(23),  Version("1.8.5"), Some(JavaVersion.compose(21))),
      (JavaVersion.compose(23),  Version("1.9.2"), None),
      (JavaVersion.compose(25),  Version("1.9.0"), None), // not present in the compatibility hardcoded table
    ).asJava.stream()
  }
}
