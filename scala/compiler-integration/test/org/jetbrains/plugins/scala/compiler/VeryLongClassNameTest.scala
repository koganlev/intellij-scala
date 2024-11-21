package org.jetbrains.plugins.scala.compiler

import org.jetbrains.plugins.scala.compiler.CompilerMessagesUtil.{assertCompilingScalaSources, assertNoErrorsOrWarnings}
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.{CompilationTests, ScalaVersion}
import org.junit.experimental.categories.Category

import scala.jdk.CollectionConverters._

@Category(Array(classOf[CompilationTests]))
class VeryLongClassNameTest extends ScalaCompilerTestBase with JdkVersionDiscovery {

  override protected def supportedIn(version: ScalaVersion): Boolean = version == ScalaVersion.Latest.Scala_2_13

  override protected def incrementalityType: IncrementalityType = IncrementalityType.SBT

  def testVertLongClassFileName(): Unit = {
    addFileToProjectSources("LongNames.scala",
      """object LongNames {
        |  object OuterLevelWithVeryVeryVeryLongClassName1 {
        |    object OuterLevelWithVeryVeryVeryLongClassName2 {
        |      object OuterLevelWithVeryVeryVeryLongClassName3 {
        |        object OuterLevelWithVeryVeryVeryLongClassName4 {
        |          object OuterLevelWithVeryVeryVeryLongClassName5 {
        |            object OuterLevelWithVeryVeryVeryLongClassName6 {
        |              object OuterLevelWithVeryVeryVeryLongClassName7 {
        |                object OuterLevelWithVeryVeryVeryLongClassName8 {
        |                  object OuterLevelWithVeryVeryVeryLongClassName9 {
        |                    object OuterLevelWithVeryVeryVeryLongClassName10 {
        |                      object OuterLevelWithVeryVeryVeryLongClassName11 {
        |                        object OuterLevelWithVeryVeryVeryLongClassName12 {
        |                          object OuterLevelWithVeryVeryVeryLongClassName13 {
        |                            object OuterLevelWithVeryVeryVeryLongClassName14 {
        |                              object OuterLevelWithVeryVeryVeryLongClassName15 {
        |                                object OuterLevelWithVeryVeryVeryLongClassName16 {
        |                                  object OuterLevelWithVeryVeryVeryLongClassName17 {
        |                                    object OuterLevelWithVeryVeryVeryLongClassName18 {
        |                                      object OuterLevelWithVeryVeryVeryLongClassName19 {
        |                                        object OuterLevelWithVeryVeryVeryLongClassName20 {
        |                                          case class MalformedNameExample(x: Int)
        |                                        }}}}}}}}}}}}}}}}}}}}
        |}
        |""".stripMargin)

    val messages = compiler.make().asScala.toSeq
    assertNoErrorsOrWarnings(messages)
    assertCompilingScalaSources(messages, 1)

    findClassFile("LongNames$OuterLevelWithVeryVeryVeryLongClassName1$OuterLe$$$$33a930f152b194d33bc475b527dab5d7$$$$yLongClassName19$OuterLevelWithVeryVeryVeryLongClassName20$MalformedNameExample$")
    findClassFile("LongNames$OuterLevelWithVeryVeryVeryLongClassName1$OuterLe$$$$33a930f152b194d33bc475b527dab5d7$$$$yLongClassName19$OuterLevelWithVeryVeryVeryLongClassName20$MalformedNameExample")
  }
}
