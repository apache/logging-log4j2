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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class GraalVmProcessorTest {

    private static final String FAKE_PLUGIN_NAME = "example.FakePlugin";
    private static final Object FAKE_PLUGIN = asMap(
            "name",
            FAKE_PLUGIN_NAME,
            "methods",
            asList(
                    asMap("name", "<init>", "parameterTypes", emptyList()),
                    asMap(
                            "name",
                            "newPlugin",
                            "parameterTypes",
                            asList(
                                    "int",
                                    "org.apache.logging.log4j.core.Layout",
                                    "org.apache.logging.log4j.core.config.Configuration",
                                    "org.apache.logging.log4j.core.config.Node",
                                    "org.apache.logging.log4j.core.LoggerContext",
                                    "java.lang.String"))),
            "fields",
            emptyList());
    private static final String FAKE_PLUGIN_BUILDER_NAME = FAKE_PLUGIN_NAME + "$Builder";
    private static final Object FAKE_PLUGIN_BUILDER = asMap(
            "name",
            FAKE_PLUGIN_BUILDER_NAME,
            "methods",
            emptyList(),
            "fields",
            asList(
                    asMap("name", "attribute"),
                    asMap("name", "attributeWithoutPublicSetterButWithSuppressAnnotation"),
                    asMap("name", "config"),
                    asMap("name", "layout"),
                    asMap("name", "loggerContext"),
                    asMap("name", "node"),
                    asMap("name", "value")));
    private static final String FAKE_PLUGIN_NESTED_NAME = FAKE_PLUGIN_NAME + "$Nested";
    private static final Object FAKE_PLUGIN_NESTED = onlyNoArgsConstructor(FAKE_PLUGIN_NESTED_NAME);
    private static final String FAKE_CONSTRAINT_VALIDATOR_NAME = "example.FakeAnnotations$FakeConstraintValidator";
    private static final Object FAKE_CONSTRAINT_VALIDATOR = onlyNoArgsConstructor(FAKE_CONSTRAINT_VALIDATOR_NAME);
    private static final String FAKE_PLUGIN_VISITOR_NAME = "example.FakeAnnotations$FakePluginVisitor";
    private static final Object FAKE_PLUGIN_VISITOR = onlyNoArgsConstructor(FAKE_PLUGIN_VISITOR_NAME);
    private static final String FAKE_CONVERTER_NAME = "example.FakeConverter";
    private static final Object FAKE_CONVERTER = asMap(
            "name",
            FAKE_CONVERTER_NAME,
            "methods",
            singletonList(asMap(
                    "name",
                    "newInstance",
                    "parameterTypes",
                    asList("org.apache.logging.log4j.core.config.Configuration", "java.lang.String[]"))),
            "fields",
            emptyList());

    private static final String GROUP_ID = "groupId";
    private static final String ARTIFACT_ID = "artifactId";
    private static final String FALLBACK_METADATA_FOLDER = "fooBar";

    /**
     * Generates a metadata element with just a single no-arg constructor.
     *
     * @param className The name of the metadata element.
     * @return A GraalVM metadata element.
     */
    private static Object onlyNoArgsConstructor(String className) {
        return asMap(
                "name",
                className,
                "methods",
                singletonList(asMap("name", "<init>", "parameterTypes", emptyList())),
                "fields",
                emptyList());
    }

    private static Map<String, ?> asMap(Object... pairs) {
        final Map<String, Object> map = new LinkedHashMap<>();
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("odd number of arguments: " + pairs.length);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static Path sourceDir;

    @TempDir
    private static Path outputDir;

    @BeforeAll
    static void setup() throws Exception {
        URL sourceUrl = requireNonNull(GraalVmProcessorTest.class.getResource("/GraalVmProcessorTest/java"));
        sourceDir = Paths.get(sourceUrl.toURI());
        // Generate metadata
        List<String> diagnostics = generateDescriptor(sourceDir, GROUP_ID, ARTIFACT_ID, outputDir);
        assertThat(diagnostics).isEmpty();
    }

    static Stream<Arguments> containsSpecificEntries() {
        return Stream.of(
                Arguments.of(FAKE_PLUGIN_NAME, FAKE_PLUGIN),
                Arguments.of(FAKE_PLUGIN_BUILDER_NAME, FAKE_PLUGIN_BUILDER),
                Arguments.of(FAKE_PLUGIN_NESTED_NAME, FAKE_PLUGIN_NESTED),
                Arguments.of(FAKE_CONSTRAINT_VALIDATOR_NAME, FAKE_CONSTRAINT_VALIDATOR),
                Arguments.of(FAKE_PLUGIN_VISITOR_NAME, FAKE_PLUGIN_VISITOR),
                Arguments.of(FAKE_CONVERTER_NAME, FAKE_CONVERTER));
    }

    @ParameterizedTest
    @MethodSource
    void containsSpecificEntries(String className, Object expectedJson) throws IOException {
        // Read metadata
        Path reachabilityMetadataPath =
                outputDir.resolve("META-INF/native-image/log4j-generated/groupId/artifactId/reflect-config.json");
        String reachabilityMetadata = new String(Files.readAllBytes(reachabilityMetadataPath), UTF_8);
        assertThatJson(reachabilityMetadata)
                .inPath(String.format("$[?(@.name == '%s')]", className))
                .isArray()
                .hasSize(1)
                .first()
                .isEqualTo(json(expectedJson));
    }

    static Stream<Arguments> reachabilityMetadataPath() {
        return Stream.of(
                Arguments.of(
                        "groupId",
                        "artifactId",
                        "META-INF/native-image/log4j-generated/groupId/artifactId/reflect-config.json"),
                Arguments.of(null, "artifactId", "META-INF/native-image/log4j-generated/fooBar/reflect-config.json"),
                Arguments.of("groupId", null, "META-INF/native-image/log4j-generated/fooBar/reflect-config.json"),
                Arguments.of(null, null, "META-INF/native-image/log4j-generated/fooBar/reflect-config.json"));
    }

    @ParameterizedTest
    @MethodSource
    void reachabilityMetadataPath(@Nullable String groupId, @Nullable String artifactId, String expected) {
        Messager messager = Mockito.mock(Messager.class);
        Elements elements = Mockito.mock(Elements.class);
        ProcessingEnvironment processingEnv = Mockito.mock(ProcessingEnvironment.class);
        when(processingEnv.getMessager()).thenReturn(messager);
        when(processingEnv.getElementUtils()).thenReturn(elements);
        GraalVmProcessor processor = new GraalVmProcessor();
        processor.init(processingEnv);
        assertThat(processor.getReachabilityMetadataPath(groupId, artifactId, FALLBACK_METADATA_FOLDER))
                .isEqualTo(expected);
    }

    @Test
    void whenNoGroupIdAndArtifactId_thenWarningIsPrinted(@TempDir(cleanup = CleanupMode.NEVER) Path outputDir)
            throws Exception {
        List<String> diagnostics = generateDescriptor(sourceDir, null, null, outputDir);
        assertThat(diagnostics).hasSize(1);
        // The warning message should contain the information about the missing groupId and artifactId arguments
        assertThat(diagnostics.get(0))
                .contains(
                        "recommended",
                        "-A" + GraalVmProcessor.GROUP_ID + "=<groupId>",
                        "-A" + GraalVmProcessor.ARTIFACT_ID + "=<artifactId>");
        Path path = outputDir.resolve("META-INF/native-image/log4j-generated");
        List<Path> reachabilityMetadataFolders;
        try (Stream<Path> files = Files.list(path)) {
            reachabilityMetadataFolders = files.filter(Files::isDirectory).collect(Collectors.toList());
        }
        // The generated folder name should be deterministic and based solely on the descriptor content.
        // If the descriptor changes, this test and the expected folder name must be updated accordingly.
        assertThat(reachabilityMetadataFolders).hasSize(1).containsExactly(path.resolve("e51e0522"));
        assertThat(reachabilityMetadataFolders.get(0).resolve("reflect-config.json"))
                .as("Reachability metadata file")
                .exists();
    }

    private static List<String> generateDescriptor(
            Path sourceDir, @Nullable String groupId, @Nullable String artifactId, Path outputDir) throws Exception {
        // Instantiate the tooling
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.ROOT, UTF_8);

        // Populate sources
        final Iterable<? extends JavaFileObject> sources;
        try (final Stream<Path> files = Files.walk(sourceDir)) {
            File[] sourceFiles =
                    files.filter(Files::isRegularFile).map(Path::toFile).toArray(File[]::new);
            sources = fileManager.getJavaFileObjects(sourceFiles);
        }

        // Set the target path used by `DescriptorGenerator` to dump the generated files
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(outputDir.toFile()));

        // Prepare the compiler options
        List<String> options = new ArrayList<>();
        options.add("-proc:only");
        options.add("-processor");
        options.add(GraalVmProcessor.class.getName());
        if (groupId != null) {
            options.add("-A" + GraalVmProcessor.GROUP_ID + "=" + groupId);
        }
        if (artifactId != null) {
            options.add("-A" + GraalVmProcessor.ARTIFACT_ID + "=" + artifactId);
        }

        // Compile the sources
        final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        final JavaCompiler.CompilationTask task =
                compiler.getTask(null, fileManager, diagnosticCollector, options, null, sources);
        task.call();

        // Verify successful compilation
        return diagnosticCollector.getDiagnostics().stream()
                .filter(d -> d.getKind() != Diagnostic.Kind.NOTE)
                .map(d -> d.getMessage(Locale.ROOT))
                .collect(Collectors.toList());
    }
}
