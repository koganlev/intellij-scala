package org.jetbrains.sbt.project

import com.intellij.codeInsight.daemon.ProblemHighlightFilter
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile}
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.{PsiFile, PsiManager}
import com.intellij.ui.EditorNotificationProvider
import junit.framework.TestCase.{assertFalse, assertNotNull, assertNull, assertTrue}
import org.jetbrains.plugins.scala.SlowTests
import org.jetbrains.plugins.scala.util.TestUtils
import org.junit.experimental.categories.Category

import java.nio.file.Path

@Category(Array(classOf[SlowTests]))
class SetupScalaHighlightingNotificationProviderTest extends SbtExternalSystemImportingTestLike {
  override protected def getTestProjectPath: String =
    s"${TestUtils.getTestDataPath}/sbt/projects/setupScalaHighlightingNotificationProvider"

  override protected def projectJdkLanguageLevel: LanguageLevel = LanguageLevel.JDK_17

  override def setUp(): Unit = {
    super.setUp()
    SbtProjectResolver.processOutputOfLatestStructureDump = ""
    SbtCachesSetupUtil.setupCoursierAndIvyCache(getProject)
  }

  def testSetupScalaHighlighting(): Unit = {
    val notificationProvider = EditorNotificationProvider.EP_NAME.findExtensionOrFail(classOf[SetupScalaHighlightingNotificationProvider], getProject)

    val greeterPath = Path.of(getTestProjectPath, "module1", "src", "main", "java", "Greeter.java")
    val abstractGreeterPath = Path.of(getTestProjectPath, "module1", "src", "main", "kotlin", "AbstractGreeter.kt")
    val helloWorldGreeterPath = Path.of(getTestProjectPath, "module2", "src", "main", "scala", "HelloWorldGreeter.scala")

    val shouldHighlightGreeterBefore = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(greeterPath))
    val shouldHighlightAbstractGreeterBefore = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(abstractGreeterPath))
    val shouldHighlightHelloWorldGreeterBefore = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(helloWorldGreeterPath))

    assertFalse("Greeter.java should not be highlighted before the project has been imported", shouldHighlightGreeterBefore)
    assertFalse("AbstractGreeter.kt should not be highlighted before the project has been imported", shouldHighlightAbstractGreeterBefore)
    assertFalse("HelloWorldGreeter.scala should not be highlighted before the project has been imported", shouldHighlightHelloWorldGreeterBefore)
    assertFalse("The sbt project should not have been imported yet", SbtProjectImportStateService.instance(getProject).isImported)

    importProject(false)

    val shouldHighlightGreeterAfter = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(greeterPath))
    val shouldHighlightAbstractGreeterAfter = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(abstractGreeterPath))
    val shouldHighlightHelloWorldGreeterAfter = ProblemHighlightFilter.shouldHighlightFile(findPsiFile(helloWorldGreeterPath))

    val notificationBannerGreeterAfter = notificationProvider.collectNotificationData(getProject, findVirtualFile(greeterPath))
    val notificationBannerAbstractGreeterAfter = notificationProvider.collectNotificationData(getProject, findVirtualFile(abstractGreeterPath))
    val notificationBannerHelloWorldGreeterAfter = notificationProvider.collectNotificationData(getProject, findVirtualFile(helloWorldGreeterPath))

    assertTrue("Greeter.java should be highlighted after the project has been imported", shouldHighlightGreeterAfter)
    assertTrue("AbstractGreeter.kt should be highlighted after the project has been imported", shouldHighlightAbstractGreeterAfter)
    assertTrue("HelloWorldGreeter.scala should be highlighted after the project has been imported", shouldHighlightHelloWorldGreeterAfter)
    assertTrue("The sbt project should have been imported", SbtProjectImportStateService.instance(getProject).isImported)
    assertNull("A notification banner should not be shown in Greeter.java after the project has been imported", notificationBannerGreeterAfter)
    assertNull("A notification banner should not be shown in AbstractGreeter.kt after the project has been imported", notificationBannerAbstractGreeterAfter)
    assertNull("A notification banner should not be shown in HelloWorldGreeter.scala after the project has been imported", notificationBannerHelloWorldGreeterAfter)
  }

  private def findPsiFile(path: Path): PsiFile = {
    val virtualFile = findVirtualFile(path)
    val manager = PsiManager.getInstance(getProject)
    val psiFile = manager.findFile(virtualFile)
    assertNotNull(s"Could not find psi file for virtual file: $virtualFile", psiFile)
    psiFile
  }

  private def findVirtualFile(path: Path): VirtualFile = {
    // It's necessary to refresh the virtual file system to get the up-to-date VirtualFile/PsiFile instances before
    // and after project import.
    val virtualFile = VfsUtil.findFile(path, true)
    assertNotNull(s"Could not find virtual file for path: $path", virtualFile)
    virtualFile
  }
}
