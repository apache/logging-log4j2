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
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.plugins.model.PluginEntry;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.Issue;

class PluginProcessorTest {

    private static final String CORE_NAMESPACE = "Core";
    private static final String TEST_NAMESPACE = "Test";

    private static PathClassLoader classLoader;
    private static PluginService pluginService;

    @BeforeAll
    static void setup() throws Exception {
        classLoader = new PathClassLoader();
        pluginService = generatePluginService("example");
    }

    @AfterAll
    static void cleanup() {
        pluginService = null;
        classLoader = null;
    }

    private static PluginService generatePluginService(String expectedPluginPackage, String... options)
            throws Exception {
        // Source file
        URL fakePluginUrl = PluginProcessorTest.class.getResource("/example/FakePlugin.java");
        assertThat(fakePluginUrl).isNotNull();
        Path fakePluginPath = Paths.get(fakePluginUrl.toURI());
        // Collect warnings
        WarningCollector collector = new WarningCollector();
        String fqcn = expectedPluginPackage + ".plugins.Log4jPlugins";
        Path outputDir = Files.createTempDirectory("PluginProcessorTest");

        try {
            // Instantiate the tooling
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, Locale.ROOT, UTF_8);

            // Populate sources
            Iterable<? extends JavaFileObject> sources = fileManager.getJavaFileObjects(fakePluginPath);

            // Set the target path used by `DescriptorGenerator` to dump the generated files
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Set.of(outputDir));
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, Set.of(outputDir));

            // Compile the sources
            final JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, collector, Arrays.asList(options), null, sources);
            task.setProcessors(List.of(new PluginProcessor()));
            task.call();

            // Verify successful compilation
            List<Diagnostic<? extends JavaFileObject>> diagnostics = collector.getDiagnostics();
            assertThat(diagnostics).isEmpty();

            // Find the PluginService class
            Path pluginServicePath = outputDir.resolve(fqcn.replaceAll("\\.", "/") + ".class");
            assertThat(pluginServicePath).exists();
            Class<?> pluginServiceClass = classLoader.defineClass(fqcn, pluginServicePath);
            return (PluginService) pluginServiceClass.getConstructor().newInstance();
        } finally {
            FileUtils.deleteDirectory(outputDir.toFile());
        }
    }

    @Test
    void namespaceFound() {
        assertThat(pluginService.size()).as("Number of namespaces").isNotZero();
        assertThat(pluginService.getNamespace(CORE_NAMESPACE))
                .as("Namespace %s", CORE_NAMESPACE)
                .isNotNull();
    }

    static Stream<String> checkFakePluginInformation() {
        return Stream.of("Fake", "AnotherFake", "StillFake");
    }

    @ParameterizedTest
    @MethodSource
    void checkFakePluginInformation(String aliasName) {
        PluginNamespace namespace = pluginService.getNamespace(CORE_NAMESPACE);
        assertThat(namespace).isNotNull();
        PluginType<?> pluginType = namespace.get(aliasName);
        assertThat(pluginType).as("Plugin type with alias `%s`", aliasName).isNotNull();
        verifyPluginEntry(
                pluginType.getPluginEntry(),
                aliasName.toLowerCase(Locale.ROOT),
                CORE_NAMESPACE,
                "Fake",
                "example.FakePlugin",
                "Fake",
                true,
                true);
    }

    @Test
    void checkNestedPluginInformation() {
        PluginNamespace namespace = pluginService.getNamespace(TEST_NAMESPACE);
        assertThat(namespace).isNotNull();
        PluginType<?> pluginType = namespace.get("Nested");
        assertThat(pluginType).as("Plugin type with alias `%s`", "Nested").isNotNull();
        verifyPluginEntry(
                pluginType.getPluginEntry(),
                "nested",
                TEST_NAMESPACE,
                "Nested",
                "example.FakePlugin$Nested",
                "",
                false,
                false);
    }

    @Test
    void checkPluginPackageOption() throws Exception {
        PluginService pluginService = generatePluginService("com.example", "-Alog4j.plugin.package=com.example");
        assertThat(pluginService).isNotNull();
    }

    @Test
    void checkEnableBndAnnotationsOption() {
        // If we don't have the annotations on the classpath compilation should fail
        assumeThat(areBndAnnotationsAbsent()).isTrue();
        Assertions.assertThrows(
                NullPointerException.class,
                () -> generatePluginService(
                        "com.example.bnd",
                        "-Alog4j.plugin.package=com.example.bnd",
                        "-Alog4j.plugin.enableBndAnnotations=true"));
    }

    private boolean areBndAnnotationsAbsent() {
        try {
            Class.forName("aQute.bnd.annotation.spi.ServiceConsumer");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    private void verifyPluginEntry(
            PluginEntry actual,
            String key,
            String namespace,
            String name,
            String className,
            String elementType,
            boolean deferChildren,
            boolean printable) {
        assertThat(actual.key()).as("Key").isEqualTo(key);
        assertThat(actual.namespace()).as("Namespace").isEqualTo(namespace);
        assertThat(actual.name()).as("Name").isEqualTo(name);
        assertThat(actual.className()).as("Class name").isEqualTo(className);
        assertThat(actual.elementType()).as("Element type").isEqualTo(elementType);
        assertThat(actual.deferChildren()).as("Deferred children").isEqualTo(deferChildren);
        assertThat(actual.printable()).as("Printable").isEqualTo(printable);
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/1520")
    public void testReproducibleOutputOrder() {
        assertThat(pluginService.getEntries()).isSorted();
    }

    private static class WarningCollector implements DiagnosticListener<JavaFileObject> {

        private final List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<>();

        private WarningCollector() {}

        public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
            return diagnostics;
        }

        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            switch (diagnostic.getKind()) {
                case ERROR:
                case WARNING:
                case MANDATORY_WARNING:
                    diagnostics.add(diagnostic);
                    break;
                default:
            }
        }
    }

    private static class PathClassLoader extends ClassLoader {

        public PathClassLoader() {
            super(PluginProcessorTest.class.getClassLoader());
        }

        public Class<?> defineClass(String name, Path path) throws IOException {
            final byte[] bytes;
            try (InputStream inputStream = Files.newInputStream(path)) {
                bytes = inputStream.readAllBytes();
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
