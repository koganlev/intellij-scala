package org.jetbrains.bsp.project

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.{PersistentStateComponent, Service, State, Storage, StoragePathMacros}
import com.intellij.openapi.project.Project

import scala.beans.BeanProperty

@State(
  name = "PersistentBspTargetCapabilitiesHolder",
  storages = Array(new Storage(StoragePathMacros.WORKSPACE_FILE))
)
@Service(Array(Service.Level.PROJECT))
final class PersistentBspTargetCapabilitiesHolder
  extends PersistentStateComponent[PersistentBspTargetCapabilitiesHolder] {

  @BeanProperty
  var btIdToCapabilities: Map[BuildTargetIdentifier, BspTargetCapabilities] = Map.empty

  override def getState: PersistentBspTargetCapabilitiesHolder = this

  override def loadState(state: PersistentBspTargetCapabilitiesHolder): Unit = {
    btIdToCapabilities = state.btIdToCapabilities
  }
}

object PersistentBspTargetCapabilitiesHolder {
  def getInstance(project: Project): PersistentBspTargetCapabilitiesHolder = {
    project.getService(classOf[PersistentBspTargetCapabilitiesHolder])
  }
}