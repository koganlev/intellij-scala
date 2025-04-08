package org.jetbrains.plugins.scala.compiler.polyglot

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.projectRoots.{ProjectJdkTable, Sdk}
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.platform.externalSystem.testFramework.ExternalSystemImportingTestCase
import com.intellij.testFramework.{CompilerTester, IndexingTestUtil}
import junit.framework.TestCase.{assertEquals, assertNotNull}
import org.jetbrains.plugins.scala.CompilationTests_Zinc
import org.jetbrains.plugins.scala.base.libraryLoaders.SmartJDKLoader
import org.jetbrains.plugins.scala.compiler.data.IncrementalityType
import org.jetbrains.plugins.scala.compiler.{CompileServerLauncher, JdkVersionDiscovery}
import org.jetbrains.plugins.scala.extensions.inWriteAction
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration
import org.jetbrains.plugins.scala.settings.ScalaCompileServerSettings
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.settings.SbtProjectSettings
import org.jetbrains.sbt.project.{SbtCachesSetupUtil, SbtProjectSystem}
import org.junit.experimental.categories.Category

@Category(Array(classOf[CompilationTests_Zinc]))
abstract class PolyglotSbtCompilationTestBase(separateModules: Boolean) extends ExternalSystemImportingTestCase {

  private var sdk: Sdk = _

  private var compiler: CompilerTester = _

  private var module1: Module = _

  private var module2: Module = _

  override lazy val getCurrentExternalProjectSettings: SbtProjectSettings = {
    val settings = new SbtProjectSettings()
    settings.separateProdAndTestSources = separateModules
    settings.jdk = sdk.getName
    settings
  }

  override def getExternalSystemId: ProjectSystemId = SbtProjectSystem.Id

  override def getTestsTempDir: String = this.getClass.getSimpleName

  override def getExternalSystemConfigFileName: String = Sbt.BuildFile

  override def setUp(): Unit = {
    super.setUp()

    sdk = {
      val jdkVersion = JdkVersionDiscovery.discoveredJdk
      val res = SmartJDKLoader.getOrCreateJDK(jdkVersion)
      val settings = ScalaCompileServerSettings.getInstance()
      settings.COMPILE_SERVER_SDK = res.getName
      settings.USE_DEFAULT_SDK = false
      res
    }

    SbtCachesSetupUtil.setupCoursierAndIvyCache(getProject)

    createProjectSubDirs("project", "module1/src/main/java", "module1/src/main/kotlin", "module2/src/main/scala")
    createProjectSubFile("project/build.properties",
      """sbt.version=1.10.5
        |""".stripMargin)
    createProjectSubFile("project/plugins.sbt",
      """addSbtPlugin("org.jetbrains.scala" % "sbt-kotlin-plugin" % "3.1.4")
        |""".stripMargin)
    createProjectConfig(
      """lazy val root = project.in(file("."))
        |  .aggregate(module1, module2)
        |  .settings(
        |    name := "polyglot-sbt"
        |  )
        |
        |lazy val module1 = project.in(file("module1"))
        |  .enablePlugins(KotlinPlugin)
        |
        |lazy val module2 = project.in(file("module2"))
        |  .dependsOn(module1)
        |  .settings(
        |    scalaVersion := "2.13.15"
        |  )
        |""".stripMargin)
    createProjectSubFile("module1/src/main/java/Greeter.java",
      """public interface Greeter {
        |  String greeting();
        |}
        |""".stripMargin)
    createProjectSubFile("module1/src/main/kotlin/AbstractGreeter.kt",
      """abstract class AbstractGreeter(private val str: String) : Greeter {
        |  override fun greeting(): String = str
        |}
        |""".stripMargin)
    createProjectSubFile("module2/src/main/scala/HelloWorldGreeter.scala",
      """object HelloWorldGreeter extends AbstractGreeter("Hello, world!")
        |""".stripMargin)

    importProject()

    KotlinDaemonUtil.disableKotlinDaemon(getProject)

    val modules = ModuleManager.getInstance(getProject).getModules
    modules.foreach(ModuleRootModificationUtil.setModuleSdk(_, sdk))

    IndexingTestUtil.waitUntilIndexesAreReady(getProject)

    val module1Name = if (separateModules) "polyglot-sbt.module1.main" else "polyglot-sbt.module1"
    val module2Name = if (separateModules) "polyglot-sbt.module2.main" else "polyglot-sbt.module2"

    module1 = modules.find(_.getName == module1Name).orNull
    assertNotNull(s"Could not find module with name '$module1Name'", module1)
    module2 = modules.find(_.getName == module2Name).orNull
    assertNotNull(s"Could not find module with name '$module2Name'", module2)
    compiler = new CompilerTester(getProject, java.util.Arrays.asList(modules: _*), null, false)
  }

  override def tearDown(): Unit = try {
    CompileServerLauncher.stopServerAndWait()
    compiler.tearDown()
    val settings = ScalaCompileServerSettings.getInstance()
    settings.USE_DEFAULT_SDK = true
    settings.COMPILE_SERVER_SDK = null
    inWriteAction {
      val jdkTable = ProjectJdkTable.getInstance()
      jdkTable.removeJdk(sdk)
      val kotlinSdk = jdkTable.getAllJdks.find(_.getName.contains("Kotlin SDK"))
      kotlinSdk.foreach(jdkTable.removeJdk)
    }
  } finally {
    super.tearDown()
  }

  def testPolyglotCompilation(): Unit = {
    assertEquals(IncrementalityType.SBT, ScalaCompilerConfiguration.instanceIn(getProject).incrementalityType)
    compiler.make()
    assertClassExists("Greeter", module1)
    assertClassExists("AbstractGreeter", module1)
    assertClassExists("HelloWorldGreeter", module2)
  }

  private def assertClassExists(name: String, module: Module): Unit = {
    val file = compiler.findClassFile(name, module)
    assertNotNull(s"Could not find class file for $name", file)
  }
}

class PolyglotSbtCompilationTest extends PolyglotSbtCompilationTestBase(separateModules = false)

class PolyglotSbtCompilationWithSeparateModulesTest extends PolyglotSbtCompilationTestBase(separateModules = true)
