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
package org.apache.logging.log4j.util;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.ReadsSystemProperty;

public class PropertiesUtilTest {

    private final Properties properties = new Properties();

    @BeforeEach
    public void setUp() throws Exception {
        properties.load(ClassLoader.getSystemResourceAsStream("PropertiesUtilTest.properties"));
    }

    @Test
    public void testExtractSubset() {
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "a"));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "b."));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "c.1"));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "dd"));
        // One invalid entry remains
        assertEquals(1, properties.size());
    }

    @Test
    public void testPartitionOnCommonPrefix() {
        final Map<String, Properties> parts = PropertiesUtil.partitionOnCommonPrefixes(properties);
        assertEquals(4, parts.size());
        assertHasAllProperties(parts.get("a"));
        assertHasAllProperties(parts.get("b"));
        assertHasAllProperties(
                PropertiesUtil.partitionOnCommonPrefixes(parts.get("c")).get("1"));
        assertHasAllProperties(parts.get("dd"));
    }

    private static void assertHasAllProperties(final Properties properties) {
        assertNotNull(properties);
        assertEquals("1", properties.getProperty("1"));
        assertEquals("2", properties.getProperty("2"));
        assertEquals("3", properties.getProperty("3"));
    }

    @Test
    public void testGetCharsetProperty() {
        final Properties p = new Properties();
        p.setProperty("e.1", StandardCharsets.US_ASCII.name());
        p.setProperty("e.2", "wrong-charset-name");
        final PropertiesUtil pu = new PropertiesUtil(p);

        assertEquals(Charset.defaultCharset(), pu.getCharsetProperty("e.0"));
        assertEquals(StandardCharsets.US_ASCII, pu.getCharsetProperty("e.1"));
        assertEquals(Charset.defaultCharset(), pu.getCharsetProperty("e.2"));
    }

    static Stream<Arguments> should_properly_parse_duration() {
        return Stream.of(
                Arguments.of(Duration.of(1, NANOS), "1 ns"),
                Arguments.of(Duration.of(1, NANOS), "1 nano"),
                Arguments.of(Duration.of(3, NANOS), "3 nanos"),
                Arguments.of(Duration.of(1, NANOS), "1 nanosecond"),
                Arguments.of(Duration.of(5, NANOS), "5 nanoseconds"),
                Arguments.of(Duration.of(6, MICROS), "6 us"),
                Arguments.of(Duration.of(1, MICROS), "1 micro"),
                Arguments.of(Duration.of(8, MICROS), "8 micros"),
                Arguments.of(Duration.of(1, MICROS), "1 microsecond"),
                Arguments.of(Duration.of(10, MICROS), "10 microseconds"),
                Arguments.of(Duration.of(11, MILLIS), "11 ms"),
                Arguments.of(Duration.of(1, MILLIS), "1 milli"),
                Arguments.of(Duration.of(13, MILLIS), "13 millis"),
                Arguments.of(Duration.of(1, MILLIS), "1 millisecond"),
                Arguments.of(Duration.of(15, MILLIS), "15 milliseconds"),
                Arguments.of(Duration.of(16, SECONDS), "16 s"),
                Arguments.of(Duration.of(1, SECONDS), "1 second"),
                Arguments.of(Duration.of(18, SECONDS), "18 seconds"),
                Arguments.of(Duration.of(19, MINUTES), "19 m"),
                Arguments.of(Duration.of(1, MINUTES), "1 minute"),
                Arguments.of(Duration.of(21, MINUTES), "21 minutes"),
                Arguments.of(Duration.of(22, HOURS), "22 h"),
                Arguments.of(Duration.of(1, HOURS), "1 hour"),
                Arguments.of(Duration.of(24, HOURS), "24 hours"),
                Arguments.of(Duration.of(25, DAYS), "25 d"),
                Arguments.of(Duration.of(1, DAYS), "1 day"),
                Arguments.of(Duration.of(27, DAYS), "27 days"),
                Arguments.of(Duration.of(28, MILLIS), "28"));
    }

    @ParameterizedTest
    @MethodSource
    void should_properly_parse_duration(final Duration expected, final String value) {
        Assertions.assertThat(PropertiesUtil.parseDuration(value)).isEqualTo(expected);
    }

    static List<String> should_throw_on_invalid_duration() {
        return Arrays.asList(
                // more than long
                "18446744073709551616 nanos", "1 month", "invalid pattern");
    }

    @ParameterizedTest
    @MethodSource
    void should_throw_on_invalid_duration(final String value) {
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.parseDuration(value));
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testGetMappedProperty_sun_stdout_encoding() {
        final PropertiesUtil pu = new PropertiesUtil(System.getProperties());
        final Charset expected = System.console() == null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        assertEquals(expected, pu.getCharsetProperty("sun.stdout.encoding"));
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testGetMappedProperty_sun_stderr_encoding() {
        final PropertiesUtil pu = new PropertiesUtil(System.getProperties());
        final Charset expected = System.console() == null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        assertEquals(expected, pu.getCharsetProperty("sun.err.encoding"));
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    public void testNonStringSystemProperties() {
        final Object key1 = "1";
        final Object key2 = new Object();
        System.getProperties().put(key1, new Object());
        System.getProperties().put(key2, "value-2");
        try {
            final PropertiesUtil util = new PropertiesUtil(new Properties());
            assertNull(util.getStringProperty("1"));
        } finally {
            System.getProperties().remove(key1);
            System.getProperties().remove(key2);
        }
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testPublish() {
        final Properties props = new Properties();
        final PropertiesUtil util = new PropertiesUtil(props);
        final String value = System.getProperty("Application");
        assertNotNull(value, "System property was not published");
        assertEquals("Log4j", value);
    }

    private static final String[][] data = {
        {null, "org.apache.logging.log4j.level"},
        {null, "Log4jAnotherProperty"},
        {null, "log4j2.catalinaBase"},
        {"ok", "log4j2.configurationFile"},
        {"ok", "log4j2.defaultStatusLevel"},
        {"ok", "log4j2.newLevel"},
        {"ok", "log4j2.asyncLoggerTimeout"},
        {"ok", "log4j2.asyncLoggerConfigRingBufferSize"},
        {"ok", "log4j2.disableThreadContext"},
        {"ok", "log4j2.disableThreadContextStack"},
        {"ok", "log4j2.disableThreadContextMap"},
        {"ok", "log4j2.isThreadContextMapInheritable"}
    };

    /**
     * LOG4J2-3413: Log4j should only resolve properties that start with a 'log4j'
     * prefix or similar.
     */
    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    public void testResolvesOnlyLog4jProperties() {
        final PropertiesUtil util = new PropertiesUtil("Jira3413Test.properties");
        for (final String[] pair : data) {
            assertEquals(pair[0], util.getStringProperty(pair[1]));
        }
    }

    /**
     * LOG4J2-3559: the fix for LOG4J2-3413 returns the value of 'log4j2.' for each
     * property not starting with 'log4j'.
     */
    @Test
    @ReadsSystemProperty
    public void testLog4jProperty() {
        final Properties props = new Properties();
        final String incorrect = "log4j2.";
        final String correct = "not.starting.with.log4j";
        props.setProperty(incorrect, incorrect);
        props.setProperty(correct, correct);
        final PropertiesUtil util = new PropertiesUtil(props);
        assertEquals(correct, util.getStringProperty(correct));
    }
}
