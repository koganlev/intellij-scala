package org.jetbrains.plugins.scala.project

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Assert._
import org.mockito.Mockito._

import java.io.File
import java.nio.file.Files
import java.util.jar.{JarEntry, JarOutputStream}
import scala.util.Using

/**
 * Integration test demonstrating that the SCL-24273 fix works end-to-end.
 * 
 * This test simulates the scenario where a JAR file with special characters 
 * in its name needs to be processed, which was previously failing.
 */
class SCL24273IntegrationTest extends LightPlatformTestCase {

  def testJarWithSpecialCharactersIntegration(): Unit = {
    // Create a JAR file with special characters that previously caused issues
    val problematicJarName = "scala-library with spaces & special#chars@2.13.jar"
    val testJar = createRealJarFile(problematicJarName)
    
    try {
      // Test 1: SafeJarLoader should handle this correctly
      val classLoaderOpt = SafeJarLoader.createClassLoader(testJar.getAbsolutePath)
      assertTrue("Should create ClassLoader for problematic JAR name", classLoaderOpt.isDefined)
      
      // Test 2: URL creation should work
      val urlOpt = SafeJarLoader.createJarUrl(testJar.getAbsolutePath)  
      assertTrue("Should create URL for problematic JAR name", urlOpt.isDefined)
      
      // Test 3: Resource URL creation should work
      val resourceUrlOpt = SafeJarLoader.createJarResourceUrl(
        testJar.getAbsolutePath, 
        "library.properties"
      )
      assertTrue("Should create resource URL for problematic JAR name", resourceUrlOpt.isDefined)
      
      // Test 4: Verify that the URLs are properly encoded
      val url = urlOpt.get
      val urlString = url.toString
      assertTrue("URL should be properly encoded", urlString.contains("%20")) // spaces encoded
      assertFalse("URL should not contain raw spaces", urlString.contains(" "))
      
      // Test 5: Verify ClassLoader functionality
      val classLoader = classLoaderOpt.get
      assertNotNull("ClassLoader should not be null", classLoader)
      
      // Test 6: Verify we can actually read from the JAR using the resource URL
      val resourceUrl = resourceUrlOpt.get
      Using.resource(resourceUrl.openStream()) { stream =>
        assertNotNull("Should be able to open resource stream", stream)
        
        // Read the properties content
        val properties = new java.util.Properties()
        properties.load(stream)
        assertEquals("Should read correct property value", "2.13.0", properties.getProperty("version"))
      }
      
      // Clean up
      classLoader.close()
      
    } finally {
      testJar.delete()
    }
  }

  def testLibraryExtJarUrlsWithSpecialCharacters(): Unit = {
    // Test the LibraryExt.jarUrls method that was fixed
    val testJars = Seq(
      "library with spaces.jar",
      "library#hash.jar", 
      "library@at.jar",
      "library&ampersand.jar"
    )
    
    val createdJars = testJars.map(createRealJarFile)
    
    try {
      // Create URLs using the fixed method
      val urls = createdJars.flatMap(jar => SafeJarLoader.createJarUrl(jar.getAbsolutePath))
      
      assertEquals("Should create URLs for all test JARs", testJars.size, urls.size)
      
      // Verify all URLs are properly formed
      urls.foreach { url =>
        assertNotNull("URL should not be null", url)
        assertTrue("URL should start with file:", url.toString.startsWith("file:"))
        
        // Verify we can open a connection (validates the URL)
        assertNotNull("Should be able to open connection", url.openConnection())
      }
      
    } finally {
      createdJars.foreach(_.delete())
    }
  }

  def testArtifactReadPropertyWithSpecialCharacters(): Unit = {
    // Test the Artifact.readProperty method that was fixed
    val jarName = "test artifact & special chars.jar"
    val testJar = createJarWithProperties(jarName, Map("version" -> "1.0.0", "name" -> "test-lib"))
    
    try {
      val jarUri = testJar.toPath.toUri.toString
      
      // This simulates what the fixed readProperty method does
      val resourceUrlOpt = SafeJarLoader.createJarResourceUrlFromUri(jarUri, "library.properties")
      assertTrue("Should create resource URL from URI", resourceUrlOpt.isDefined)
      
      val resourceUrl = resourceUrlOpt.get
      Using.resource(resourceUrl.openStream()) { stream =>
        val properties = new java.util.Properties()
        properties.load(stream)
        
        assertEquals("Should read version property", "1.0.0", properties.getProperty("version"))
        assertEquals("Should read name property", "test-lib", properties.getProperty("name"))
      }
      
    } finally {
      testJar.delete()
    }
  }

  private def createRealJarFile(jarName: String): File = {
    val tempDir = Files.createTempDirectory("scl-24273-test")
    val jarFile = tempDir.resolve(jarName).toFile
    
    Using.resource(new JarOutputStream(Files.newOutputStream(jarFile.toPath))) { jos =>
      // Add a properties file 
      val propsEntry = new JarEntry("library.properties")
      jos.putNextEntry(propsEntry)
      jos.write("version=2.13.0\nname=scala-library\n".getBytes)
      jos.closeEntry()
      
      // Add a test class file entry
      val classEntry = new JarEntry("TestClass.class")
      jos.putNextEntry(classEntry)
      jos.write("fake class content".getBytes)
      jos.closeEntry()
    }
    
    jarFile
  }

  private def createJarWithProperties(jarName: String, properties: Map[String, String]): File = {
    val tempDir = Files.createTempDirectory("scl-24273-test")
    val jarFile = tempDir.resolve(jarName).toFile
    
    Using.resource(new JarOutputStream(Files.newOutputStream(jarFile.toPath))) { jos =>
      val propsEntry = new JarEntry("library.properties")
      jos.putNextEntry(propsEntry)
      
      val propsContent = properties.map { case (k, v) => s"$k=$v" }.mkString("\n")
      jos.write(propsContent.getBytes)
      jos.closeEntry()
    }
    
    jarFile
  }
}