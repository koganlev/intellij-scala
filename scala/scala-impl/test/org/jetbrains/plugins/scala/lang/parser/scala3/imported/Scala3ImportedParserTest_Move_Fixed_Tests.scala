package org.jetbrains.plugins.scala.lang.parser.scala3.imported

import com.intellij.openapi.project.Project
import com.intellij.psi.impl.DebugUtil.psiToString
import junit.framework.{Test, TestCase, TestSuite}
import org.jetbrains.plugins.scala.extensions.{PathExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.util.TestUtils
import org.junit.Ignore

import java.nio.file.{Files, Path, Paths, StandardCopyOption, StandardOpenOption}

@Ignore("for local running only")
class Scala3ImportedParserTest_Move_Fixed_Tests extends TestCase

object Scala3ImportedParserTest_Move_Fixed_Tests {
  /**
   * Run this main method to move all scala 3 test files that generate no PsiErrorElements anymore to
   * the succeeding directory
   *
   * Use this if you have made progress in the parser and fixed files that produced PsiErrorElement
   * and, now, make Scala3ImportedParserTest_Fail fail. In this case this method will move those
   * into the succeeding folder, so they can fail if someone screws anything up in the parser, that
   * had previously worked.
   */
  def suite(): TestSuite = {
    val suite = new TestSuite
    suite.addTest(new Scala3ImportedParserTest_Move_Fixed_Tests(Scala3ImportedParserTestConfig.LTS))
    suite.addTest(new Scala3ImportedParserTest_Move_Fixed_Tests(Scala3ImportedParserTestConfig.Newest))
    suite
  }

  @Ignore("for local running only")
  class Scala3ImportedParserTest_Move_Fixed_Tests(config: Scala3ImportedParserTestConfig)
    extends Scala3ImportedParserTestBase(config, runOnSucceedDirectory = false) {

    protected override def transform(testName: String, fileText: String, project: Project): String = {
      val (errors, file) = findErrorElements(fileText, project)
      val interlaced = findInterlacedRanges(file, testName)

      if (errors.isEmpty && interlaced.isEmpty) {
        val rootTestDataPath = Path.of(TestUtils.getTestDataPath)
        val from = rootTestDataPath / config.failDataDirectory / s"$testName.test"
        val to = rootTestDataPath / config.successDataDirectory / s"$testName.test"

        println("Move " + from)
        println("  to " + to)
        Files.move(
          from,
          to,
          StandardCopyOption.REPLACE_EXISTING
        )

        val psiTreeText = psiToString(file, true).replace(": " + file.name, "")
        Files.writeString(to, psiTreeText, StandardOpenOption.APPEND)
      }
      // all files of failing test have no ast to test against, so return an empty string here
      ""
    }

    override protected def transformExpectedResult(text: String): String = {
      assert(text.isEmpty, "Expected result should be empty")
      text.trim
    }
  }
}
