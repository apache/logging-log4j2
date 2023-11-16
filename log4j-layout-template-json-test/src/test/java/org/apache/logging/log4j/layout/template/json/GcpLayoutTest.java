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
package org.apache.logging.log4j.layout.template.json;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.CONFIGURATION;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.JAVA_BASE_PREFIX;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.usingSerializedLogEventAccessor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

class GcpLayoutTest {

    private static final JsonTemplateLayout LAYOUT = JsonTemplateLayout.newBuilder()
            .setConfiguration(CONFIGURATION)
            .setStackTraceEnabled(true)
            .setLocationInfoEnabled(true)
            .setEventTemplateUri("classpath:GcpLayout.json")
            .build();

    private static final int LOG_EVENT_COUNT = 1_000;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    @Test
    void test_lite_log_events() {
        LogEventFixture.createLiteLogEvents(LOG_EVENT_COUNT).forEach(GcpLayoutTest::verifySerialization);
    }

    @Test
    void test_full_log_events() {
        LogEventFixture.createFullLogEvents(LOG_EVENT_COUNT).forEach(GcpLayoutTest::verifySerialization);
    }

    private static void verifySerialization(final LogEvent logEvent) {
        usingSerializedLogEventAccessor(LAYOUT, logEvent, accessor -> {

            // Verify timestamp.
            final String expectedTimestamp = formatLogEventInstant(logEvent);
            assertThat(accessor.getString("timestamp")).isEqualTo(expectedTimestamp);

            // Verify severity.
            final Level level = logEvent.getLevel();
            final String expectedSeverity;
            if (Level.WARN.equals(level)) {
                expectedSeverity = "WARNING";
            } else if (Level.TRACE.equals(level)) {
                expectedSeverity = "TRACE";
            } else if (Level.FATAL.equals(level)) {
                expectedSeverity = "EMERGENCY";
            } else {
                expectedSeverity = level.name();
            }
            assertThat(accessor.getString("severity")).isEqualTo(expectedSeverity);

            // Verify message.
            final Throwable exception = logEvent.getThrown();
            if (exception != null) {
                final String actualMessage = accessor.getString("message");
                assertThat(actualMessage)
                        .contains(logEvent.getMessage().getFormattedMessage())
                        .contains(exception.getLocalizedMessage())
                        .contains("at org.apache.logging.log4j.layout.template.json")
                        .contains("at " + JAVA_BASE_PREFIX + "java.lang.reflect.Method")
                        .contains("at org.junit.platform.engine");
            }

            // Verify labels.
            logEvent.getContextData().forEach((key, value) -> {
                final String expectedValue = String.valueOf(value);
                final String actualValue = accessor.getString(new String[] {"logging.googleapis.com/labels", key});
                assertThat(actualValue).isEqualTo(expectedValue);
            });

            final StackTraceElement source = logEvent.getSource();
            if (source != null) {

                // Verify file name.
                final String actualFileName =
                        accessor.getString(new String[] {"logging.googleapis.com/sourceLocation", "file"});
                assertThat(actualFileName).isEqualTo(source.getFileName());

                // Verify line number.
                final int actualLineNumber =
                        accessor.getInteger(new String[] {"logging.googleapis.com/sourceLocation", "line"});
                assertThat(actualLineNumber).isEqualTo(source.getLineNumber());

                // Verify function.
                final String expectedFunction = source.getClassName() + "." + source.getMethodName();
                final String actualFunction =
                        accessor.getString(new String[] {"logging.googleapis.com/sourceLocation", "function"});
                assertThat(actualFunction).isEqualTo(expectedFunction);

            } else {
                assertThat(accessor.exists(new String[] {"logging.googleapis.com/sourceLocation", "file"}))
                        .isFalse();
                assertThat(accessor.exists(new String[] {"logging.googleapis.com/sourceLocation", "line"}))
                        .isFalse();
                assertThat(accessor.getString(new String[] {"logging.googleapis.com/sourceLocation", "function"}))
                        .isEmpty();
            }

            // Verify insert id.
            assertThat(accessor.getString("logging.googleapis.com/insertId")).matches("[-]?[0-9]+");

            // Verify exception.
            if (exception != null) {

                // Verify exception class.
                assertThat(accessor.getString(new String[] {"_exception", "class"}))
                        .isEqualTo(exception.getClass().getCanonicalName());

                // Verify exception message.
                assertThat(accessor.getString(new String[] {"_exception", "message"}))
                        .isEqualTo(exception.getMessage());

                // Verify exception stack trace.
                assertThat(accessor.getString(new String[] {"_exception", "stackTrace"}))
                        .contains(exception.getLocalizedMessage())
                        .contains("at org.apache.logging.log4j.layout.template.json")
                        .contains("at " + JAVA_BASE_PREFIX + "java.lang.reflect.Method")
                        .contains("at org.junit.platform.engine");

            } else {
                assertThat(accessor.getObject(new String[] {"_exception", "class"}))
                        .isNull();
                assertThat(accessor.getObject(new String[] {"_exception", "message"}))
                        .isNull();
                assertThat(accessor.getString(new String[] {"_exception", "stackTrace"}))
                        .isEmpty();
            }

            // Verify thread name.
            assertThat(accessor.getString("_thread")).isEqualTo(logEvent.getThreadName());

            // Verify logger name.
            assertThat(accessor.getString("_logger")).isEqualTo(logEvent.getLoggerName());
        });
    }

    private static String formatLogEventInstant(final LogEvent logEvent) {
        final org.apache.logging.log4j.core.time.Instant instant = logEvent.getInstant();
        final ZonedDateTime dateTime = Instant.ofEpochSecond(instant.getEpochSecond(), instant.getNanoOfSecond())
                .atZone(ZoneId.of("UTC"));
        return DATE_TIME_FORMATTER.format(dateTime);
    }
}
