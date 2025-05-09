package org.jetbrains.sbt.project

import com.intellij.util.lang.JavaVersion
import junitparams.{JUnitParamsRunner, Parameters}
import org.jetbrains.sbt.SbtVersion
import org.jetbrains.sbt.project.template.wizard.JdkSbtCompatibilityChecker
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith

import scala.annotation.unused

@RunWith(classOf[JUnitParamsRunner])
class JdkSbtCompatibilityCheckerTest {

  @unused("used reflectively by the @Parameters annotation")
  private def testDataMinimumSbtToJdkCompatibleVersion: Array[AnyRef] = Array(
    Array(JavaVersion.compose(6), SbtVersion("1.0.0"), None), // not present in the compatibility hardcoded table
    Array(JavaVersion.compose(8), SbtVersion("1.0.0"), None),
    Array(JavaVersion.compose(11),  SbtVersion("1.0.4"), Some(SbtVersion("1.1.0"))),
    Array(JavaVersion.compose(11),  SbtVersion("1.1.1"), None),
    Array(JavaVersion.compose(18),  SbtVersion("1.6.5"), None),
    Array(JavaVersion.compose(18),  SbtVersion("1.5.5"), Some(SbtVersion("1.6.0"))),
    Array(JavaVersion.compose(22),  SbtVersion("1.8.0"), Some(SbtVersion("1.9.0"))),
    Array(JavaVersion.compose(23),  SbtVersion("1.8.5"), Some(SbtVersion("1.9.0"))),
    Array(JavaVersion.compose(23),  SbtVersion("1.9.2"), None),
    Array(JavaVersion.compose(25),  SbtVersion("1.9.0"), None) // not present in the compatibility hardcoded table
  )

  @Test
  @Parameters(method = "testDataMinimumSbtToJdkCompatibleVersion")
  def testMinimumSbtToJdkCompatibleVersion(jdk: JavaVersion, sbt: SbtVersion, expectedResult: Option[SbtVersion]): Unit = {
    val minimumCompatibleVersion = JdkSbtCompatibilityChecker.getMinimumSbtToJdkCompatibleVersion(jdk, sbt)
    assertEquals(expectedResult, minimumCompatibleVersion)
  }

  @unused("used reflectively by the @Parameters annotation")
  private def testDataHighestCompatibleJdkForSbt: Array[AnyRef] = Array(
    Array(JavaVersion.compose(6), SbtVersion("1.0.0"), None), // not present in the compatibility hardcoded table
    Array(JavaVersion.compose(8), SbtVersion("1.0.0"), None),
    Array(JavaVersion.compose(11),  SbtVersion("1.0.4"), Some(JavaVersion.compose(10))),
    Array(JavaVersion.compose(11),  SbtVersion("1.1.1"), None),
    Array(JavaVersion.compose(18),  SbtVersion("1.6.5"), None),
    Array(JavaVersion.compose(18),  SbtVersion("1.5.5"), Some(JavaVersion.compose(16))),
    Array(JavaVersion.compose(22),  SbtVersion("1.8.0"), Some(JavaVersion.compose(20))),
    Array(JavaVersion.compose(23),  SbtVersion("1.8.5"), Some(JavaVersion.compose(20))),
    Array(JavaVersion.compose(23),  SbtVersion("1.9.2"), None),
    Array(JavaVersion.compose(25),  SbtVersion("1.9.0"), None) // not present in the compatibility hardcoded table
  )

  @Test
  @Parameters(method = "testDataHighestCompatibleJdkForSbt")
  def testHighestCompatibleJdkForSbt(jdk: JavaVersion, sbt: SbtVersion, expectedResult: Option[JavaVersion]): Unit = {
    val highestCompatibleVersion = JdkSbtCompatibilityChecker.getHighestCompatibleJdkForSbt(jdk, sbt)
    assertEquals(expectedResult, highestCompatibleVersion)
  }
}
