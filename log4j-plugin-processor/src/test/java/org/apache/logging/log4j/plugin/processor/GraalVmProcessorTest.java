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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraalVmProcessorTest {
    static final String GROUP_ID = "org.apache.logging.log4j";
    static final String ARTIFACT_ID = "log4j-plugin-processor-test";
    static final Path REFLECT_CONFIG_PATH =
            Path.of("META-INF", "native-image", "log4j-generated", GROUP_ID, ARTIFACT_ID, "reflect-config.json");

    static String readExpectedReflectConfig() throws IOException {
        var url = Objects.requireNonNull(GraalVmProcessorTest.class.getResource("/expected-reflect-config.json"));
        try (var inputStream = url.openStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static String readActualReflectConfig(Path baseDirectory) throws IOException {
        return Files.readString(baseDirectory.resolve(REFLECT_CONFIG_PATH));
    }

    static List<Path> findInputSourceFiles() throws IOException {
        try (var stream = Files.list(Path.of("src", "test", "resources", "example"))) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    void verifyAnnotationProcessorGeneratesExpectedReachability(@TempDir Path outputDir) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDir));
        fileManager.setLocationFromPaths(StandardLocation.SOURCE_OUTPUT, List.of(outputDir));
        var sourceFiles = fileManager.getJavaFileObjectsFromPaths(findInputSourceFiles());
        var options = List.of("-Alog4j.graalvm.groupId=" + GROUP_ID, "-Alog4j.graalvm.artifactId=" + ARTIFACT_ID);
        var task = compiler.getTask(null, fileManager, null, options, null, sourceFiles);
        task.setProcessors(List.of(new GraalVmProcessor()));
        assertEquals(true, task.call());
        String expected = readExpectedReflectConfig();
        String actual = readActualReflectConfig(outputDir);
        assertEquals(expected, actual);
    }
}
