package org.jetbrains.plugins.scala.lang.surroundWith

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler
import com.intellij.lang.Language
import com.intellij.lang.surroundWith.Surrounder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.{FileEditorManager, OpenFileDescriptor}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.{PsiFile, PsiFileFactory}
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase.assertNotNull
import junitparams.naming.TestCaseName
import junitparams.{JUnitParamsRunner, Parameters}
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.surroundWith.descriptors.ScalaSurroundDescriptors
import org.jetbrains.plugins.scala.util.TestUtils
import org.jetbrains.plugins.scala.{FileSetTests, Scala3Language, ScalaLanguage}
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.regex.Pattern
import scala.annotation.unused

@RunWith(classOf[JUnitParamsRunner])
@Category(Array(classOf[FileSetTests]))
abstract class SurroundWithTestBase(relativeTestDataPath: Path, language: Language) extends LightJavaCodeInsightFixtureTestCase {

  private val FilePartsSeparatorPattern: Pattern = Pattern.compile("\\n?(?m)^-{4,}\\n?")

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
  def surroundWith(@unused("used reflectively by the @TestCaseName annotation") testName: String, testFile: Path): Unit = {
    runTest(testFile)
  }

  private def runTest(testFile: Path): Unit = {
    val testName = testFile.getFileName.toString
    val fileText = StringUtil.convertLineSeparators(Files.readString(testFile, StandardCharsets.UTF_8))

    val fileParts = parseTestFileText(fileText)

    assertTrue("Test file should have at least two sections separated with ----", fileParts.sizeIs > 1)

    val inputRaw = fileParts.head
    val expectedResultRaw = fileParts.last

    val testNameWithoutDot = testName.split("\\.").head

    val actualResult = transform(testNameWithoutDot, inputRaw, getProject).trim
    val expectedResult = transformExpectedResult(expectedResultRaw).trim

    assertEquals(expectedResult, actualResult)
  }

  private def doSurround(project: Project, file: PsiFile, surrounder: Surrounder, startSelection: Int, endSelection: Int): Unit = {
    val fileEditorManager = FileEditorManager.getInstance(project)
    try {
      val editor = fileEditorManager.openTextEditor(new OpenFileDescriptor(project, file.getVirtualFile, 0), false)
      assertNotNull(editor)
      editor.getSelectionModel.setSelection(startSelection, endSelection)
      SurroundWithHandler.invoke(project, editor, file, surrounder)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally fileEditorManager.closeFile(file.getVirtualFile)
  }

  private def transform(@unused testName: String, fileText: String, project: Project): String = {
    val res = SurroundWithTestUtil.prepareFile(fileText)
    val psiFile = createLightFile(res._1, project)
    val surrounder = ScalaSurroundDescriptors.getSurroundDescriptors.head.getSurrounders
    val runnable: Runnable = () => doSurround(project, psiFile, surrounder(res._4), res._2, res._3)
    WriteCommandAction.runWriteCommandAction(project, runnable)
    psiFile.getText
  }

  private def transformExpectedResult(text: String): String = SurroundWithTestUtil.prepareExpectedResult(text)

  protected def createLightFile(@NonNls text: String, project: Project): PsiFile =
    PsiFileFactory.getInstance(project).createFileFromText("dummy.scala", language, text)

  private def parseTestFileText(text: String): List[String] =
    FilePartsSeparatorPattern.split(text, -1).toList

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

class SurroundWithTest_Scala_2 extends SurroundWithTestBase(Path.of("surroundWith", "data", "2"), ScalaLanguage.INSTANCE)

class SurroundWithTest_Scala_3 extends SurroundWithTestBase(Path.of("surroundWith", "data", "3"), Scala3Language.INSTANCE) {
  override def setUp(): Unit = {
    super.setUp()
    CodeStyle.getSettings(getProject).getCustomSettings(classOf[ScalaCodeStyleSettings]).USE_SCALA3_INDENTATION_BASED_SYNTAX = true
  }
}
