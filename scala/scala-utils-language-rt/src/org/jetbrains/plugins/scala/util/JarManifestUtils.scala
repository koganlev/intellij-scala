package org.jetbrains.plugins.scala.util

import java.io.{BufferedInputStream, File}
import java.nio.file.{FileSystems, Files, Path}
import scala.util.{Try, Using}

object JarManifestUtils {

  def readManifestAttribute(jar: Path, attributeName: String): Try[String] =
    Using(FileSystems.newFileSystem(jar, null: ClassLoader)) { fileSystem =>
      val manifestPath = fileSystem.getPath("META-INF", "MANIFEST.MF")
      val manifest = Using.resource(new BufferedInputStream(Files.newInputStream(manifestPath)))(new java.util.jar.Manifest(_))
      manifest.getMainAttributes.getValue(attributeName)
    }

  /**
   * @return Some list of classpath files if it's specified in the manifest. No validation is done for the files<br>
   *         None - if manifest or the class path attribute were not found
   */
  def readClassPath(jarFile: File): Option[Seq[File]] = {
    val classpathAttributeOpt = readManifestAttribute(jarFile.toPath, "Class-Path").toOption
    classpathAttributeOpt.map { classPathAttribute =>
      val paths = classPathAttribute.split(" ").map(_.trim)
      val parentDirectory = jarFile.getParentFile
      paths.map(new File(parentDirectory, _)).map(_.getCanonicalFile).toSeq
    }
  }
}
