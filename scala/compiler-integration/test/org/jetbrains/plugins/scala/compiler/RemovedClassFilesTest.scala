package org.jetbrains.plugins.scala.compiler

import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.plugins.scala.compiler.CompilerMessagesUtil.{assertCompilingScalaSources, assertNoErrorsOrWarnings}
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.plugins.scala.{CompilationTests_Zinc, ScalaVersion}
import org.junit.Assert.assertTrue
import org.junit.experimental.categories.Category

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

@Category(Array(classOf[CompilationTests_Zinc]))
class RemovedClassFilesTest extends ScalaCompilerTestBase with JdkVersionDiscovery {

  override protected def supportedIn(version: ScalaVersion): Boolean = version == ScalaVersion.Latest.Scala_2_13

  override protected def incrementalityType: IncrementalityType = IncrementalityType.SBT

  override protected def setUp(): Unit = {
    super.setUp()
    addFileToProjectSources("A.scala", "class A { def foo = 5 }")
    addFileToProjectSources("B.scala", "class B")
    addFileToProjectSources("C.scala", "class C")
    addFileToProjectSources("D.scala", "class D")
    addFileToProjectSources("E.scala", "class E")
  }

  def testRemoveAllClassFilesAndCompileAgain(): Unit = {
    val messages1 = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages1)
    assertCompilingScalaSources(messages1, 5)

    val classFileNames = List("A", "B", "C", "D", "E")

    val firstClassFiles = classFileNames.map(findClassFile)
    val firstTimestamps = firstClassFiles.map(Files.getLastModifiedTime(_).toMillis)

    firstClassFiles.foreach(removeFile)

    val messages2 = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages2)
    assertCompilingScalaSources(messages2, 5)

    val secondClassFiles = classFileNames.map(findClassFile)
    val secondTimestamps = secondClassFiles.map(Files.getLastModifiedTime(_).toMillis)

    val recompiled = firstTimestamps.zip(secondTimestamps).forall { case (a, b) => a < b }
    assertTrue("Not all source files were recompiled", recompiled)
  }

  def testRemoveTwoClassFilesAndCompileAgain(): Unit = {
    val messages1 = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages1)
    assertCompilingScalaSources(messages1, 5)

    val classFileNames = List("A", "B", "C", "D", "E")

    val firstClassFiles = classFileNames.map(findClassFile)
    val firstTimestamps = firstClassFiles.map(Files.getLastModifiedTime(_).toMillis)

    removeFile(firstClassFiles(2)) // delete C.class

    val messages2 = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages2)
    assertCompilingScalaSources(messages2, 1)

    val secondClassFiles = classFileNames.map(findClassFile)
    val secondTimestamps = secondClassFiles.map(Files.getLastModifiedTime(_).toMillis)

    val correct = firstTimestamps.zip(secondTimestamps).zipWithIndex.forall {
      case ((x, y), 2) => x < y
      case ((x, y), _) => x == y
    }
    assertTrue(correct)
  }

  def testRemoveClassFileAndEditDependentSource(): Unit = {
    val messages1 = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages1)
    assertCompilingScalaSources(messages1, 5)

    val classFileNames = List("A", "B", "C", "D", "E")

    val firstClassFiles = classFileNames.map(findClassFile)
    val firstTimestamps = firstClassFiles.map(Files.getLastModifiedTime(_).toMillis)

    removeFile(firstClassFiles.head) // delete A.class

    val bSourcePath = getSourceRootDir.toNioPath.resolve(Path.of("B.scala"))
    val bSource = VfsUtil.findFileByIoFile(bSourcePath.toFile, true)
    inWriteAction {
      VfsUtil.saveText(bSource, """class B { new A().foo }""")
    }

    val messages2 = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages2)
    assertCompilingScalaSources(messages2, 2)

    val secondClassFiles = classFileNames.map(findClassFile)
    val secondTimestamps = secondClassFiles.map(Files.getLastModifiedTime(_).toMillis)

    val correct = firstTimestamps.zip(secondTimestamps).zipWithIndex.forall {
      case ((x, y), 0) => x < y
      case ((x, y), 1) => x < y
      case ((x, y), _) => x == y
    }
    assertTrue(correct)
  }
}
