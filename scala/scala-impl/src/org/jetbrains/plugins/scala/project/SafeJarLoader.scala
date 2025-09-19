package org.jetbrains.plugins.scala.project

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile

import java.io.File
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path, Paths}
import scala.util.{Failure, Success, Try}

/**
 * Utility for safely creating ClassLoaders and URLs from JAR files with proper URL encoding.
 * 
 * This fixes SCL-24273: Classes decompiled from JAR files with special characters in their names are not resolved.
 * The root cause was manual construction of jar:file: URLs instead of using Java's proper Path→URI conversion APIs.
 * 
 * Special characters that caused issues include: #, spaces, @, &, +, %
 */
object SafeJarLoader {
  
  private val LOG = Logger.getInstance(getClass)

  /**
   * Creates a URLClassLoader from a JAR file with proper URL encoding.
   * 
   * @param jarFile the JAR file to create a ClassLoader for
   * @param parent the parent ClassLoader (defaults to current ClassLoader)
   * @return Some(URLClassLoader) if successful, None if all attempts fail
   */
  def createClassLoader(jarFile: VirtualFile, parent: ClassLoader = null): Option[URLClassLoader] = {
    createJarUrl(jarFile.getPath).map { url =>
      new URLClassLoader(Array(url), parent)
    }
  }

  /**
   * Creates a URLClassLoader from a JAR file path with proper URL encoding.
   * 
   * @param jarPath the path to the JAR file
   * @param parent the parent ClassLoader (defaults to current ClassLoader)
   * @return Some(URLClassLoader) if successful, None if all attempts fail
   */
  def createClassLoader(jarPath: String, parent: ClassLoader = null): Option[URLClassLoader] = {
    createJarUrl(jarPath).map { url =>
      new URLClassLoader(Array(url), parent)
    }
  }

  /**
   * Creates a proper JAR URL from a file path with correct encoding.
   * 
   * This method tries multiple approaches to handle edge cases:
   * 1. Primary: Java NIO Path with proper URI encoding
   * 2. Fallback: Legacy File.toURI() method
   * 
   * @param jarPath the path to the JAR file
   * @return Some(URL) if successful, None if all attempts fail
   */
  def createJarUrl(jarPath: String): Option[URL] = {
    val attempts = Seq(
      // Primary: NIO Path with proper URI encoding
      () => Paths.get(jarPath).toUri().toURL(),
      
      // Fallback: Legacy File.toURI() method for compatibility
      () => new File(jarPath).toURI().toURL()
    )
    
    attempts.view.flatMap { attempt =>
      Try(attempt()) match {
        case Success(url) => 
          LOG.debug(s"Successfully created URL for JAR: $jarPath -> $url")
          Some(url)
        case Failure(ex) =>
          LOG.debug(s"URL creation attempt failed for $jarPath: ${ex.getMessage}")
          None
      }
    }.headOption.orElse {
      logJarLoadingIssue(jarPath, new IllegalStateException("All URL creation attempts failed"))
      None
    }
  }

  /**
   * Creates a JAR resource URL (jar:file://path!/resource) with proper encoding.
   * 
   * @param jarPath path to the JAR file
   * @param resource resource path within the JAR (e.g., "META-INF/MANIFEST.MF")
   * @return Some(URL) if successful, None if creation fails
   */
  def createJarResourceUrl(jarPath: String, resource: String): Option[URL] = {
    createJarUrl(jarPath).flatMap { jarUrl =>
      Try {
        // Use the properly encoded jar URL and append the resource
        new URL(s"jar:${jarUrl.toString}!/$resource")
      }.toOption.orElse {
        LOG.warn(s"Failed to create JAR resource URL for $jarPath!/$resource")
        None
      }
    }
  }

  /**
   * Creates a JAR resource URL from a URI string with proper encoding.
   * This is useful when you already have a file URI.
   * 
   * @param jarFileUri the URI string of the JAR file
   * @param resource resource path within the JAR
   * @return Some(URL) if successful, None if creation fails
   */
  def createJarResourceUrlFromUri(jarFileUri: String, resource: String): Option[URL] = {
    Try {
      new URL(s"jar:$jarFileUri!/$resource")
    }.toOption.orElse {
      LOG.warn(s"Failed to create JAR resource URL from URI $jarFileUri!/$resource")
      None
    }
  }

  private def logJarLoadingIssue(jarPath: String, error: Throwable): Unit = {
    val fileName = Paths.get(jarPath).getFileName.toString
    val hasSpecialChars = """[#\s@&+%]""".r.findFirstIn(fileName).isDefined
    
    if (hasSpecialChars) {
      LOG.warn(s"JAR file name contains special characters that may cause URL encoding issues: $fileName")
      LOG.warn("Consider renaming the JAR file to avoid characters: # @ & + % (spaces)")
      LOG.warn("See: https://youtrack.jetbrains.com/issue/SCL-24273")
    }
    
    LOG.error(s"Failed to create URL for JAR: $jarPath", error)
  }
}