package org.jetbrains.plugins.scala.project.external;

import com.intellij.workspaceModel.ide.impl.legacyBridge.LegacyBridgeModifiableBase;
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory;

public class CompanionProxyUtils {
    public final static LegacyBridgeJpsEntitySourceFactory.Companion LegacyBridgeJpsEntitySourceFactoryCompanion = LegacyBridgeJpsEntitySourceFactory.Companion;
    public final static LegacyBridgeModifiableBase.Companion LegacyBridgeModifiableBaseCompanion = LegacyBridgeModifiableBase.Companion;
}
