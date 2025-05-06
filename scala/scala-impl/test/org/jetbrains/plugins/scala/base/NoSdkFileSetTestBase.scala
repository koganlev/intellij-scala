package org.jetbrains.plugins.scala.base

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.{PsiFile, PsiFileFactory}
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junitparams.naming.TestCaseName
import junitparams.{JUnitParamsRunner, Parameters}
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.util.TestUtils
import org.jetbrains.plugins.scala.{FileSetTests, ScalaLanguage}
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.regex.Pattern
import scala.annotation.unused

/**
 * This needs to be an abstract class, otherwise the @RunWith annotation will be ignored
 * and the test will be run as a JUnit 3 test, which will fail.
 */
@RunWith(classOf[JUnitParamsRunner])
@Category(Array(classOf[FileSetTests]))
abstract class NoSdkFileSetTestBase extends LightJavaCodeInsightFixtureTestCase {

  protected def relativeTestDataPath: Path

  protected def language: Language = ScalaLanguage.INSTANCE

  private val testDirectoryPath: Path = Path.of(TestUtils.getTestDataPath) / relativeTestDataPath

  @unused("used reflectively by the @Parameters annotation")
  private def testParameters: Array[AnyRef] =
    findTestFiles(testDirectoryPath).map { path =>
      val testName = {
        val p = FileUtil.toSystemIndependentName(testDirectoryPath.relativize(path).toString)
        val ext = path.getFileName.toString.split("\\.").lastOption.map(e => s".$e").getOrElse("")
        p.stripSuffix(ext)
      }
      Array(testName, path)
    }

  @Test
  @Parameters(method = "testParameters")
  @TestCaseName(value = "{0}")
  def fileSetTest(@unused("used reflectively by the @TestCaseName annotation") testName: String, testFile: Path): Unit = {
    val testName = testFile.getFileName.toString
    val fileText = StringUtil.convertLineSeparators(Files.readString(testFile, StandardCharsets.UTF_8))

    val fileParts = parseTestFileText(fileText)

    assertTrue("Test file should have at least two sections separated with ----", fileParts.sizeIs > 1)

    val inputRaw = fileParts.head
    val expectedResultRaw = fileParts.last

    val testNameWithoutDot = testName.split("\\.").head

    val actualResult = transform(testNameWithoutDot, inputRaw).trim
    val expectedResult = transformExpectedResult(expectedResultRaw).trim

    assertEquals(expectedResult, actualResult)
  }

  protected def transform(testName: String, fileText: String): String

  protected def transformExpectedResult(text: String): String

  protected def createLightFile(@NonNls text: String, project: Project): PsiFile =
    PsiFileFactory.getInstance(project).createFileFromText("dummy.scala", language, text)

  private def parseTestFileText(text: String): List[String] =
    NoSdkFileSetTestBase.FilePartsSeparatorPattern.split(text, -1).toList

  private def findTestFiles(path: Path): Array[Path] =
    if (path.isDirectory) path.children().toArray.flatMap(findTestFiles)
    else Array(path).filter(isTestFile)

  private def isTestFile(path: Path): Boolean = {
    val canonicalPath = path.toCanonicalPath.toString
    val name = path.getFileName.toString

    !canonicalPath.contains(".svn") &&
      !canonicalPath.contains(".cvs") &&
      name.endsWith(".test") &&
      !name.startsWith("_") &&
      name != "CVS"
  }
}

private object NoSdkFileSetTestBase {
  private final val FilePartsSeparatorPattern: Pattern = Pattern.compile("\\n?(?m)^-{4,}\\n?")
}
