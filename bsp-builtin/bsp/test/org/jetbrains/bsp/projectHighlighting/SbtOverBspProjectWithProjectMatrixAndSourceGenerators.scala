package org.jetbrains.bsp.projectHighlighting

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.scala.projectHighlighting.reporter.HighlightingProgressReporter
import org.jetbrains.sbt.project.ProjectStructureDsl.{contentRoots, module}
import org.jetbrains.sbt.project.utils.ProjectStructureComparisonContext
import org.jetbrains.sbt.project.utils.ProjectStructureComparisonContext.AssertionFailStrategy.CollectErrors
import org.jetbrains.sbt.project.{CollectingNotificationsListener, ExactMatch, ProjectStructureDsl, ProjectStructureMatcher}

/**
 * ! IMPORTANT
 *
 * Modules created with the `module` type instead of the custom `myModule` type
 * have duplicate content roots shared with other modules. The `module` type is used
 * to avoid adding empty content roots or sources since, in our tests, attributes
 * that are not defined are not tested.
 *
 * Once the https://youtrack.jetbrains.com/issue/SCL-20966 issue is fixed,
 * these modules should also use the `myModule` type, and their corresponding custom
 * content roots/sources should be added accordingly.
 */

class SbtOverBspProjectWithProjectMatrixAndSourceGenerators
  extends SbtOverBspProjectHighlightingLocalProjectsTestBase
    with ProjectStructureMatcher
    with ExactMatch {

  override def projectName = "sbt-projectmatrix-with-source-generators"

  override protected def importProjectDuringTestSetup: Boolean = false

  override protected val projectFileName = projectName

  override def testHighlighting(): Unit = {
    importProject(false)
    super.testHighlighting()
  }

  override protected def highlightSingleFile(
    virtualFile: VirtualFile,
    psiFile: PsiFile,
    reporter: HighlightingProgressReporter,
  ): Unit =
    doHighlightingForFile(virtualFile, psiFile, reporter)

  private def standardRootsForMatrixModule(m: module, moduleBaseName: String, scalaVersionMajor: String): Unit = {
    import m._
    contentRoots := Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/main/scala-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/src/main/scalajvm-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/src/test/scala-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/src/test/scalajvm-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-$scalaVersionMajor/resource_managed/main"
    )
    val sources = (scope: String) => Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajvm-$scalaVersionMajor"
    )

    ProjectStructureDsl.testSources := sources("test")
    ProjectStructureDsl.sources := sources("main")
  }

  private def standardRootsForMatrixModuleAndPlatform(m: module, moduleBaseName: String, scalaVersionMajor: String, platform: String): Unit = {
    import m._
    contentRoots := Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/main/scala$platform-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/src/test/scala$platform-$scalaVersionMajor",
      s"%PROJECT_ROOT%/$moduleBaseName/target/$platform-$scalaVersionMajor/resource_managed/main",
    )

    ProjectStructureDsl.testSources += s"%PROJECT_ROOT%/$moduleBaseName/src/test/scala$platform-$scalaVersionMajor"
    ProjectStructureDsl.sources += s"%PROJECT_ROOT%/$moduleBaseName/src/main/scala$platform-$scalaVersionMajor"
  }

  private def standardRootsForSharedModuleJVM(m: module, moduleBaseName: String, scope: String, scalaVersions: String*): Unit = {
    import m._

    ProjectStructureDsl.contentRoots := Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/java",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/resources",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-2",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajvm",
    ) ++ scalaVersions.map { version =>s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-$version/src_managed/$scope" }

    val sources = Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-2",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajvm",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/java",
    ) ++ scalaVersions.map { version => s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-$version/src_managed/$scope" }

    if (scope == "test")
      ProjectStructureDsl.testSources := sources
    else
      ProjectStructureDsl.sources := sources
  }

  private def standardRootsForSharedModuleBothPlatformsAndVersions(m: module, moduleBaseName: String, scope: String): Unit = {
    import m._
    contentRoots := Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/java",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/resources",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-2",
      s"%PROJECT_ROOT%/$moduleBaseName/target/js-2.11/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/js-2.12/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/js-2.13/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-2.11/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-2.12/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-2.13/src_managed/$scope"
    )
    val sources = Seq(
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/java",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala",
      s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-2",
      s"%PROJECT_ROOT%/$moduleBaseName/target/js-2.11/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/js-2.12/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/js-2.13/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-2.11/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-2.12/src_managed/$scope",
      s"%PROJECT_ROOT%/$moduleBaseName/target/jvm-2.13/src_managed/$scope",
    )

    if (scope == "test")
      ProjectStructureDsl.testSources := sources
    else
      ProjectStructureDsl.sources := sources
  }

  private def standardRootsForSharedModuleJVM(m: module, moduleBaseName: String, scope: String): Unit = {
    import m._
    contentRoots += s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajvm"
    val sources = s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajvm"
    if (scope == "test")
      ProjectStructureDsl.testSources += sources
    else
      ProjectStructureDsl.sources += sources
  }

  private def standardRootsForSharedModule(m: module, moduleBaseName: String, scope: String, scalaVersionMajor: String): Unit = {
    import m._
    contentRoots += s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-$scalaVersionMajor"
    val sources = s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scala-$scalaVersionMajor"
    if (scope == "test")
      ProjectStructureDsl.testSources += sources
    else
      ProjectStructureDsl.sources += sources
  }

  private def standardRootsForSharedModuleJS(m: module, moduleBaseName: String, scope: String): Unit = {
    import m._
    contentRoots += s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajs"
    val sources = s"%PROJECT_ROOT%/$moduleBaseName/src/$scope/scalajs"
    if (scope == "test")
      ProjectStructureDsl.testSources += sources
    else
      ProjectStructureDsl.sources += sources
  }

  private class myModule(
    name: String,
    group: Array[String] = Array.empty,
  ) extends module(name, group) {
    import ProjectStructureDsl._
    locally {
      contentRoots := Seq()
      sources := Seq()
      testSources := Seq()
      moduleDependencies := Seq()
      excluded := Seq()
      // NOTE: we don't test resources directories as they should behave similar to sources/testSources
      // We comment them out to avoid too much test data.
      // We could bring it back once it becomes essential for some use cases
      //resources := Seq()
      //testResources := Seq()
    }
  }

  //noinspection ScalaUnusedSymbol,TypeAnnotation
  def testProjectStructure(): Unit = {
    val notificationsCollector = CollectingNotificationsListener.subscribeOnWarningsAndErrors(getProject)

    importProject(false)

    import org.jetbrains.sbt.project.ProjectStructureDsl._

    val expectedProject: project = new project(projectName) {
      val sbtProjectmatrix= new myModule(projectName) {
        excluded := Seq("target", ".bsp", ".bloop")
        contentRoots := Seq(s"%PROJECT_ROOT%")

        val sources = (scope: String) => Seq(
          s"%PROJECT_ROOT%/src/$scope/scala",
          s"%PROJECT_ROOT%/src/$scope/java",
          s"%PROJECT_ROOT%/src/$scope/scala-2",
          s"%PROJECT_ROOT%/src/$scope/scala-2.12",
          s"%PROJECT_ROOT%/target/scala-2.12/src_managed/$scope",
        )

        ProjectStructureDsl.testSources := sources("test")
        ProjectStructureDsl.sources := sources("main")
      }
      val sbtProjectmatrixBuild = new myModule("sbt-projectmatrix-with-source-generators-build") {
        contentRoots := Seq(s"%PROJECT_ROOT%/project")
        ProjectStructureDsl.sources := Seq(
          s"%PROJECT_ROOT%/project/src/main/scala",
          s"%PROJECT_ROOT%/project/src/main/java",
          s"%PROJECT_ROOT%/project/src/main/scala-sbt-1.0",
          s"%PROJECT_ROOT%/project/src/main/scala-2",
          s"%PROJECT_ROOT%/project/src/main/scala-2.12",
          s"%PROJECT_ROOT%/project/target/scala-2.12/sbt-1.0/src_managed/main",
        )
      }

      val upstreamUpstream2SharedTest = new module("(upstream+upstream2)(-+_)(test+11+12)-test (shared)")
      val upstreamUpstream2Shared = new myModule("(upstream+upstream2)_(11+12) (shared)") {
        standardRootsForSharedModuleJVM(this, "upstream", "main", "2.11", "2.12", "2.13")
      }

      val upstream = new module("upstream")
      val upstream2_11 = new module("upstream2_11")
      val upstream2_12 = new module("upstream2_12")

      val downstreamDownstream2SharedTest = new module("(downstream+downstream2)(-+_)(test+11+12)-test (shared)")
      val downstreamDownstream2Shared = new myModule("(downstream+downstream2)_(11+12) (shared)") {
        standardRootsForSharedModuleJVM(this, "downstream", "main", "2.11", "2.12", "2.13")
      }

      val downstream = new module("downstream")
      val downstream2_11 = new module("downstream2_11")
      val downstream2_12 = new module("downstream2_12")

      val upstreamBothShared = new myModule("upstreamBoth(Platforms+Platforms2)_(11+12) (shared)") {
        standardRootsForSharedModuleJVM(this, "upstreamBothPlatforms", "main")
      }
      val upstreamBothSharedTest = new module("upstreamBoth(Platforms+Platforms2)(-+_)(test+11+12)-test (shared)")

      val upstreamBothSharedJS2 = new myModule("upstreamBoth(Platforms2+Platforms)(_+JS2)(11+_)11 (shared)") {
        standardRootsForSharedModule(this, "upstreamBothPlatforms", "main", "2.11")
      }
      val upstreamBothSharedTestJS2 = new myModule("upstreamBoth(Platforms2+Platforms)(_+JS2)(11+_)(-+11)(test+-)test (shared)") {
        standardRootsForSharedModule(this, "upstreamBothPlatforms", "test", "2.11")
      }
      val upstreamBothSharedJS_JS2_11_12 = new myModule("upstreamBoth(Platforms+Platforms2)(_+JS+JS2)(11+12+_)(11+12) (shared)") {
        standardRootsForSharedModuleBothPlatformsAndVersions(this, "upstreamBothPlatforms", "main")
      }
      val upstreamBothSharedTestJS_JS2_11_12 = new module("upstreamBoth(Platforms+Platforms2)(-+_+JS+JS2)(test+11+12+-+_)(-+test+11+12)(test+-)test (shared)")

      val upstreamBothShared_JS2_12 = new module("upstreamBoth(Platforms2+Platforms)(_+JS2)(12+_)12 (shared)")
      val upstreamBothSharedTest_JS2_12 = new module("upstreamBoth(Platforms2+Platforms)(_+JS2)(12+_)(-+12)(test+-)test (shared)")
      val upstreamBothPlatformsSharedJS = new myModule("upstreamBothPlatformsJS (shared)") {
        standardRootsForSharedModule(this, "upstreamBothPlatforms", "main", "2.13")
      }
      val upstreamBothPlatformsSharedTestJS = new module("upstreamBothPlatforms(-+JS)(test+-)test (shared)")
      val upstreamBothPlatformsSharedJSJS2 = new myModule("upstreamBothPlatforms(JS+JS2)_(11+12) (shared)") {
        standardRootsForSharedModuleJS(this, "upstreamBothPlatforms", "main")
      }
      val upstreamBothPlatformsSharedTestJSJS2 = new module("upstreamBothPlatforms(JS+JS2)(-+_)(test+11+12)-test (shared)")

      val upstreamBothPlatforms = new module("upstreamBothPlatforms")
      val upstreamBothPlatforms2_11 = new module("upstreamBothPlatforms2_11")

      val upstreamBothPlatforms2_12 = new module("upstreamBothPlatforms2_12")
      val upstreamBothPlatformsJS = new module("upstreamBothPlatformsJS")
      val upstreamBothPlatformsJS2_11 = new module("upstreamBothPlatformsJS2_11")
      val upstreamBothPlatformsJS2_12 = new module("upstreamBothPlatformsJS2_12")

      val downstreamBothShared = new myModule("downstreamBoth(Platforms+Platforms2)_(11+12) (shared)") {
        standardRootsForSharedModuleJVM(this, "downstreamBothPlatforms", "main")
      }
      val downstreamBothSharedTest = new module("downstreamBoth(Platforms+Platforms2)(-+_)(test+11+12)-test (shared)")
      val downstreamBothSharedJS2 = new myModule("downstreamBoth(Platforms2+Platforms)(_+JS2)(11+_)11 (shared)") {
        standardRootsForSharedModule(this, "downstreamBothPlatforms", "main", "2.11")
      }
      val downstreamBothSharedTestJS2 = new module("downstreamBoth(Platforms2+Platforms)(_+JS2)(11+_)(-+11)(test+-)test (shared)")

      val downstreamBothSharedJS_JS2_11_12 = new myModule("downstreamBoth(Platforms+Platforms2)(_+JS+JS2)(11+12+_)(11+12) (shared)") {
        standardRootsForSharedModuleBothPlatformsAndVersions(this, "downstreamBothPlatforms", "main")
      }
      val downstreamBothSharedTestJS_JS2_11_12 = new module("downstreamBoth(Platforms+Platforms2)(-+_+JS+JS2)(test+11+12+-+_)(-+test+11+12)(test+-)test (shared)")
      val downstreamBothShared_JS2_12 = new myModule("downstreamBoth(Platforms2+Platforms)(_+JS2)(12+_)12 (shared)") {
        standardRootsForSharedModule(this, "downstreamBothPlatforms", "main", "2.12")
      }
      val downstreamBothSharedTest_JS2_12 = new module("downstreamBoth(Platforms2+Platforms)(_+JS2)(12+_)(-+12)(test+-)test (shared)")

      val downstreamBothPlatforms = new module("downstreamBothPlatforms")
      val downstreamBothPlatforms2_12 = new module("downstreamBothPlatforms2_12")
      val downstreamBothPlatforms2_11 = new module("downstreamBothPlatforms2_11")
      val downstreamBothPlatformsJS = new module("downstreamBothPlatformsJS")
      val downstreamBothPlatformsJS2_11 = new module("downstreamBothPlatformsJS2_11")
      val downstreamBothPlatformsJS2_12 = new module("downstreamBothPlatformsJS2_12")

      val downstreamBothPlatformSharedJSJS2 = new myModule("downstreamBothPlatforms(JS+JS2)_(11+12) (shared)") {
        standardRootsForSharedModuleJS(this, "downstreamBothPlatforms", "main")
      }
      val downstreamBothPlatformSharedTestJSJS2 = new module("downstreamBothPlatforms(JS+JS2)(-+_)(test+11+12)-test (shared)")
      val downstreamBothPlatformSharedJS = new myModule("downstreamBothPlatformsJS (shared)") {
        standardRootsForSharedModule(this, "downstreamBothPlatforms", "main", "2.13")
      }
      val downstreamBothPlatformSharedTestJS = new module("downstreamBothPlatforms(-+JS)(test+-)test (shared)")

      downstream.dependsOn(upstream, upstream, downstreamDownstream2Shared, downstreamDownstream2SharedTest)
      downstream2_11.dependsOn(upstream2_11, upstream2_11, downstreamDownstream2Shared, downstreamDownstream2SharedTest)
      downstream2_12.dependsOn(upstream2_12, upstream2_12, downstreamDownstream2Shared, downstreamDownstream2SharedTest)

      downstreamDownstream2Shared.dependsOn(upstream, upstream, upstream2_11, upstream2_11, upstream2_12, upstream2_12)

      upstream.dependsOn(upstreamUpstream2SharedTest, upstreamUpstream2Shared)
      upstream2_11.dependsOn(upstreamUpstream2SharedTest, upstreamUpstream2Shared)
      upstream2_12.dependsOn(upstreamUpstream2SharedTest, upstreamUpstream2Shared)

      upstreamBothPlatforms.dependsOn(upstreamBothPlatformsSharedJS, upstreamBothPlatformsSharedTestJS, upstreamBothShared, upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTestJS_JS2_11_12, upstreamBothSharedTest)
      upstreamBothPlatforms2_11.dependsOn(upstreamBothSharedJS2, upstreamBothSharedTestJS2, upstreamBothShared, upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTestJS_JS2_11_12, upstreamBothSharedTest)
      upstreamBothPlatforms2_12.dependsOn(upstreamBothSharedTest, upstreamBothSharedTestJS_JS2_11_12, upstreamBothShared, upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTest_JS2_12, upstreamBothShared_JS2_12)
      upstreamBothPlatformsJS.dependsOn(upstreamBothPlatformsSharedJS, upstreamBothPlatformsSharedJSJS2, upstreamBothPlatformsSharedTestJS,  upstreamBothPlatformsSharedTestJSJS2, upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTestJS_JS2_11_12)
      upstreamBothPlatformsJS2_11.dependsOn(upstreamBothSharedTestJS_JS2_11_12, upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTestJS2, upstreamBothSharedJS2, upstreamBothPlatformsSharedTestJSJS2, upstreamBothPlatformsSharedJSJS2)
      upstreamBothPlatformsJS2_12.dependsOn(upstreamBothSharedTestJS_JS2_11_12, upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTest_JS2_12, upstreamBothShared_JS2_12, upstreamBothPlatformsSharedTestJSJS2, upstreamBothPlatformsSharedJSJS2)

      downstreamBothShared.dependsOn(upstreamBothPlatforms, upstreamBothPlatforms, upstreamBothPlatforms2_11, upstreamBothPlatforms2_11, upstreamBothPlatforms2_12, upstreamBothPlatforms2_12)
      downstreamBothSharedJS2.dependsOn(upstreamBothPlatforms2_11, upstreamBothPlatforms2_11, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_11)
      downstreamBothSharedJS_JS2_11_12.dependsOn(upstreamBothPlatforms, upstreamBothPlatforms, upstreamBothPlatforms2_11, upstreamBothPlatforms2_11, upstreamBothPlatforms2_12, upstreamBothPlatforms2_12, upstreamBothPlatformsJS, upstreamBothPlatformsJS, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_12, upstreamBothPlatformsJS2_12)
      downstreamBothShared_JS2_12.dependsOn(upstreamBothPlatforms2_12, upstreamBothPlatforms2_12, upstreamBothPlatformsJS2_12, upstreamBothPlatformsJS2_12)

      downstreamBothPlatforms.dependsOn(upstreamBothPlatforms, upstreamBothPlatforms, downstreamBothPlatformSharedJS, downstreamBothPlatformSharedTestJS, downstreamBothShared, downstreamBothSharedJS_JS2_11_12, downstreamBothSharedTestJS_JS2_11_12, downstreamBothSharedTest)
      downstreamBothPlatforms2_11.dependsOn(downstreamBothSharedTest, downstreamBothSharedTestJS_JS2_11_12, downstreamBothSharedJS_JS2_11_12, downstreamBothShared, downstreamBothSharedTestJS2, downstreamBothSharedJS2, upstreamBothPlatforms2_11, upstreamBothPlatforms2_11)
      downstreamBothPlatforms2_12.dependsOn(downstreamBothSharedTest, downstreamBothSharedTestJS_JS2_11_12, downstreamBothSharedJS_JS2_11_12, downstreamBothShared, downstreamBothSharedTest_JS2_12, downstreamBothShared_JS2_12, upstreamBothPlatforms2_12, upstreamBothPlatforms2_12)
      downstreamBothPlatformsJS2_12.dependsOn(downstreamBothSharedTestJS_JS2_11_12, downstreamBothSharedJS_JS2_11_12, downstreamBothSharedTest_JS2_12, downstreamBothShared_JS2_12, downstreamBothPlatformSharedTestJSJS2, downstreamBothPlatformSharedJSJS2, upstreamBothPlatformsJS2_12, upstreamBothPlatformsJS2_12)
      downstreamBothPlatformsJS2_11.dependsOn(downstreamBothSharedTestJS_JS2_11_12, downstreamBothSharedJS_JS2_11_12, downstreamBothSharedTestJS2, downstreamBothSharedJS2, downstreamBothPlatformSharedTestJSJS2, downstreamBothPlatformSharedJSJS2, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_11)
      downstreamBothPlatformsJS.dependsOn(downstreamBothSharedTestJS_JS2_11_12, downstreamBothSharedJS_JS2_11_12, downstreamBothPlatformSharedTestJS, downstreamBothPlatformSharedTestJSJS2, downstreamBothPlatformSharedJSJS2, downstreamBothPlatformSharedJS, upstreamBothPlatformsJS, upstreamBothPlatformsJS)

      downstreamBothPlatformSharedJS.dependsOn(upstreamBothPlatforms, upstreamBothPlatforms, upstreamBothPlatformsJS, upstreamBothPlatformsJS)
      downstreamBothPlatformSharedJSJS2.dependsOn(upstreamBothPlatformsJS, upstreamBothPlatformsJS, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_12, upstreamBothPlatformsJS2_12)

      val upstreamModules: Seq[module] = Seq(
        upstream, upstream2_11, upstream2_12,
        upstreamBothShared, upstreamBothSharedTest, upstreamBothSharedJS2, upstreamBothSharedTestJS2,
        upstreamBothSharedJS_JS2_11_12, upstreamBothSharedTestJS_JS2_11_12,
        upstreamBothShared_JS2_12, upstreamBothSharedTest_JS2_12,
        upstreamBothPlatforms, upstreamBothPlatforms2_11, upstreamBothPlatforms2_12,
        upstreamBothPlatformsJS, upstreamBothPlatformsJS2_11, upstreamBothPlatformsJS2_12,
        upstreamBothPlatformsSharedJS, upstreamBothPlatformsSharedJSJS2,
        upstreamBothPlatformsSharedTestJS, upstreamBothPlatformsSharedTestJSJS2
      )

      val downstreamModules: Seq[module] = Seq(
        downstream, downstream2_11, downstream2_12,
        downstreamBothShared, downstreamBothSharedTest, downstreamBothSharedJS2,
        downstreamBothSharedTestJS2, downstreamBothSharedJS_JS2_11_12,
        downstreamBothSharedTestJS_JS2_11_12, downstreamBothShared_JS2_12,
        downstreamBothSharedTest_JS2_12, downstreamBothPlatforms, downstreamBothPlatforms2_11,
        downstreamBothPlatforms2_12, downstreamBothPlatformsJS, downstreamBothPlatformsJS2_11,
        downstreamBothPlatformsJS2_12, downstreamBothPlatformSharedJS,
        downstreamBothPlatformSharedJSJS2, downstreamBothPlatformSharedTestJS,
        downstreamBothPlatformSharedTestJSJS2
      )

      modules := Seq(
        sbtProjectmatrix, sbtProjectmatrixBuild, downstreamDownstream2SharedTest, downstreamDownstream2Shared, upstreamUpstream2SharedTest, upstreamUpstream2Shared
      ) ++ downstreamModules ++ upstreamModules
    }

    val matcher = new ProjectStructureMatcher {
      override protected def defaultAssertMatch: ProjectStructureMatcher.AttributeMatchType =
        ProjectStructureMatcher.AttributeMatchType.Exact

      override protected def useNewLogicForSourceFolderComparison: Boolean = true
    }
    val compareContext = ProjectStructureComparisonContext.Implicit.default(getProject)
      .withOptions(_.copy(strictCheckForBuildModules = true))
      .copy(assertionFailStrategy = new CollectErrors())

    matcher.assertProjectsEqual(expectedProject, getProject, singleContentRootModules = false)(compareContext)

    matcher.assertNoNotificationsShown(
      myProject,
      notificationsCollector.getNotifications,
      mutedNotificationTitles = Seq("Duplicate content roots detected")
    )
  }
}
