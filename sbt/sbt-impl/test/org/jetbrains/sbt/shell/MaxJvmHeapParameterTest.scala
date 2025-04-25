package org.jetbrains.sbt.shell

import org.jetbrains.sbt.project.settings.SbtExecutionSettings
import org.jetbrains.sbt.{JvmMemorySize, SbtVersion}
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.{Test, TestInfo}

import java.nio.file.Files
import scala.util.Using

class MaxJvmHeapParameterTest {

  private val hiddenDefaultSize = JvmMemorySize.Megabytes(1500)
  private val hiddenDefaultParam = "-Xmx" + hiddenDefaultSize
  private val hardcoded = List("-Dsbt.supershell=false", "-Djdk.console=java.base")

  private def buildParamSeq(userOpts: String*)(jvmOpts: String*)(implicit testInfo: TestInfo): Seq[String] = {
    import org.jetbrains.sbt.PathTestUtil._

    Using.resource(Files.createTempDirectory(s"maxHeapJvmParamTest-${testInfo.getDisplayName}")) { workingDir =>
      if (jvmOpts.nonEmpty) {
        val jvmOptsFile = workingDir.resolve(".jvmopts")
        Files.writeString(jvmOptsFile, jvmOpts.mkString("\n"))
      }

      val settings = new SbtExecutionSettings(
        realProjectPath = null,
        vmExecutable = null,
        vmOptions = userOpts,
        sbtOptions = List.empty,
        hiddenDefaultMaxHeapSize = hiddenDefaultSize,
        customLauncher = null,
        customSbtStructureFile = null,
        jdk = null,
        resolveClassifiers = false,
        resolveSbtClassifiers = false,
        useShellForImport = false ,
        shellDebugMode = false,
        preferScala2 = true,
        userSetEnvironment = Map.empty,
        passParentEnvironment = false,
        useSeparateCompilerOutputPaths = false,
        separateProdTestSources = false,
        sbtVersion = SbtVersion.Latest.Sbt_1
      )

      SbtProcessManager.buildVMParameters(settings, workingDir.toFile, List.empty)
    }
  }

  /*
    has userOpts xmx =>
       use xmx from userOpts
    has no userOpts xmx =>
       max(hidden default xmx, xms aus jvmopts
   */

  @Test
  def userSettingsSmallerThanHiddenDefault(implicit testInfo: TestInfo): Unit = {
    assertEquals(
      hardcoded ++ Seq("-Xmx4g", "-Xms4g", "-Xmx1g"),
      buildParamSeq("-Xmx1g")("-Xmx4g", "-Xms4g")
    )
  }

  @Test
  def userSettingsGreaterThanHiddenDefault(implicit testInfo: TestInfo): Unit = {
    assertEquals(
      hardcoded ++ Seq("-Xmx4g", "-Xms4g", "-Xmx2g"),
      buildParamSeq("-Xmx2g")("-Xmx4g", "-Xms4g")
    )
  }

  @Test
  def noSettings(implicit testInfo: TestInfo): Unit = {
    assertEquals(
      hiddenDefaultParam +: hardcoded,
      buildParamSeq()()
    )
  }

  @Test
  def noSettingsWithXmsSmallerThanDefaultParam(implicit testInfo: TestInfo): Unit = {
    assertEquals(
      hiddenDefaultParam +: hardcoded :+ "-Xms1g",
      buildParamSeq("-Xms1g")()
    )
  }

  @Test
  def noSettingsWithXmsGreaterThanDefaultParam(implicit testInfo: TestInfo): Unit = {
    assertEquals(
      hardcoded :+ "-Xms2g",
      buildParamSeq("-Xms2g")()
    )
  }
}
