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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.Issue;
import org.junitpioneer.jupiter.ReadsSystemProperty;

class PropertiesUtilTest {

    private final Properties properties = new Properties();

    @BeforeEach
    void setUp() throws Exception {
        properties.load(ClassLoader.getSystemResourceAsStream("PropertiesUtilTest.properties"));
    }

    @Test
    void testExtractSubset() {
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "a"));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "b."));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "c.1"));
        assertHasAllProperties(PropertiesUtil.extractSubset(properties, "dd"));
        // One invalid entry remains
        assertEquals(1, properties.size());
    }

    @Test
    void testPartitionOnCommonPrefix() {
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
    void testGetCharsetProperty() {
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
    void should_properly_parse_duration(final Duration expected, final CharSequence value) {
        assertThat(PropertiesUtil.parseDuration(value)).isEqualTo(expected);
    }

    static List<String> should_throw_on_invalid_duration() {
        return Arrays.asList(
                // more than long
                "18446744073709551616 nanos", "1 month", "invalid pattern");
    }

    @ParameterizedTest
    @MethodSource
    void should_throw_on_invalid_duration(final CharSequence value) {
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.parseDuration(value));
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    void testGetMappedProperty_sun_stdout_encoding() {
        final PropertiesUtil pu = new PropertiesUtil(System.getProperties());
        final Charset expected = System.console() == null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        assertEquals(expected, pu.getCharsetProperty("sun.stdout.encoding"));
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    void testGetMappedProperty_sun_stderr_encoding() {
        final PropertiesUtil pu = new PropertiesUtil(System.getProperties());
        final Charset expected = System.console() == null ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        assertEquals(expected, pu.getCharsetProperty("sun.err.encoding"));
    }

    @Test
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void testNonStringSystemProperties() {
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
    void testPublish() {
        final Properties props = new Properties();
        new PropertiesUtil(props);
        final String value = System.getProperty("Application");
        assertNotNull(value, "System property was not published");
        assertEquals("Log4j", value);
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    @Issue("https://github.com/spring-projects/spring-boot/issues/33450")
    @UsingStatusListener
    void testErrorPropertySource(ListStatusListener statusListener) {
        final String key = "testKey";
        final Properties props = new Properties();
        props.put(key, "test");
        final PropertiesUtil util = new PropertiesUtil(props);
        final ErrorPropertySource source = new ErrorPropertySource();
        util.addPropertySource(source);
        try {
            statusListener.clear();
            assertEquals("test", util.getStringProperty(key));
            assertTrue(source.exceptionThrown);
            assertThat(statusListener.findStatusData(Level.WARN))
                    .anySatisfy(data ->
                            assertThat(data.getMessage().getFormattedMessage()).contains("Failed"));
        } finally {
            util.removePropertySource(source);
        }
    }

    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    @Issue("https://github.com/apache/logging-log4j2/issues/3252")
    @UsingStatusListener
    void testRecursivePropertySource(ListStatusListener statusListener) {
        final String key = "testKey";
        final Properties props = new Properties();
        props.put(key, "test");
        final PropertiesUtil util = new PropertiesUtil(props);
        final PropertySource source = new RecursivePropertySource(util);
        util.addPropertySource(source);
        try {
            // We ignore the recursive source
            statusListener.clear();
            assertThat(util.getStringProperty(key)).isEqualTo("test");
            assertThat(statusListener.findStatusData(Level.WARN))
                    .anySatisfy(data -> assertThat(data.getMessage().getFormattedMessage())
                            .contains("Recursive call", "getProperty"));

            statusListener.clear();
            // To check for existence, the sources are looked up in a random order.
            assertThat(util.hasProperty(key)).isTrue();
            // To find a missing key, all the sources must be used.
            assertThat(util.hasProperty("noSuchKey")).isFalse();
            assertThat(statusListener.findStatusData(Level.WARN))
                    .anySatisfy(data -> assertThat(data.getMessage().getFormattedMessage())
                            .contains("Recursive call", "containsProperty"));
            // We check that the source is recursive
            assertThat(source.getProperty(key)).isEqualTo("test");
            assertThat(source.containsProperty(key)).isTrue();
        } finally {
            util.removePropertySource(source);
        }
    }

    private static final String[][] data = {
        {null, "org.apache.logging.log4j.level"},
        {null, "Log4jAnotherProperty"},
        {null, "log4j2.catalinaBase"},
        {"ok", "log4j.configurationFile"},
        {"ok", "Log4jDefaultStatusLevel"},
        {"ok", "org.apache.logging.log4j.newLevel"},
        {"ok", "AsyncLogger.Timeout"},
        {"ok", "AsyncLoggerConfig.RingBufferSize"},
        {"ok", "disableThreadContext"},
        {"ok", "disableThreadContextStack"},
        {"ok", "disableThreadContextMap"},
        {"ok", "isThreadContextMapInheritable"}
    };

    /**
     * LOG4J2-3413: Log4j should only resolve properties that start with a 'log4j'
     * prefix or similar.
     */
    @Test
    @ResourceLock(value = Resources.SYSTEM_PROPERTIES, mode = ResourceAccessMode.READ)
    void testResolvesOnlyLog4jProperties() {
        final PropertiesUtil util = new PropertiesUtil("Jira3413Test.properties");
        for (final String[] pair : data) {
            assertThat(util.getStringProperty(pair[1]))
                    .as("Checking property %s", pair[1])
                    .isEqualTo(pair[0]);
        }
    }

    /**
     * LOG4J2-3559: the fix for LOG4J2-3413 returns the value of 'log4j2.' for each
     * property not starting with 'log4j'.
     */
    @Test
    @ReadsSystemProperty
    void testLog4jProperty() {
        final Properties props = new Properties();
        final String incorrect = "log4j2.";
        final String correct = "not.starting.with.log4j";
        props.setProperty(incorrect, incorrect);
        props.setProperty(correct, correct);
        final PropertiesUtil util = new PropertiesUtil(props);
        assertEquals(correct, util.getStringProperty(correct));
    }

    @Test
    void should_support_multiple_sources_with_same_priority() {
        final int priority = 2003;
        final String key1 = "propertySource1";
        final Properties props1 = new Properties();
        props1.put(key1, "props1");
        final String key2 = "propertySource2";
        final Properties props2 = new Properties();
        props2.put(key2, "props2");
        final PropertiesUtil util = new PropertiesUtil(new PropertiesPropertySource(props1, priority));
        util.addPropertySource(new PropertiesPropertySource(props2, priority));
        assertThat(util.getStringProperty(key1)).isEqualTo("props1");
        assertThat(util.getStringProperty(key2)).isEqualTo("props2");
    }

    private static class ErrorPropertySource implements PropertySource {
        public boolean exceptionThrown = false;

        @Override
        public int getPriority() {
            return Integer.MIN_VALUE;
        }

        @Override
        public String getProperty(final String key) {
            exceptionThrown = true;
            throw new IllegalStateException("Test");
        }

        @Override
        public boolean containsProperty(final String key) {
            exceptionThrown = true;
            throw new IllegalStateException("Test");
        }
    }

    private static class RecursivePropertySource implements PropertySource {

        private final PropertiesUtil propertiesUtil;

        private RecursivePropertySource(PropertiesUtil propertiesUtil) {
            this.propertiesUtil = propertiesUtil;
        }

        @Override
        public int getPriority() {
            return Integer.MIN_VALUE;
        }

        @Override
        public String getProperty(String key) {
            return propertiesUtil.getStringProperty(key);
        }

        @Override
        public boolean containsProperty(String key) {
            return propertiesUtil.hasProperty(key);
        }
    }
}
