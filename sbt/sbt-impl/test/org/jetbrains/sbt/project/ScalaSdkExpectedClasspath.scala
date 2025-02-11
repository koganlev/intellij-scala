package org.jetbrains.sbt.project

import org.jetbrains.plugins.scala.ScalaVersion

case class ScalaSdkExpectedClasspath(
  classpath: Seq[String],
  extraClasspath: Seq[String]
)

object ScalaSdkExpectedClasspath {

  private def create(
    classpathText: String,
    extraClasspathText: String
  ): ScalaSdkExpectedClasspath = ScalaSdkExpectedClasspath(
    classpathText.linesIterator.filter(_.nonEmpty).toSeq,
    extraClasspathText.linesIterator.filter(_.nonEmpty).toSeq,
  )

  object Coursier {
    private def scalaLibs(scalaVersion: String): Seq[String] = Seq(
      s"org/scala-lang/scala-compiler/$scalaVersion/scala-compiler-$scalaVersion.jar",
      s"org/scala-lang/scala-library/$scalaVersion/scala-library-$scalaVersion.jar",
      s"org/scala-lang/scala-reflect/$scalaVersion/scala-reflect-$scalaVersion.jar",
    )

    private val Scala_2_13_5: ScalaSdkExpectedClasspath = ScalaSdkExpectedClasspath(
      scalaLibs("2.13.5") ++ Seq(
        "net/java/dev/jna/jna/5.3.1/jna-5.3.1.jar",
        "org/jline/jline/3.19.0/jline-3.19.0.jar",
      ),
      Nil
    )

    private val Scala_2_13_6: ScalaSdkExpectedClasspath = ScalaSdkExpectedClasspath(
      scalaLibs("2.13.6") ++ Seq(
        "net/java/dev/jna/jna/5.3.1/jna-5.3.1.jar",
        "org/jline/jline/3.19.0/jline-3.19.0.jar",
      ),
      Nil
    )

    private val Scala_2_13_14: ScalaSdkExpectedClasspath = ScalaSdkExpectedClasspath(
      scalaLibs("2.13.14") ++ Seq(
        "io/github/java-diff-utils/java-diff-utils/4.12/java-diff-utils-4.12.jar",
        "net/java/dev/jna/jna/5.14.0/jna-5.14.0.jar",
        "org/jline/jline/3.25.1/jline-3.25.1.jar",
      ),
      Nil
    )

    private val Scala_3_0_2: ScalaSdkExpectedClasspath = create(
      """org/scala-lang/scala-library/2.13.6/scala-library-2.13.6.jar
        |org/scala-lang/scala3-library_3/3.0.2/scala3-library_3-3.0.2.jar
        |com/google/protobuf/protobuf-java/3.7.0/protobuf-java-3.7.0.jar
        |net/java/dev/jna/jna/5.3.1/jna-5.3.1.jar
        |org/jline/jline-reader/3.19.0/jline-reader-3.19.0.jar
        |org/jline/jline-terminal-jna/3.19.0/jline-terminal-jna-3.19.0.jar
        |org/jline/jline-terminal/3.19.0/jline-terminal-3.19.0.jar
        |org/scala-lang/modules/scala-asm/9.1.0-scala-1/scala-asm-9.1.0-scala-1.jar
        |org/scala-lang/scala3-compiler_3/3.0.2/scala3-compiler_3-3.0.2.jar
        |org/scala-lang/scala3-interfaces/3.0.2/scala3-interfaces-3.0.2.jar
        |org/scala-lang/tasty-core_3/3.0.2/tasty-core_3-3.0.2.jar
        |org/scala-sbt/compiler-interface/1.3.5/compiler-interface-1.3.5.jar
        |org/scala-sbt/util-interface/1.3.0/util-interface-1.3.0.jar
        |""".stripMargin,
      """com/fasterxml/jackson/core/jackson-annotations/2.2.3/jackson-annotations-2.2.3.jar
        |com/fasterxml/jackson/core/jackson-core/2.9.8/jackson-core-2.9.8.jar
        |com/fasterxml/jackson/core/jackson-databind/2.2.3/jackson-databind-2.2.3.jar
        |com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.9.8/jackson-dataformat-yaml-2.9.8.jar
        |com/vladsch/flexmark/flexmark-ext-anchorlink/0.42.12/flexmark-ext-anchorlink-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-autolink/0.42.12/flexmark-ext-autolink-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-emoji/0.42.12/flexmark-ext-emoji-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-strikethrough/0.42.12/flexmark-ext-gfm-strikethrough-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-tables/0.42.12/flexmark-ext-gfm-tables-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-tasklist/0.42.12/flexmark-ext-gfm-tasklist-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-ins/0.42.12/flexmark-ext-ins-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-superscript/0.42.12/flexmark-ext-superscript-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-tables/0.42.12/flexmark-ext-tables-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-wikilink/0.42.12/flexmark-ext-wikilink-0.42.12.jar
        |com/vladsch/flexmark/flexmark-ext-yaml-front-matter/0.42.12/flexmark-ext-yaml-front-matter-0.42.12.jar
        |com/vladsch/flexmark/flexmark-formatter/0.42.12/flexmark-formatter-0.42.12.jar
        |com/vladsch/flexmark/flexmark-html-parser/0.42.12/flexmark-html-parser-0.42.12.jar
        |com/vladsch/flexmark/flexmark-jira-converter/0.42.12/flexmark-jira-converter-0.42.12.jar
        |com/vladsch/flexmark/flexmark-util/0.42.12/flexmark-util-0.42.12.jar
        |com/vladsch/flexmark/flexmark/0.42.12/flexmark-0.42.12.jar
        |nl/big-o/liqp/0.6.7/liqp-0.6.7.jar
        |org/antlr/ST4/4.0.7/ST4-4.0.7.jar
        |org/antlr/antlr-runtime/3.5.1/antlr-runtime-3.5.1.jar
        |org/antlr/antlr/3.5.1/antlr-3.5.1.jar
        |org/jsoup/jsoup/1.13.1/jsoup-1.13.1.jar
        |org/nibor/autolink/autolink/0.6.0/autolink-0.6.0.jar
        |org/scala-lang/scala3-tasty-inspector_3/3.0.2/scala3-tasty-inspector_3-3.0.2.jar
        |org/scala-lang/scaladoc_3/3.0.2/scaladoc_3-3.0.2.jar
        |org/yaml/snakeyaml/1.23/snakeyaml-1.23.jar
        |""".stripMargin,

    )

    private val Scala_3_3_3: ScalaSdkExpectedClasspath = create(
      """net/java/dev/jna/jna/5.3.1/jna-5.3.1.jar
        |org/jline/jline-reader/3.19.0/jline-reader-3.19.0.jar
        |org/jline/jline-terminal-jna/3.19.0/jline-terminal-jna-3.19.0.jar
        |org/jline/jline-terminal/3.19.0/jline-terminal-3.19.0.jar
        |org/scala-lang/modules/scala-asm/9.5.0-scala-1/scala-asm-9.5.0-scala-1.jar
        |org/scala-lang/scala-library/2.13.12/scala-library-2.13.12.jar
        |org/scala-lang/scala3-compiler_3/3.3.3/scala3-compiler_3-3.3.3.jar
        |org/scala-lang/scala3-interfaces/3.3.3/scala3-interfaces-3.3.3.jar
        |org/scala-lang/scala3-library_3/3.3.3/scala3-library_3-3.3.3.jar
        |org/scala-lang/tasty-core_3/3.3.3/tasty-core_3-3.3.3.jar
        |org/scala-sbt/compiler-interface/1.9.3/compiler-interface-1.9.3.jar
        |org/scala-sbt/util-interface/1.9.2/util-interface-1.9.2.jar
        |""".stripMargin,
      """com/fasterxml/jackson/core/jackson-annotations/2.15.1/jackson-annotations-2.15.1.jar
        |com/fasterxml/jackson/core/jackson-core/2.15.1/jackson-core-2.15.1.jar
        |com/fasterxml/jackson/core/jackson-databind/2.15.1/jackson-databind-2.15.1.jar
        |com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.15.1/jackson-dataformat-yaml-2.15.1.jar
        |com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.12.1/jackson-datatype-jsr310-2.12.1.jar
        |com/vladsch/flexmark/flexmark-ext-anchorlink/0.62.2/flexmark-ext-anchorlink-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-emoji/0.62.2/flexmark-ext-emoji-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-strikethrough/0.62.2/flexmark-ext-gfm-strikethrough-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-tasklist/0.62.2/flexmark-ext-gfm-tasklist-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-ins/0.62.2/flexmark-ext-ins-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-superscript/0.62.2/flexmark-ext-superscript-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-wikilink/0.62.2/flexmark-ext-wikilink-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-yaml-front-matter/0.62.2/flexmark-ext-yaml-front-matter-0.62.2.jar
        |com/vladsch/flexmark/flexmark-jira-converter/0.62.2/flexmark-jira-converter-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar
        |com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar
        |nl/big-o/liqp/0.8.2/liqp-0.8.2.jar
        |org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.jar
        |org/jetbrains/annotations/15.0/annotations-15.0.jar
        |org/jsoup/jsoup/1.17.2/jsoup-1.17.2.jar
        |org/nibor/autolink/autolink/0.6.0/autolink-0.6.0.jar
        |org/scala-lang/scala3-tasty-inspector_3/3.3.3/scala3-tasty-inspector_3-3.3.3.jar
        |org/scala-lang/scaladoc_3/3.3.3/scaladoc_3-3.3.3.jar
        |org/yaml/snakeyaml/2.0/snakeyaml-2.0.jar
        |ua/co/k/strftime4j/1.0.5/strftime4j-1.0.5.jar
        |""".stripMargin,

    )

    private val Scala_3_6_2: ScalaSdkExpectedClasspath = create(
      """net/java/dev/jna/jna/5.15.0/jna-5.15.0.jar
        |org/jline/jline-native/3.27.0/jline-native-3.27.0.jar
        |org/jline/jline-reader/3.27.0/jline-reader-3.27.0.jar
        |org/jline/jline-terminal-jna/3.27.0/jline-terminal-jna-3.27.0.jar
        |org/jline/jline-terminal/3.27.0/jline-terminal-3.27.0.jar
        |org/scala-lang/modules/scala-asm/9.7.0-scala-2/scala-asm-9.7.0-scala-2.jar
        |org/scala-lang/scala-library/2.13.15/scala-library-2.13.15.jar
        |org/scala-lang/scala3-compiler_3/3.6.2/scala3-compiler_3-3.6.2.jar
        |org/scala-lang/scala3-interfaces/3.6.2/scala3-interfaces-3.6.2.jar
        |org/scala-lang/scala3-library_3/3.6.2/scala3-library_3-3.6.2.jar
        |org/scala-lang/tasty-core_3/3.6.2/tasty-core_3-3.6.2.jar
        |org/scala-sbt/compiler-interface/1.9.6/compiler-interface-1.9.6.jar
        |org/scala-sbt/util-interface/1.9.8/util-interface-1.9.8.jar
        |""".stripMargin,
      """com/fasterxml/jackson/core/jackson-annotations/2.15.1/jackson-annotations-2.15.1.jar
        |com/fasterxml/jackson/core/jackson-core/2.15.1/jackson-core-2.15.1.jar
        |com/fasterxml/jackson/core/jackson-databind/2.15.1/jackson-databind-2.15.1.jar
        |com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.15.1/jackson-dataformat-yaml-2.15.1.jar
        |com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.12.1/jackson-datatype-jsr310-2.12.1.jar
        |com/vladsch/flexmark/flexmark-ext-anchorlink/0.62.2/flexmark-ext-anchorlink-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-emoji/0.62.2/flexmark-ext-emoji-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-strikethrough/0.62.2/flexmark-ext-gfm-strikethrough-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-gfm-tasklist/0.62.2/flexmark-ext-gfm-tasklist-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-ins/0.62.2/flexmark-ext-ins-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-superscript/0.62.2/flexmark-ext-superscript-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-wikilink/0.62.2/flexmark-ext-wikilink-0.62.2.jar
        |com/vladsch/flexmark/flexmark-ext-yaml-front-matter/0.62.2/flexmark-ext-yaml-front-matter-0.62.2.jar
        |com/vladsch/flexmark/flexmark-jira-converter/0.62.2/flexmark-jira-converter-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar
        |com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar
        |com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar
        |nl/big-o/liqp/0.8.2/liqp-0.8.2.jar
        |org/antlr/antlr4-runtime/4.7.2/antlr4-runtime-4.7.2.jar
        |org/jetbrains/annotations/15.0/annotations-15.0.jar
        |org/jsoup/jsoup/1.17.2/jsoup-1.17.2.jar
        |org/nibor/autolink/autolink/0.6.0/autolink-0.6.0.jar
        |org/scala-lang/scala3-tasty-inspector_3/3.6.2/scala3-tasty-inspector_3-3.6.2.jar
        |org/scala-lang/scaladoc_3/3.6.2/scaladoc_3-3.6.2.jar
        |org/yaml/snakeyaml/2.0/snakeyaml-2.0.jar
        |ua/co/k/strftime4j/1.0.5/strftime4j-1.0.5.jar
        |""".stripMargin,
    )

    def getForVersion(scalaVersion: ScalaVersion): ScalaSdkExpectedClasspath = scalaVersion.minor match {
      case "2.13.5" => Scala_2_13_5
      case "2.13.6" => Scala_2_13_6
      case "2.13.14" => Scala_2_13_14
      case "3.0.2" => Scala_3_0_2
      case "3.3.3" => Scala_3_3_3
      case "3.6.2" => Scala_3_6_2
      case _ => throw new IllegalArgumentException(s"No expected scala sdk classpath for version: $scalaVersion (Coursier)")
    }
  }

  object Ivy {
    private val Scala_2_12_10 = ScalaSdkExpectedClasspath(
      classpath = Seq(
        "jline/jline/jars/jline-2.14.6.jar",
        "org.fusesource.jansi/jansi/jars/jansi-1.12.jar",
        "org.scala-lang.modules/scala-xml_2.12/bundles/scala-xml_2.12-1.0.6.jar",
        "org.scala-lang/scala-compiler/jars/scala-compiler-2.12.10.jar",
        "org.scala-lang/scala-library/jars/scala-library-2.12.10.jar",
        "org.scala-lang/scala-reflect/jars/scala-reflect-2.12.10.jar",
      ),
      extraClasspath = Nil
    )

    def getForVersion(scalaVersion: ScalaVersion): ScalaSdkExpectedClasspath = scalaVersion.minor match {
      case "2.12.10" => Scala_2_12_10
      case _ => throw new IllegalArgumentException(s"No expected scala sdk classpath for version: $scalaVersion (Ivy)")
    }
  }
}
