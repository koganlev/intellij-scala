package org.jetbrains.plugins.scala.project

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Assert._
import org.mockito.Mockito._

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path, Paths}
import scala.util.Using

/**
 * Tests for SafeJarLoader utility that handles JAR files with special characters in filenames.
 * 
 * This addresses SCL-24273: Classes decompiled from JAR files with special characters 
 * in their names are not resolved.
 */
class SafeJarLoaderTest extends LightPlatformTestCase {

  def testJarUrlCreationWithSpecialCharacters(): Unit = {
    val testCases = Map(
      "library with spaces.jar" -> true,
      "library#version.jar" -> true,
      "library@snapshot.jar" -> true,
      "library&dependency.jar" -> true,
      "library+extra.jar" -> true,
      "library%encoded.jar" -> true,
      "normal-library.jar" -> true,
      "scala-library_2.13.jar" -> true,
      "lib[brackets].jar" -> true,
      "lib(parentheses).jar" -> true
    )
    
    testCases.foreach { case (jarName, shouldSucceed) =>
      val tempJar = createTestJarFile(jarName)
      try {
        val urlOpt = SafeJarLoader.createJarUrl(tempJar.getAbsolutePath)
        
        if (shouldSucceed) {
          assertTrue(s"Should create URL for: $jarName", urlOpt.isDefined)
          val url = urlOpt.get
          
          // Verify the URL is properly formed
          assertNotNull(s"URL should not be null for: $jarName", url)
          assertTrue(s"URL should start with file: for: $jarName", url.toString.startsWith("file:"))
          
          // Verify we can actually use the URL (it should be valid)
          assertNotNull(s"Should be able to open connection for: $jarName", url.openConnection())
        } else {
          assertFalse(s"Should not create URL for: $jarName", urlOpt.isDefined)
        }
      } finally {
        tempJar.delete()
      }
    }
  }

  def testClassLoaderCreationWithSpecialCharacters(): Unit = {
    val jarName = "test library with spaces & special#chars.jar"
    val tempJar = createTestJarFile(jarName)
    
    try {
      val classLoaderOpt = SafeJarLoader.createClassLoader(tempJar.getAbsolutePath)
      
      assertTrue("Should create ClassLoader for JAR with special characters", classLoaderOpt.isDefined)
      
      val classLoader = classLoaderOpt.get
      assertNotNull("ClassLoader should not be null", classLoader)
      assertTrue("Should be URLClassLoader", classLoader.isInstanceOf[URLClassLoader])
      
      val urls = classLoader.asInstanceOf[URLClassLoader].getURLs
      assertEquals("Should have exactly one URL", 1, urls.length)
      
      // Clean up
      classLoader.close()
    } finally {
      tempJar.delete()
    }
  }

  def testVirtualFileClassLoaderCreation(): Unit = {
    val jarName = "virtual file test.jar"
    val tempJar = createTestJarFile(jarName)
    
    try {
      // Mock VirtualFile
      val virtualFile = mock(classOf[VirtualFile])
      when(virtualFile.getPath).thenReturn(tempJar.getAbsolutePath)
      when(virtualFile.getName).thenReturn(jarName)
      
      val classLoaderOpt = SafeJarLoader.createClassLoader(virtualFile)
      
      assertTrue("Should create ClassLoader from VirtualFile", classLoaderOpt.isDefined)
      assertNotNull("ClassLoader should not be null", classLoaderOpt.get)
      
      // Clean up
      classLoaderOpt.get.close()
    } finally {
      tempJar.delete()
    }
  }

  def testJarResourceUrlCreation(): Unit = {
    val jarName = "resource test & chars.jar"
    val tempJar = createTestJarFile(jarName)
    
    try {
      val resourceUrlOpt = SafeJarLoader.createJarResourceUrl(tempJar.getAbsolutePath, "META-INF/MANIFEST.MF")
      
      assertTrue("Should create resource URL", resourceUrlOpt.isDefined)
      
      val resourceUrl = resourceUrlOpt.get
      assertTrue("Resource URL should start with jar:", resourceUrl.toString.startsWith("jar:"))
      assertTrue("Resource URL should contain resource path", resourceUrl.toString.contains("META-INF/MANIFEST.MF"))
    } finally {
      tempJar.delete()
    }
  }

  def testJarResourceUrlFromUri(): Unit = {
    val jarName = "uri test.jar"
    val tempJar = createTestJarFile(jarName)
    
    try {
      val jarUri = tempJar.toPath.toUri.toString
      val resourceUrlOpt = SafeJarLoader.createJarResourceUrlFromUri(jarUri, "test/resource.properties")
      
      assertTrue("Should create resource URL from URI", resourceUrlOpt.isDefined)
      
      val resourceUrl = resourceUrlOpt.get
      assertTrue("Resource URL should start with jar:", resourceUrl.toString.startsWith("jar:"))
      assertTrue("Resource URL should contain resource path", resourceUrl.toString.contains("test/resource.properties"))
    } finally {
      tempJar.delete()
    }
  }

  def testUrlEncodingCorrectness(): Unit = {
    val jarName = "encoding test & special chars #@+%.jar"
    val tempJar = createTestJarFile(jarName)
    
    try {
      val urlOpt = SafeJarLoader.createJarUrl(tempJar.getAbsolutePath)
      assertTrue("Should create URL", urlOpt.isDefined)
      
      val url = urlOpt.get
      val urlString = url.toString
      
      // Verify that special characters are properly encoded
      // Spaces should be encoded as %20
      assertTrue("URL should contain encoded spaces", urlString.contains("%20"))
      
      // The URL should be valid and openable
      Using.resource(url.openStream()) { stream =>
        assertNotNull("Should be able to open stream", stream)
      }
    } finally {
      tempJar.delete()
    }
  }

  def testNonExistentJarHandling(): Unit = {
    val nonExistentPath = "/path/that/does/not/exist/test.jar"
    
    val urlOpt = SafeJarLoader.createJarUrl(nonExistentPath)
    // URL creation might succeed even for non-existent files (depending on the JVM implementation)
    // but opening a stream would fail. The main thing is that it shouldn't throw an exception.
    
    val classLoaderOpt = SafeJarLoader.createClassLoader(nonExistentPath)
    // Similar to URL creation - might succeed but usage would fail
    // Main thing is no exceptions thrown during creation
  }

  def testEmptyAndInvalidPaths(): Unit = {
    val invalidPaths = Seq("", "   ", "not-a-jar", "file-without-extension")
    
    invalidPaths.foreach { path =>
      // Should not throw exceptions, even with invalid input
      val urlOpt = SafeJarLoader.createJarUrl(path)
      val classLoaderOpt = SafeJarLoader.createClassLoader(path)
      
      // We don't strictly require these to fail, just that they don't crash
    }
  }

  private def createTestJarFile(fileName: String): File = {
    val tempDir = Files.createTempDirectory("safe-jar-loader-test")
    val jarFile = tempDir.resolve(fileName).toFile
    
    // Create a minimal valid JAR file
    Using.resource(new java.util.jar.JarOutputStream(Files.newOutputStream(jarFile.toPath))) { jos =>
      val entry = new java.util.jar.JarEntry("test.txt")
      jos.putNextEntry(entry)
      jos.write("test content".getBytes)
      jos.closeEntry()
    }
    
    jarFile
  }
}