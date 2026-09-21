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
package org.apache.logging.log4j.plugin.processor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginProcessorPublicSetterTest {

    private static final String FAKE_PLUGIN_SOURCE = "/setter-test/FakePluginPublicSetter.java";

    private DiagnosticCollector<JavaFileObject> diagnosticCollector;
    private List<Diagnostic<? extends JavaFileObject>> errorDiagnostics;

    @TempDir
    private Path outputDir;

    @BeforeEach
    void setup() throws Exception {
        final URL fakePluginUrl = PluginProcessorTest.class.getResource(FAKE_PLUGIN_SOURCE);
        assertThat(fakePluginUrl).isNotNull();
        final Path fakePluginPath = Paths.get(fakePluginUrl.toURI());
        diagnosticCollector = new DiagnosticCollector<>();
        try {
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            final StandardJavaFileManager fileManager =
                    compiler.getStandardFileManager(diagnosticCollector, Locale.ROOT, UTF_8);
            try {
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Set.of(outputDir.toFile()));
                fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Set.of(outputDir.toFile()));
                final JavaCompiler.CompilationTask task = compiler.getTask(
                        null,
                        fileManager,
                        diagnosticCollector,
                        List.of("-proc:only", "-processor", PluginProcessor.class.getName()),
                        null,
                        fileManager.getJavaFileObjects(fakePluginPath));
                task.call();
            } finally {
                fileManager.close();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        errorDiagnostics = diagnosticCollector.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .collect(Collectors.toList());
    }

    @Test
    void warnWhenPluginBuilderAttributeLacksPublicSetter() {
        assertThat(errorDiagnostics).hasSize(1);
        assertThat(errorDiagnostics).anyMatch(errorMessage -> errorMessage
                .getMessage(Locale.ROOT)
                .contains("The field `attributeWithoutPublicSetter` does not have a public setter"));
    }

    @Test
    void ignoreWarningWhenSuppressWarningsIsPresent() {
        assertThat(errorDiagnostics).hasSize(1);
        assertThat(errorDiagnostics).allMatch(errorMessage -> !errorMessage
                .getMessage(Locale.ROOT)
                .contains("The field `attributeWithoutPublicSetterButWithSuppressAnnotation`"
                        + " does not have a public setter"));
    }

    @Test
    void noWarningWhenPublicSetterExists() {
        assertThat(errorDiagnostics).hasSize(1);
        assertThat(errorDiagnostics).allMatch(errorMessage -> !errorMessage
                .getMessage(Locale.ROOT)
                .contains("The field `attribute` does not have a public setter"));
    }
}
