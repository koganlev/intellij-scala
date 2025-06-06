package org.jetbrains.plugins.scala.project.sdkdetect.repository;

import dev.dirs.ProjectDirectories;
import dev.dirs.impl.Windows;
import dev.dirs.jni.WindowsJni;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Computes Coursier's directories according to the standard
 * defined by operating system Coursier is running on.
 * <p>
 * Copied verbatim from
 * <a href="https://github.com/coursier/coursier/blob/a8fdaf7a8e3ea62e24ed6b06cbd4e943a7a67f64/modules/paths/src/main/java/coursier/paths/CoursierPaths.java">CoursierPaths.java</a>
 * with the intention of eventually depending on the code directly.
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
    private static volatile Path archiveCacheDirectory0 = null;
    private static volatile Path priviledgedArchiveCacheDirectory0 = null;
    private static volatile Path digestBasedCacheDirectory0 = null;
    private static volatile Path jvmCacheDirectory0 = null;

    private static final Object configDirectoryLock = new Object();
    private static volatile Path[] configDirectories0 = null;

    private static final Object dataLocalDirectoryLock = new Object();
    private static volatile Path dataLocalDirectory0 = null;

    // TODO After switching to nio, that logic can be unit tested with mock filesystems.

    private static String computeCacheDirectory() throws IOException {
        return computeCacheDirectory("COURSIER_CACHE", "coursier.cache", "v1");
    }

    private static String computeArchiveCacheDirectory() throws IOException {
        return computeCacheDirectory("COURSIER_ARCHIVE_CACHE", "coursier.archive.cache", "arc");
    }

    private static String computePriviledgedArchiveCacheDirectory() throws IOException {
        return computeCacheDirectory("COURSIER_PRIVILEDGED_ARCHIVE_CACHE", "coursier.priviledged.archive.cache", "priv");
    }

    private static String computeDigestBasedCacheDirectory() throws IOException {
        return computeCacheDirectory("COURSIER_DIGEST_BASED_CACHE", "coursier.digest-based.cache", "digest");
    }

    private static String computeJvmCacheDirectory() throws IOException {
        return computeCacheDirectory("COURSIER_JVM_CACHE", "coursier.jvm.cache", "jvm");
    }

    private static String computeCacheDirectory(String envVar, String propName, String dirName) throws IOException {
        String path = System.getenv(envVar);

        if (path == null)
            path = System.getProperty(propName);

        if (path != null)
            return path;

        Path baseXdgDir = Paths.get(coursierDirectories().cacheDir);
        Path xdgDir = baseXdgDir.resolve(dirName);

        createDirectories(xdgDir);

        return xdgDir.toAbsolutePath().normalize().toString();
    }

    public static File cacheDirectory() throws IOException {
        return cacheDirectoryPath().toFile();
    }

    public static Path cacheDirectoryPath() throws IOException {

        if (cacheDirectory0 == null)
            synchronized (cacheDirectoryLock) {
                if (cacheDirectory0 == null) {
                    cacheDirectory0 = Paths.get(computeCacheDirectory()).toAbsolutePath().normalize();
                }
            }

        return cacheDirectory0;
    }

    public static File archiveCacheDirectory() throws IOException {
        return archiveCacheDirectoryPath().toFile();
    }

    public static Path archiveCacheDirectoryPath() throws IOException {

        if (archiveCacheDirectory0 == null)
            synchronized (cacheDirectoryLock) {
                if (archiveCacheDirectory0 == null) {
                    archiveCacheDirectory0 = Paths.get(computeArchiveCacheDirectory()).toAbsolutePath().normalize();
                }
            }

        return archiveCacheDirectory0;
    }

    public static File priviledgedArchiveCacheDirectory() throws IOException {
        return priviledgedArchiveCacheDirectoryPath().toFile();
    }

    public static Path priviledgedArchiveCacheDirectoryPath() throws IOException {

        if (priviledgedArchiveCacheDirectory0 == null)
            synchronized (cacheDirectoryLock) {
                if (priviledgedArchiveCacheDirectory0 == null) {
                    priviledgedArchiveCacheDirectory0 = Paths.get(computePriviledgedArchiveCacheDirectory()).toAbsolutePath().normalize();
                }
            }

        return priviledgedArchiveCacheDirectory0;
    }

    public static File digestBasedCacheDirectory() throws IOException {
        return digestBasedCacheDirectoryPath().toFile();
    }

    public static Path digestBasedCacheDirectoryPath() throws IOException {

        if (digestBasedCacheDirectory0 == null)
            synchronized (cacheDirectoryLock) {
                if (digestBasedCacheDirectory0 == null) {
                    digestBasedCacheDirectory0 = Paths.get(computeDigestBasedCacheDirectory()).toAbsolutePath().normalize();
                }
            }

        return digestBasedCacheDirectory0;
    }

    public static File jvmCacheDirectory() throws IOException {
        return jvmCacheDirectoryPath().toFile();
    }

    public static Path jvmCacheDirectoryPath() throws IOException {

        if (jvmCacheDirectory0 == null)
            synchronized (cacheDirectoryLock) {
                if (jvmCacheDirectory0 == null) {
                    jvmCacheDirectory0 = Paths.get(computeJvmCacheDirectory()).toAbsolutePath().normalize();
                }
            }

        return jvmCacheDirectory0;
    }

    public static ProjectDirectories directoriesInstance(String name) {
        Supplier<Windows> windows;
        if (useJni())
            windows = WindowsJni.getJdkAwareSupplier();
        else
            windows = Windows.getDefaultSupplier();
        return ProjectDirectories.from(null, null, name, windows);
    }

    private static ProjectDirectories coursierDirectories() throws IOException {

        if (coursierDirectories0 == null)
            synchronized (coursierDirectoriesLock) {
                if (coursierDirectories0 == null) {
                    coursierDirectories0 = directoriesInstance("Coursier");
                }
            }

        return coursierDirectories0;
    }

    private static Path[] computeConfigDirectories() throws IOException {
        String path = System.getenv("COURSIER_CONFIG_DIR");

        if (path == null)
            path = System.getProperty("coursier.config-dir");

        if (path != null)
            return new Path[] { Paths.get(path).toAbsolutePath().normalize() };

        String configDir = coursierDirectories().configDir;
        String preferenceDir = coursierDirectories().preferenceDir;
        if (configDir.equals(preferenceDir))
            return new Path[] {
                    Paths.get(configDir).toAbsolutePath().normalize(),
            };
        else
            return new Path[] {
                    Paths.get(configDir).toAbsolutePath().normalize(),
                    Paths.get(preferenceDir).toAbsolutePath().normalize()
            };
    }

    public static File[] configDirectories() throws IOException {
        return Arrays.stream(configDirectoriesPaths()).map(Path::toFile).toArray(File[]::new);
    }

    public static Path[] configDirectoriesPaths() throws IOException {

        if (configDirectories0 == null)
            synchronized (configDirectoryLock) {
                if (configDirectories0 == null) {
                    configDirectories0 = computeConfigDirectories();
                }
            }

        return configDirectories0.clone();
    }

    @Deprecated
    public static File configDirectory() throws IOException {
        return configDirectories()[0];
    }

    public static File defaultConfigDirectory() throws IOException {
        return configDirectories()[0];
    }

    public static Path defaultConfigDirectoryPath() throws IOException {
        return configDirectoriesPaths()[0];
    }

    private static String computeDataLocalDirectory() throws IOException {
        String path = System.getenv("COURSIER_DATA_DIR");

        if (path == null)
            path = System.getProperty("coursier.data-dir");

        if (path != null)
            return path;

        return coursierDirectories().dataLocalDir;
    }

    public static File dataLocalDirectory() throws IOException {
        return dataLocalDirectoryPath().toFile();
    }

    public static Path dataLocalDirectoryPath() throws IOException {

        if (dataLocalDirectory0 == null)
            synchronized (dataLocalDirectoryLock) {
                if (dataLocalDirectory0 == null) {
                    dataLocalDirectory0 = Paths.get(computeDataLocalDirectory()).toAbsolutePath().normalize();
                }
            }

        return dataLocalDirectory0;
    }

    public static File projectCacheDirectory() throws IOException {
        return new File(coursierDirectories().cacheDir);
    }

    public static Path projectCacheDirectoryPath() throws IOException {
        return Paths.get(coursierDirectories().cacheDir);
    }

    private static Path scalaConfigFile0 = null;

    public static Path scalaConfigFile() throws Throwable {
        if (scalaConfigFile0 == null) {
            Path configPath = null;
            String fromEnv = System.getenv("SCALA_CLI_CONFIG");
            if (fromEnv != null && fromEnv.length() > 0)
                configPath = Paths.get(fromEnv);
            if (configPath == null) {
                String fromProps = System.getProperty("scala-cli.config");
                if (fromProps != null && fromProps.length() > 0)
                    configPath = Paths.get(fromProps);
            }
            if (configPath == null) {
                ProjectDirectories dirs = CoursierPaths.directoriesInstance("ScalaCli");
                configPath = Paths.get(dirs.dataLocalDir).resolve("secrets/config.json");
            }

            scalaConfigFile0 = configPath;
        }
        return scalaConfigFile0;
    }

    /**
     * Copied verbatim from
     * <a href="https://github.com/coursier/coursier/blob/a8fdaf7a8e3ea62e24ed6b06cbd4e943a7a67f64/modules/paths/src/main/java/coursier/paths/Util.java#L82-L91">Util.createDirectories</a>
     * with the intention of eventually depending on the code directly.
     */
    private static void createDirectories(Path path) throws IOException {
        try {
            Files.createDirectories(path);
        } catch (FileAlreadyExistsException ex) {
            // see https://bugs.openjdk.java.net/browse/JDK-8130464
            // Files.createDirectories does that check too, but with LinkOptions.NOFOLLOW_LINKS
            if (!Files.isDirectory(path))
                throw ex;
        }
    }

    /**
     * Copied verbatim from
     * <a href="https://github.com/coursier/coursier/blob/a8fdaf7a8e3ea62e24ed6b06cbd4e943a7a67f64/modules/paths/src/main/java/coursier/paths/Util.java#L158-L204">Util.useJni</a>
     * with the intention of eventually depending on the code directly.
     */
    private static Boolean useJni0 = null;
    private static boolean useJni() {
        return useJni(() -> {});
    }
    private static boolean useJni(Runnable beforeJni) {
        if (useJni0 != null)
            return useJni0;

        boolean isWindows = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("windows");
        if (!isWindows) {
            useJni0 = false;
            return useJni0;
        }

        String prop = System.getenv("COURSIER_JNI");
        if (prop == null || prop.isEmpty())
            prop = System.getProperty("coursier.jni", "");

        boolean force = prop.equalsIgnoreCase("force");
        if (force) {
            beforeJni.run();
            useJni0 = true;
            return useJni0;
        }

        boolean disabled = prop.equalsIgnoreCase("false");
        if (disabled) {
            useJni0 = false;
            return useJni0;
        }

        // Try to get a dummy user env var from registry. If it fails, assume the JNI stuff is broken,
        // and fallback on PowerShell scripts.
        try {
            beforeJni.run();
            coursier.jniutils.WindowsEnvironmentVariables.get("PATH");
            useJni0 = true;
        } catch (Throwable t) {
            if (System.getProperty("coursier.jni.check.throw", "").equalsIgnoreCase("true"))
                throw new RuntimeException(t);
            useJni0 = false;
        }

        return useJni0;
    }
}
