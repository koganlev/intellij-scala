package org.jetbrains.plugins.scala.lang.surroundWith;

import com.intellij.openapi.project.Project;
import junit.framework.Test;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.Scala3Language;

public class SurroundWith3Test extends TestCase {
    public static Test suite() {
        return new ScalaSurroundWithFileSetTestCase("/surroundWith/data/3/", Scala3Language.INSTANCE) {
            @Override
            protected void setSettings(@NotNull Project project) {
                super.setSettings(project);
                getScalaSettings(project).USE_SCALA3_INDENTATION_BASED_SYNTAX = true;
            }
        };
    }
}
