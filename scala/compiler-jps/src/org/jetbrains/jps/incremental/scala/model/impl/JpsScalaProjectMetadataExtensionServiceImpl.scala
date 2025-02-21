package org.jetbrains.jps.incremental.scala.model
package impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.containers.FileCollectionFactory
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.scala.model.impl.JpsScalaProjectMetadataExtensionServiceImpl.Log
import org.jetbrains.jps.incremental.scala.{ScalaJpsProjectMetadataConstants, SettingsManager}
import org.jetbrains.jps.model.module.JpsModule

import java.nio.file.Path
import java.util.concurrent.locks.{Lock, ReentrantLock}
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

private final class JpsScalaProjectMetadataExtensionServiceImpl extends JpsScalaProjectMetadataExtensionService {

  private val loadedConfigs: mutable.Map[Path, Set[String]] = FileCollectionFactory.createCanonicalPathMap[Set[String]]().asScala

  private val lock: Lock = new ReentrantLock()

  override def projectHasScala(context: CompileContext): Boolean =
    loadConfig(context).nonEmpty

  override def moduleHasScala(context: CompileContext)(module: JpsModule): Boolean = {
    val name = module.getName
    loadConfig(context).contains(name)
  }

  private def loadConfig(context: CompileContext): Set[String] = {
    val paths = context.getProjectDescriptor.dataManager.getDataPaths
    val dataStorageDir = paths.getDataStorageDir
    val configFilePath = dataStorageDir.resolve(ScalaJpsProjectMetadataConstants.ScalaJpsProjectMetadataFileName)
    lock.lock()
    try loadedConfigs.getOrElseUpdate(configFilePath, computeConfig(configFilePath, context))
    finally lock.unlock()
  }

  private def computeConfig(configFilePath: Path, context: CompileContext): Set[String] =
    try readConfigFromDisk(configFilePath)
    catch {
      case NonFatal(t) =>
        Log.info(s"Failed to read the modules with a configured Scala SDK from $configFilePath, falling back to manual search", t)
        manualSearchFallback(context)
    }

  private def readConfigFromDisk(configFilePath: Path): Set[String] = {
    val element = JDOMUtil.load(configFilePath)
    element.getChild(ScalaJpsProjectMetadataConstants.ModulesWithScalaSdkElement)
      .getChildren(ScalaJpsProjectMetadataConstants.ModuleElement)
      .asScala
      .map(_.getAttributeValue(ScalaJpsProjectMetadataConstants.NameAttribute))
      .toSet
  }

  private def manualSearchFallback(context: CompileContext): Set[String] = {
    val modules = context.getProjectDescriptor.getProject.getModules.asScala
    modules.filter(SettingsManager.getScalaSdk(_).isDefined).map(_.getName).toSet
  }
}

private object JpsScalaProjectMetadataExtensionServiceImpl {
  val Log: Logger = Logger.getInstance(classOf[JpsScalaProjectMetadataExtensionServiceImpl])
}
