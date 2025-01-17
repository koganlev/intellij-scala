package org.jetbrains.plugins.scala.refactoring.move.directory

import org.jetbrains.plugins.scala.extensions.PathExt
import org.jetbrains.plugins.scala.util.runners.{MultipleScalaVersionsRunner, RunWithScalaVersions, TestScalaVersion}
import org.junit.runner.RunWith

import java.nio.file.Path

@RunWith(classOf[MultipleScalaVersionsRunner])
@RunWithScalaVersions(Array(
  TestScalaVersion.Scala_3_Latest,
))
class ScalaMoveDirectoryWithClassesTest_Scala3 extends ScalaMoveDirectoryWithClassesTestBase {
  override protected def getTestDataRoot: Path = super.getTestDataRoot / "scala3"

  // wildcard import in scala 3 is `*` instead of `_` so `after` directory is a bit different
  def testMovePackage(): Unit = doMovePackageTest()

  def testMovePackageWithToplevelFun(): Unit = doMovePackageTest("pack1.pack2", "pack1.pack3")

  def testRenamePackageWithMixedToplevelDefs(): Unit = doRenamePackageTest("pack1.pack2", "pack0.pack1.pack2")

  def testRenamePackageWithMultipleToplevelDefs(): Unit = doRenamePackageTest("pack1.pack2", "pack1.pack2.pack3")
}
