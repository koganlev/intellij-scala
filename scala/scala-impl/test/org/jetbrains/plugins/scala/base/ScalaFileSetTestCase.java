/*
 * Copyright 2000-2008 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.scala.base;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.ThrowableRunnable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.FileSetTests;
import org.jetbrains.plugins.scala.ScalaLanguage;
import org.jetbrains.plugins.scala.ScalaVersion;
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings;
import org.jetbrains.plugins.scala.util.TestUtils;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.util.text.StringUtil.endsWith;
import static com.intellij.openapi.util.text.StringUtil.startsWithChar;
import static com.intellij.psi.impl.DebugUtil.psiToString;
import static org.junit.Assert.*;

/**
 * This base class would have already been deleted, but unfortunately, we have a few hard-to-migrate scripts that
 * use functionality from this class in interesting ways.
 * <p>
 * Please do not extend this class in new tests.
 *
 * @deprecated Use {@link NoSdkFileSetTestBase} and {@link SdkFileSetTestBase} instead.
 */
@Deprecated(forRemoval = true)
public abstract class ScalaFileSetTestCase extends TestSuite {

    protected ScalaFileSetTestCase(@NotNull @NonNls String path, String... testFileExtensions) {
        String pathProperty = System.getProperty("path");
        String customOrPropertyPath = pathProperty != null ?
                pathProperty :
                getTestDataPath() + path;

        findFiles(Path.of(customOrPropertyPath))
                .filter(file -> isTestFile(file, testFileExtensions))
                .map(this::constructTestCase)
                .forEach(this::addTest);

        assertTrue("No tests found", testCount() > 0);
    }

    protected boolean needsSdk() {
        return false;
    }

    protected Test constructTestCase(Path file) {
        if (needsSdk())
            return new ActualTest(file);
        return new NoSdkTestCase(file);
    }

    public void setUp(@NotNull Project project) {
        setSettings(project);
    }

    public void tearDown(@NotNull Project project) {
    }

    @NotNull
    protected String getTestDataPath() {
        return TestUtils.getTestDataPath();
    }

    @NotNull
    protected Language getLanguage() {
        return ScalaLanguage.INSTANCE;
    }

    //used just to propagate to ActualTest.supportedIn
    //default implementation took from org.jetbrains.plugins.scala.base.ScalaSdkOwner.supportedIn
    //TODO: consider using Scala 2.13 by default
    public boolean supportedInScalaVersion(ScalaVersion version) {
        return true;
    }

    protected void setSettings(@NotNull Project project) {
        CommonCodeStyleSettings.IndentOptions indentOptions = getCommonSettings(project).getIndentOptions();
        assertNotNull(indentOptions);
        setIndentSettings(indentOptions);
    }

    protected void setIndentSettings(@NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
        indentOptions.INDENT_SIZE = 2;
        indentOptions.CONTINUATION_INDENT_SIZE = 2;
        indentOptions.TAB_SIZE = 2;
    }

    // TODO: make this method abstract and reuse implementation using e.g. mixins in parser tests
    //  this method builds psi tree string and it's only applicable to parser tests
    @NotNull
    protected String transform(@NotNull String testName,
                               @NotNull String fileText,
                               @NotNull Project project) {
        PsiFile lightFile = createLightFile(fileText, project);

        return psiToString(lightFile, true)
                .replace(": " + lightFile.getName(), "");
    }

    @NotNull
    protected String transformExpectedResult(@NotNull String text) {
        return text;
    }

    // Notice that it doesn't include the new line before and after the line with the "----" separator
    private static final Pattern FILE_PARTS_SEPARATOR_PATTERN = Pattern.compile("\\n?(?m)^-{4,}\\n?");

    /**
     * @return a list of sections from the test file, split by dashes and trimmed
     */
    protected final List<String> parseTestFileText(String textFileText) {
        // with the "limit =-1" argument, an empty content after the separator (including the new line)
        // will be treated as an empty string ""
        String[] parts = FILE_PARTS_SEPARATOR_PATTERN.split(StringUtil.convertLineSeparators(textFileText), -1);
        return Arrays.stream(parts).collect(Collectors.toList());
    }
    
    protected void runTest(@NotNull final String testName,
                           @NotNull final String testFileText,
                           @NotNull final Project project) {
        List<String> fileParts = parseTestFileText(testFileText);

        assertTrue("Test file should have at least two sections separated with ----", fileParts.size() > 1);

        final String inputRaw = fileParts.get(0);
        final String expectedResultRaw = fileParts.get(fileParts.size() - 1);

        final String testNameWithoutDot = testName.split("\\.")[0];

        final String actualResult = transform(testNameWithoutDot, inputRaw, project).trim();
        final String expectedResult = transformExpectedResult(expectedResultRaw).trim();

        if (shouldPass()) {
            assertEquals(expectedResult, actualResult);
        } else {
            assertNotEquals(expectedResult, actualResult);
        }
    }

    protected boolean shouldPass() {
        return true;
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    @Category({FileSetTests.class})
    private final class NoSdkTestCase extends LightJavaCodeInsightFixtureTestCase {
        private final Path testFile;

        private NoSdkTestCase(@NotNull Path testFile) {
            this.testFile = testFile;
        }

        @Override
        protected void setUp() throws Exception {
            super.setUp();
            ScalaFileSetTestCase.this.setUp(getProject());
        }

        @Override
        protected void tearDown() throws Exception {
            try {
                ScalaFileSetTestCase.this.tearDown(getProject());
            } finally {
                super.tearDown();
            }
        }

        @Override
        public void runTestRunnable(@NotNull ThrowableRunnable<Throwable> testRunnable) throws Throwable {
            final var fileText = Files.readString(testFile, StandardCharsets.UTF_8);
            try {
                ScalaFileSetTestCase.this.runTest(
                        testFile.getFileName().toString(),
                        StringUtil.convertLineSeparators(fileText),
                        getProject()
                );
            } catch(Throwable error) {
                // to be able to navigate to the original test file location on test failure
                // (you can use Ctrl/Cmd + Click in the console)
                // (note, might not work with Android plugin disabled, see IDEA-257969)
                System.err.println("### Test file: " + testFile.toAbsolutePath());
                throw error;
            }
        }

        @NotNull
        @Override
        public String toString() {
            return getName();
        }

        @NotNull
        @Override
        public String getName() {
            final var name = testFile.getFileName().toString();
            final var dotIndex = name.lastIndexOf('.');
            if (dotIndex == -1) {
                return name;
            }
            return name.substring(0, dotIndex);
        }
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    @Category({FileSetTests.class})
    private final class ActualTest extends ScalaLightCodeInsightFixtureTestCase {

        private final Path myTestFile;

        private ActualTest(@NotNull Path testFile) {
            myTestFile = testFile;
        }

        @Override
        public boolean supportedIn(ScalaVersion version) {
            return supportedInScalaVersion(version);
        }

        @Override
        public void setUp() {
            try {
                super.setUp();
                ScalaFileSetTestCase.this.setUp(getProject());
            } catch (Exception e) {
                try {
                    tearDown();
                } catch (Exception ignored) {
                }
                throw e;
            }
        }

        @Override
        public void tearDown() {
            ScalaFileSetTestCase.this.tearDown(getProject());
            try {
                super.tearDown();
            } catch (IllegalArgumentException ignored) {
            }
        }

        @Override
        public void runTestRunnable(@NotNull ThrowableRunnable<Throwable> testRunnable) throws Throwable {
            String fileText = new String(Files.readAllBytes(myTestFile), StandardCharsets.UTF_8);
            try {
                ScalaFileSetTestCase.this.runTest(
                        myTestFile.getFileName().toString(),
                        StringUtil.convertLineSeparators(fileText),
                        getProject()
                );
            } catch(Throwable error) {
                // to be able to navigate to the original test file location on test failure
                // (you can use Ctrl/Cmd + Click in the console)
                // (note, might not work with Android plugin disabled, see IDEA-257969)
                System.err.println("### Test file: " + myTestFile.toAbsolutePath());
                throw error;
            }
        }

        @NotNull
        @Override
        protected String getTestName(boolean lowercaseFirstLetter) {
            return "";
        }

        @NotNull
        @Override
        public String toString() {
            return getName() + " ";
        }

        @NotNull
        @Override
        public String getName() {
            final var name = myTestFile.getFileName().toString();
            final var dotIndex = name.lastIndexOf('.');
            if (dotIndex == -1) {
                return name;
            }
            return name.substring(0, dotIndex);
        }
    }

    @NotNull
    protected final ScalaCodeStyleSettings getScalaSettings(@NotNull Project project) {
        return getSettings(project).getCustomSettings(ScalaCodeStyleSettings.class);
    }

    @NotNull
    protected final CommonCodeStyleSettings getCommonSettings(@NotNull Project project) {
        return getSettings(project).getCommonSettings(ScalaLanguage.INSTANCE);
    }

    protected final PsiFile createLightFile(@NotNull @NonNls String text,
                                            @NotNull Project project) {
        return PsiFileFactory.getInstance(project).createFileFromText(
                "dummy.scala",
                getLanguage(),
                text
        );
    }

    @NotNull
    private static CodeStyleSettings getSettings(@NotNull Project project) {
        return CodeStyle.getSettings(project);
    }

    @NotNull
    private static Stream<Path> findFiles(@NotNull Path baseFile) {
        if (Files.exists(baseFile)) {
            List<Path> myFiles = new ArrayList<>();
            scanForFiles(baseFile, myFiles);
            return myFiles.stream();
        } else {
            return Stream.empty();
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static void scanForFiles(@NotNull Path directory,
                                     @NotNull List<Path> accumulator) {
        // recursively scan for all subdirectories
        if (Files.isDirectory(directory)) {
            try (Stream<Path> stream = Files.list(directory)) {
                stream.forEach(file -> {
                    if (Files.isDirectory(file)) {
                        scanForFiles(file, accumulator);
                    } else {
                        accumulator.add(file);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean isTestFile(@NotNull Path file, String[] testFileExtensions) {
        String path = file.toAbsolutePath().toString();
        String name = file.getFileName().toString();

        if (testFileExtensions.length == 0) {
            testFileExtensions = new String[] { ".test" };
        }

        return !path.contains(".svn") &&
                !path.contains(".cvs") &&
                Arrays.stream(testFileExtensions).anyMatch(ext -> endsWith(name, ext)) &&
                !startsWithChar(name, '_') &&
                !"CVS".equals(name);
    }
}
