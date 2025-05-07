package org.jetbrains.plugins.scala.lang.parser.scala3.imported

import com.intellij.openapi.project.Project
import com.intellij.psi.impl.DebugUtil.psiToString
import junit.framework.{Test, TestCase, TestSuite}
import org.jetbrains.plugins.scala.extensions.{PathExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.util.TestUtils
import org.junit.Ignore

import java.nio.file.{Files, Path}

@Ignore("for local running only")
class Scala3ImportedParserTest_RegeneratePsi extends TestCase

object Scala3ImportedParserTest_RegeneratePsi {
  /**
   * Run this main method to move all scala 3 test files that generate no PsiErrorElements anymore to
   * the succeeding directory
   *
   * Use this if you have made progress in the parser and fixed files that produced PsiErrorElement
   * and, now, make Scala3ImportedParserTest_Fail fail. In this case this method will move those
   * into the succeeding folder, so they can fail if someone screws anything up in the parser, that
   * had previously worked.
   */
  def suite(): Test = {
    val suite = new TestSuite
    suite.addTest(new Scala3ImportedParserTest_RegeneratePsi(Scala3ImportedParserTestConfig.LTS))
    suite.addTest(new Scala3ImportedParserTest_RegeneratePsi(Scala3ImportedParserTestConfig.Newest))
    suite
  }

  @Ignore("for local running only")
  class Scala3ImportedParserTest_RegeneratePsi(config: Scala3ImportedParserTestConfig)
    extends Scala3ImportedParserTestBase_UsedAsScript(config, runOnSucceedDirectory = true) {

    protected override def transform(testName: String, fileText: String, project: Project): String = {
      val (errors, file) = findErrorElements(fileText, project)
      val interlaced = findInterlacedRanges(file, testName)

      if (errors.isEmpty && interlaced.isEmpty) {
        val rootTestDataPath = Path.of(TestUtils.getTestDataPath)
        val path = rootTestDataPath / config.successDataDirectory / s"$testName.test"

        println("Regenerate " + path)
        val psiTreeText = psiToString(file, true).replace(": " + file.name, "")
        val content = Files.readString(path)
        val searchString = "-----\n"
        val idx = content.indexOf(searchString).ensuring(_ >= 0)
        val newContent = content.substring(0, idx + searchString.length) + psiTreeText
        Files.writeString(path, newContent)
      }

      ""
    }

    override protected def transformExpectedResult(text: String): String = {
      ""
    }
  }
}
