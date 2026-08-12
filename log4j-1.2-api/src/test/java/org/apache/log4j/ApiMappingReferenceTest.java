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
package org.apache.log4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Validates the Log4j 1.x to 2.x API mapping reference JSON.
 *
 * <p>The canonical mapping file lives at
 * {@code src/site/antora/modules/ROOT/attachments/api-mapping-1x-to-2x.json}.
 * A copy under {@code src/test/resources/} is kept in sync for classpath loading in tests.</p>
 */
class ApiMappingReferenceTest {

    private static final String RESOURCE_PATH = "api-mapping-1x-to-2x.json";

    private static final String CANONICAL_RELATIVE_PATH =
            "src/site/antora/modules/ROOT/attachments/api-mapping-1x-to-2x.json";

    private static final Set<String> REQUIRED_FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "sourceClass",
            "targetClass",
            "bridgeClass",
            "bridgeMechanism",
            "behavioralDifferences",
            "category",
            "deprecationNotes")));

    private static final Set<String> REQUIRED_SOURCE_CLASSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "org.apache.log4j.Logger",
            "org.apache.log4j.Level",
            "org.apache.log4j.LogManager",
            "org.apache.log4j.Category",
            "org.apache.log4j.Appender",
            "org.apache.log4j.Layout",
            "org.apache.log4j.PatternLayout",
            "org.apache.log4j.BasicConfigurator",
            "org.apache.log4j.PropertyConfigurator",
            "org.apache.log4j.xml.DOMConfigurator",
            "org.apache.log4j.MDC",
            "org.apache.log4j.NDC",
            "org.apache.log4j.Priority")));

    private static final int MIN_CONCRETE_APPENDERS = 5;

    @Test
    void mappingReferenceIsValid() throws Exception {
        final List<Map<String, Object>> entries = loadMappingEntries();
        assertFalse(entries.isEmpty(), "mapping must contain at least one entry");

        for (final Map<String, Object> entry : entries) {
            assertRequiredFieldsPresent(entry);
            assertBehavioralDifferencesPresent(entry);
            assertClassesResolvable(entry);
        }

        assertRequiredSourceClassesCovered(entries);
        assertMinimumConcreteAppenders(entries);
        assertCanonicalFileMatchesClasspathCopy();
    }

    private static List<Map<String, Object>> loadMappingEntries() throws IOException {
        try (InputStream inputStream =
                ApiMappingReferenceTest.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            assertNotNull(inputStream, "missing classpath resource: " + RESOURCE_PATH);
            return new ObjectMapper().readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        }
    }

    private static void assertRequiredFieldsPresent(final Map<String, Object> entry) {
        for (final String field : REQUIRED_FIELDS) {
            assertTrue(entry.containsKey(field), "entry for " + entry.get("sourceClass") + " missing field: " + field);
        }
    }

    private static void assertBehavioralDifferencesPresent(final Map<String, Object> entry) {
        final Object differences = entry.get("behavioralDifferences");
        assertNotNull(differences, "behavioralDifferences must not be null for " + entry.get("sourceClass"));
        assertTrue(differences instanceof List, "behavioralDifferences must be a list for " + entry.get("sourceClass"));
        assertFalse(
                ((List<?>) differences).isEmpty(),
                "behavioralDifferences must contain at least one item for " + entry.get("sourceClass"));
    }

    private static void assertClassesResolvable(final Map<String, Object> entry) throws ClassNotFoundException {
        final String sourceClass = (String) entry.get("sourceClass");
        final String targetClass = (String) entry.get("targetClass");
        final String bridgeClass = (String) entry.get("bridgeClass");

        Class.forName(targetClass);
        Class.forName(bridgeClass);
        try {
            Class.forName(sourceClass);
        } catch (final ClassNotFoundException ex) {
            assertTrue(
                    bridgeClass.startsWith("org.apache.log4j.builders."),
                    "source class not on classpath and bridge is not a builder: " + sourceClass);
        }
    }

    private static void assertRequiredSourceClassesCovered(final List<Map<String, Object>> entries) {
        final Set<String> mappedSources =
                entries.stream().map(entry -> (String) entry.get("sourceClass")).collect(Collectors.toSet());

        for (final String required : REQUIRED_SOURCE_CLASSES) {
            assertTrue(mappedSources.contains(required), "missing required mapping for " + required);
        }
    }

    private static void assertMinimumConcreteAppenders(final List<Map<String, Object>> entries) {
        final long appenderCount = entries.stream()
                .filter(entry -> "appender".equals(entry.get("category")))
                .filter(entry -> !"org.apache.log4j.Appender".equals(entry.get("sourceClass")))
                .count();
        assertTrue(
                appenderCount >= MIN_CONCRETE_APPENDERS,
                "expected at least " + MIN_CONCRETE_APPENDERS + " concrete appender mappings, found " + appenderCount);
    }

    private static void assertCanonicalFileMatchesClasspathCopy() throws IOException {
        Path canonicalPath = null;
        for (final String relativePath :
                new String[] {CANONICAL_RELATIVE_PATH, "../" + CANONICAL_RELATIVE_PATH}) {
            final Path candidate = Paths.get("").toAbsolutePath().resolve(relativePath).normalize();
            if (Files.exists(candidate)) {
                canonicalPath = candidate;
                break;
            }
        }
        if (canonicalPath == null) {
            return;
        }
        final String canonical = new String(Files.readAllBytes(canonicalPath), StandardCharsets.UTF_8);
        try (InputStream inputStream =
                ApiMappingReferenceTest.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            assertNotNull(inputStream);
            final String classpathCopy = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);
            assertTrue(
                    canonical.equals(classpathCopy),
                    "test/resources copy must match canonical Antora attachment at " + CANONICAL_RELATIVE_PATH);
        }
    }

    private static byte[] readAllBytes(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }
}
