/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.config.plugins.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PluginProcessorPublicSetterTest {

    private static final String FAKE_PLUGIN_CLASS_PATH =
            "src/test/java/org/apache/logging/log4j/core/config/plugins/processor/" + FakePlugin.class.getSimpleName()
                    + "PublicSetter.java.source";

    private File createdFile;
    private DiagnosticCollector<JavaFileObject> diagnosticCollector;
    private List<Diagnostic<? extends JavaFileObject>> errorDiagnostics;

    @BeforeEach
    void setup() {
        setupWithOptions();
    }

    private void setupWithOptions(final String... extraOptions) {
        // Instantiate the tooling
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        diagnosticCollector = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.ROOT, UTF_8);

        // Get the source files
        Path sourceFile = Paths.get(FAKE_PLUGIN_CLASS_PATH);

        assertThat(sourceFile).exists();

        final File orig = sourceFile.toFile();
        createdFile = new File(orig.getParentFile(), "FakePluginPublicSetter.java");
        assertDoesNotThrow(() -> FileUtils.copyFile(orig, createdFile));

        // get compilation units
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(createdFile);

        final List<String> args =
                new java.util.ArrayList<>(Arrays.asList("-proc:only", "-processor", PluginProcessor.class.getName()));
        args.addAll(Arrays.asList(extraOptions));

        JavaCompiler.CompilationTask task =
                compiler.getTask(null, fileManager, diagnosticCollector, args, null, compilationUnits);
        task.call();

        errorDiagnostics = diagnosticCollector.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .collect(Collectors.toList());
    }

    @AfterEach
    void tearDown() {
        assertDoesNotThrow(() -> FileUtils.delete(createdFile));
        File pluginsDatFile = Paths.get("Log4j2Plugins.dat").toFile();
        if (pluginsDatFile.exists()) {
            pluginsDatFile.delete();
        }
    }

    @Test
    void warnWhenPluginBuilderAttributeLacksPublicSetter() {
        assertThat(errorDiagnostics).anyMatch(errorMessage -> errorMessage
                .getMessage(Locale.ROOT)
                .contains("The field `attribute` does not have a public setter"));
    }

    @Test
    void ignoreWarningWhenSuppressWarningsIsPresent() {
        assertThat(errorDiagnostics)
                .allMatch(
                        errorMessage -> !errorMessage
                                .getMessage(Locale.ROOT)
                                .contains(
                                        "The field `attributeWithoutPublicSetterButWithSuppressAnnotation` does not have a public setter"));
    }

    @Test
    void noteEmittedByDefault() {
        final List<Diagnostic<? extends JavaFileObject>> noteDiagnostics = diagnosticCollector.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.NOTE)
                .collect(Collectors.toList());
        assertThat(noteDiagnostics).anyMatch(d -> d.getMessage(Locale.ROOT).contains("writing plugin descriptor"));
    }

    @Test
    void notesSuppressedWhenMinKindIsError() {
        setupWithOptions("-A" + PluginProcessor.MIN_ALLOWED_MESSAGE_KIND_OPTION + "=ERROR");

        final List<Diagnostic<? extends JavaFileObject>> noteDiagnostics = diagnosticCollector.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.NOTE)
                .collect(Collectors.toList());
        assertThat(noteDiagnostics).noneMatch(d -> d.getMessage(Locale.ROOT).contains("writing plugin descriptor"));
    }

    @Test
    void errorsStillEmittedWhenMinKindIsError() {
        setupWithOptions("-A" + PluginProcessor.MIN_ALLOWED_MESSAGE_KIND_OPTION + "=ERROR");

        assertThat(errorDiagnostics).anyMatch(d -> d.getMessage(Locale.ROOT)
                .contains("The field `attribute` does not have a public setter"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOTE", "note"})
    void explicitNoteKindBehavesLikeDefault(final String kindValue) {
        setupWithOptions("-A" + PluginProcessor.MIN_ALLOWED_MESSAGE_KIND_OPTION + "=" + kindValue);

        assertThat(errorDiagnostics).anyMatch(d -> d.getMessage(Locale.ROOT)
                .contains("The field `attribute` does not have a public setter"));

        final List<Diagnostic<? extends JavaFileObject>> noteDiagnostics = diagnosticCollector.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.NOTE)
                .collect(Collectors.toList());
        assertThat(noteDiagnostics).anyMatch(d -> d.getMessage(Locale.ROOT).contains("writing plugin descriptor"));
    }

    @Test
    void invalidKindValueEmitsWarning() {
        setupWithOptions("-A" + PluginProcessor.MIN_ALLOWED_MESSAGE_KIND_OPTION + "=INVALID");

        final List<Diagnostic<? extends JavaFileObject>> warningDiagnostics =
                diagnosticCollector.getDiagnostics().stream()
                        .filter(d -> d.getKind() == Diagnostic.Kind.WARNING)
                        .collect(Collectors.toList());
        assertThat(warningDiagnostics)
                .anyMatch(d -> d.getMessage(Locale.ROOT).contains("unrecognized value `INVALID`"));
    }
}
