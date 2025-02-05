package org.jetbrains.plugins.scala.lang.resolveSemanticDb

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestCase
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.lang.resolveSemanticDb.ComparisonTestBase.sourcePath
import org.jetbrains.plugins.scala.util.TestUtils
import org.junit.Assert.assertTrue

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}

abstract class ComparisonTestBase extends ScalaLightCodeInsightFixtureTestCase {

  def doTest(testName: String, succeeds: Boolean): Unit

  protected def setupFiles(testName: String): Seq[PsiFile] = {
    val testDirPath = sourcePath.resolve(testName)
    val testFilePath = sourcePath.resolve(testName + ".scala")
    val (source, sourceBasePath) =
      if (testDirPath.isDirectory) {
        (testDirPath, testDirPath)
      } else {
        assertTrue(s"Test file does not exist: $testFilePath", testFilePath.exists)
        assertTrue(s"Test file is not a regular file: $testFilePath", testFilePath.isRegularFile)
        (testFilePath, sourcePath)
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

object ComparisonTestBase {
  def testPathBase: Path = Paths.get(TestUtils.getTestDataPath, "lang", "resolveSemanticDb")
  def sourcePath: Path = testPathBase.resolve("source")
  def outPath: Path = testPathBase.resolve("out")
}