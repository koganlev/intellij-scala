package org.jetbrains.sbt.project.settings

import com.intellij.openapi.components.{PersistentStateComponent, Service, State, Storage, StoragePathMacros}
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

import scala.beans.BeanProperty

@State(
  name = "ShouldUpdateRunConfigurations",
  storages = Array(new Storage(StoragePathMacros.WORKSPACE_FILE))
)
@Service(Array(Service.Level.PROJECT))
final class ShouldUpdateRunConfigurations extends PersistentStateComponent[ShouldUpdateRunConfigurations]{

  @BeanProperty
  var shouldUpdate: Boolean = false

  @BeanProperty
  var isDowngrading: Boolean = false

  override def getState: ShouldUpdateRunConfigurations = this

  override def loadState(state: ShouldUpdateRunConfigurations): Unit =
    XmlSerializerUtil.copyBean(state, this)
}

object ShouldUpdateRunConfigurations {
  def getInstance(project: Project): ShouldUpdateRunConfigurations =
    project.getService(classOf[ShouldUpdateRunConfigurations])

  def disableConfigurationUpdate(project: Project): Unit = {
    val notification = getInstance(project)
    notification.shouldUpdate = false
  }
  
  def updateState(project: Project, maybeShowTheNotification: Boolean, isDowngrading: Boolean): Unit = {
    val notification = getInstance(project)
    notification.shouldUpdate = maybeShowTheNotification
    notification.isDowngrading = isDowngrading
  }
}
