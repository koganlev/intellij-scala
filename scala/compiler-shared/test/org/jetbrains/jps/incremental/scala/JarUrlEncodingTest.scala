package org.jetbrains.jps.incremental.scala

import org.junit.Assert._
import org.junit.Test

import java.io.File
import java.nio.file.{Files, Path}
import java.util.Properties
import scala.util.Using

/**
 * Tests for the fixed readProperty method that handles JAR files with special characters.
 * 
 * This addresses SCL-24273: Classes decompiled from JAR files with special characters 
 * in their names are not resolved.
 */
class JarUrlEncodingTest {

  @Test
  def testReadPropertyWithSpecialCharacters(): Unit = {
    val testCases = Seq(
      "library with spaces.jar",
      "library#version.jar", 
      "library@snapshot.jar",
      "library&dependency.jar",
      "library+extra.jar",
      "library%encoded.jar"
    )
    
    testCases.foreach { jarName =>
      val tempJar = createTestJarWithProperty(jarName, "test.properties", "version", "1.0.0")
      
      try {
        val result = readProperty(tempJar, "test.properties", "version")
        assertTrue(s"Should read property from JAR: $jarName", result.isDefined)
        assertEquals(s"Should get correct property value from: $jarName", "1.0.0", result.get)
      } finally {
        tempJar.toFile.delete()
      }
    }
  }

  @Test  
  def testReadPropertyFromNormalJar(): Unit = {
    val jarName = "normal-library.jar"
    val tempJar = createTestJarWithProperty(jarName, "library.properties", "name", "scala-library")
    
    try {
      val result = readProperty(tempJar, "library.properties", "name")
      assertTrue("Should read property from normal JAR", result.isDefined)
      assertEquals("Should get correct property value", "scala-library", result.get)
    } finally {
      tempJar.toFile.delete()
    }
  }

  @Test
  def testReadPropertyNonExistentResource(): Unit = {
    val jarName = "test.jar"
    val tempJar = createTestJarWithProperty(jarName, "existing.properties", "key", "value")
    
    try {
      val result = readProperty(tempJar, "non-existent.properties", "key")
      assertFalse("Should return None for non-existent resource", result.isDefined)
    } finally {
      tempJar.toFile.delete()
    }
  }

  @Test
  def testReadPropertyNonExistentKey(): Unit = {
    val jarName = "test.jar"
    val tempJar = createTestJarWithProperty(jarName, "test.properties", "existing-key", "value")
    
    try {
      val result = readProperty(tempJar, "test.properties", "non-existent-key")
      assertFalse("Should return None for non-existent key", result.isDefined)
    } finally {
      tempJar.toFile.delete()
    }
  }

  private def createTestJarWithProperty(jarName: String, resourceName: String, propertyKey: String, propertyValue: String): Path = {
    val tempDir = Files.createTempDirectory("jar-encoding-test")
    val jarFile = tempDir.resolve(jarName)
    
    // Create the properties content
    val properties = new Properties()
    properties.setProperty(propertyKey, propertyValue)
    
    // Create a JAR file with the properties resource
    Using.resource(new java.util.jar.JarOutputStream(Files.newOutputStream(jarFile))) { jos =>
      val entry = new java.util.jar.JarEntry(resourceName)
      jos.putNextEntry(entry)
      
      Using.resource(new java.io.ByteArrayOutputStream()) { baos =>
        properties.store(baos, "Test properties")
        jos.write(baos.toByteArray)
      }
      
      jos.closeEntry()
    }
    
    jarFile
  }
}