package org.jetbrains.plugins.scala.project.sdkdetect.repository;

import com.intellij.util.SystemProperties;
import dev.dirs.ProjectDirectories;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Computes Coursier's directories according to the standard
 * defined by operating system Coursier is running on.
 * Copied from coursier/modules/paths/src/main/java/coursier/paths/CoursierPaths.java
 *
 * @implNote If more paths e. g. for configuration or application data is required,
 * use {@link #coursierDirectories} and do not roll your own logic.
 */
public final class CoursierPaths {
    private CoursierPaths() {
        throw new Error();
    }

    private static final Object coursierDirectoriesLock = new Object();
    private static ProjectDirectories coursierDirectories0;

    private static final Object cacheDirectoryLock = new Object();
    private static volatile Path cacheDirectory0 = null;

    private static final Object configDirectoryLock = new Object();
    private static volatile Path configDirectory0 = null;

    private static final Object dataLocalDirectoryLock = new Object();
    private static volatile Path dataLocalDirectory0 = null;

    // TODO After switching to nio, that logic can be unit tested with mock filesystems.

    private static String computeCacheDirectory() throws IOException {
        String path = System.getenv("COURSIER_CACHE");

        if (path == null)
            path = System.getProperty("coursier.cache");

        if (path != null)
          return path;

        Path baseXdgDir = Path.of(coursierDirectories().cacheDir);
        Path xdgDir = baseXdgDir.resolve("v1");
        String xdgPath = xdgDir.toAbsolutePath().toString();

        if (Files.isDirectory(baseXdgDir))
            path = xdgPath;

        if (path == null) {
            Path coursierDotFile = Path.of(SystemProperties.getUserHome(), ".coursier");
            if (Files.isDirectory(coursierDotFile))
                path = SystemProperties.getUserHome() + "/.coursier/cache/v1/";
        }

        if (path == null) {
            path = xdgPath;
            Files.createDirectories(xdgDir);
        }

        return path;
    }

    public static Path cacheDirectory() throws IOException {

        if (cacheDirectory0 == null)
            synchronized (cacheDirectoryLock) {
                if (cacheDirectory0 == null) {
                    cacheDirectory0 = Path.of(computeCacheDirectory()).toAbsolutePath();
                }
            }

        return cacheDirectory0;
    }

    private static ProjectDirectories coursierDirectories() throws IOException {

        if (coursierDirectories0 == null)
            synchronized (coursierDirectoriesLock) {
                if (coursierDirectories0 == null) {
                    coursierDirectories0 = ProjectDirectories.from(null, null, "Coursier");
                }
            }

        return coursierDirectories0;
    }

    private static String computeConfigDirectory() throws IOException {
        String path = System.getenv("COURSIER_CONFIG_DIR");

        if (path == null)
            path = System.getProperty("coursier.config-dir");

        if (path != null)
          return path;

        return coursierDirectories().configDir;
    }

    public static Path configDirectory() throws IOException {

        if (configDirectory0 == null)
            synchronized (configDirectoryLock) {
                if (configDirectory0 == null) {
                    configDirectory0 = Path.of(computeConfigDirectory()).toAbsolutePath();
                }
            }

        return configDirectory0;
    }

    private static String computeDataLocalDirectory() throws IOException {
        String path = System.getenv("COURSIER_DATA_DIR");

        if (path == null)
            path = System.getProperty("coursier.data-dir");

        if (path != null)
          return path;

        return coursierDirectories().dataLocalDir;
    }

    public static Path dataLocalDirectory() throws IOException {

        if (dataLocalDirectory0 == null)
            synchronized (dataLocalDirectoryLock) {
                if (dataLocalDirectory0 == null) {
                    dataLocalDirectory0 = Path.of(computeDataLocalDirectory()).toAbsolutePath();
                }
            }

        return dataLocalDirectory0;
    }
}
