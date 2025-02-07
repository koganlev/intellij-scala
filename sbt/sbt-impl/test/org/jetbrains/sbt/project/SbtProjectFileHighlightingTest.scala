package org.jetbrains.sbt.project

import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.{PsiFile, PsiManager}
import junit.framework.TestCase.{assertFalse, assertNotNull, assertTrue}
import org.jetbrains.plugins.scala.SlowTests
import org.jetbrains.plugins.scala.util.TestUtils
import org.junit.experimental.categories.Category

import java.nio.file.Path

@Category(Array(classOf[SlowTests]))
abstract class SbtProjectFileHighlightingTestBase(sbtVersion: String) extends SbtExternalSystemImportingTestLike {

  override protected def getTestProjectPath: String =
    s"${TestUtils.getTestDataPath}/sbt/projects/sbtProjectFileHighlighting_$sbtVersion"

  override protected def projectJdkLanguageLevel: LanguageLevel = sbtVersion match {
    case "0_13" => LanguageLevel.JDK_1_8
    case "1" => LanguageLevel.JDK_17
    case _ => throw new IllegalArgumentException(s"Unsupported sbt version: $sbtVersion")
  }

  override def setUp(): Unit = {
    super.setUp()
    SbtProjectResolver.processOutputOfLatestStructureDump = ""
    SbtCachesSetupUtil.setupCoursierAndIvyCache(getProject)
  }

  def testSbtProjectFileHighlighting(): Unit = {
    val pluginsSbtPath = Path.of(getTestProjectPath, "project", "plugins.sbt")
    val buildSbtPath = Path.of(getTestProjectPath, "build.sbt")

    val shouldHighlightPluginsSbtBefore = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(pluginsSbtPath))
    val shouldHighlightBuildSbtBefore = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(buildSbtPath))

    assertFalse("plugins.sbt should not be highlighted before the project has been imported", shouldHighlightPluginsSbtBefore)
    assertFalse("build.sbt should not be highlighted before the project has been imported", shouldHighlightBuildSbtBefore)
    assertFalse("The sbt project should not have been imported yet", SbtProjectImportStateService.instance(getProject).isImported(findPsiFile(buildSbtPath)))

    importProject(false)

    val shouldHighlightPluginsSbtAfter = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(pluginsSbtPath))
    val shouldHighlightBuildSbtAfter = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(buildSbtPath))

    assertTrue("plugins.sbt should be highlighted after the project has been imported", shouldHighlightPluginsSbtAfter)
    assertTrue("build.sbt should be highlighted after the project has been imported", shouldHighlightBuildSbtAfter)
    assertTrue("The sbt project should have been imported", SbtProjectImportStateService.instance(getProject).isImported(findPsiFile(buildSbtPath)))
  }

  private def findPsiFile(path: Path): PsiFile = {
    // It's necessary to refresh the virtual file system to get the up-to-date VirtualFile/PsiFile instances before
    // and after project import.
    val virtualFile = VfsUtil.findFile(path, true)
    assertNotNull(s"Could not find virtual file for path: $path", virtualFile)
    val manager = PsiManager.getInstance(getProject)
    val psiFile = manager.findFile(virtualFile)
    assertNotNull(s"Could not find psi file for virtual file: $virtualFile", psiFile)
    psiFile
  }
}

class SbtProjectFileHighlightingTest_0_13 extends SbtProjectFileHighlightingTestBase("0_13")

class SbtProjectFileHighlightingTest_1 extends SbtProjectFileHighlightingTestBase("1")
