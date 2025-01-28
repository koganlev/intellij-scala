package org.jetbrains.plugins.scala.lang.resolveSemanticDb

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.ScalaVersion
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestCase
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.lang.resolveSemanticDb.configurations.ReferenceComparisonTestConfig
import org.junit.Assert.assertTrue

import java.nio.charset.StandardCharsets
import java.nio.file.Path

abstract class ComparisonTestBase(config: ReferenceComparisonTestConfig) extends ScalaLightCodeInsightFixtureTestCase {
  override protected def supportedIn(version: ScalaVersion): Boolean = version == config.scalaTargetVersion

  def doTest(testName: String, succeeds: Boolean): Unit

  protected def setupFiles(testName: String): Seq[PsiFile] = {
    val testDirPath = config.sourcePath.resolve(testName)
    val testFilePath = config.sourcePath.resolve(testName + ".scala")
    val (source, sourceBasePath) =
      if (testDirPath.isDirectory) {
        (testDirPath, testDirPath)
      } else {
        assertTrue(s"Test file does not exist: $testFilePath", testFilePath.exists)
        assertTrue(s"Test file is not a regular file: $testFilePath", testFilePath.isRegularFile)
        (testFilePath, config.sourcePath)
      }

    for (filePath <- allPathsIn(source).toSeq) yield {
      myFixture.addFileToProject(
        sourceBasePath.relativize(filePath).toString,
        FileUtil.loadFile(filePath.toFile, StandardCharsets.UTF_8)
      )
    }
  }

  private def allPathsIn(path: Path): Seq[Path] = {
    if (path.isRegularFile) Seq(path)
    else if (path.isDirectory) path.children().sorted
    else Seq.empty
  }
}
