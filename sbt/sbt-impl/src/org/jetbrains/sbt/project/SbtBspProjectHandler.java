package org.jetbrains.sbt.project;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SbtBspProjectHandler {
    ExtensionPointName<SbtBspProjectHandler> EP_NAME = ExtensionPointName.create("org.jetbrains.sbt.bspProjectHandler");

    boolean isHandledByBsp(Project project);

    @ApiStatus.Internal
    static boolean isImportedAsBspProject(Project project) {
        return EP_NAME.getExtensionList().stream().anyMatch(handler -> handler.isHandledByBsp(project));
    }
}
