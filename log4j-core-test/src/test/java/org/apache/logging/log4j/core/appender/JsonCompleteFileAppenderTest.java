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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEventTest;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.selector.CoreContextSelectors;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests a "complete" JSON file.
 */
@Tag("Layouts.Json")
public class JsonCompleteFileAppenderTest {

    private final File logFile = new File("target", "JsonCompleteFileAppenderTest.log");

    @RegisterExtension
    CleanFiles cleanFiles = new CleanFiles(logFile);

    @BeforeAll
    public static void beforeAll() {
        System.setProperty(ClockFactory.PROPERTY_NAME, Log4jLogEventTest.FixedTimeClock.class.getName());
    }

    @AfterAll
    public static void afterAll() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
    }

    @MethodSource
    public static Stream<Class<?>> getParameters() {
        return Stream.of(CoreContextSelectors.CLASSES);
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    @LoggerContextSource("JsonCompleteFileAppenderTest.xml")
    public void testFlushAtEndOfBatch(final Class<ContextSelector> contextSelector, final LoggerContext loggerContext)
            throws Exception {
        final Logger logger = loggerContext.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=true";
        logger.info(logMsg);
        logger.error(logMsg, new IllegalArgumentException("badarg"));
        loggerContext.getConfiguration().getLoggerContext().stop(); // stops async thread

        final List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);

        final String[] expected = {
            "[", // equals
            "{", // equals
            "  \"instant\" : {", //
            "    \"epochSecond\" : 1234567,", //
            "    \"nanoOfSecond\" : 890000000", //
            "  },", //
            "  \"thread\" : \"main\",", //
            "  \"level\" : \"INFO\",", //
            "  \"loggerName\" : \"com.foo.Bar\",", //
            "  \"message\" : \"Message flushed with immediate flush=true\",", //
            "  \"endOfBatch\" : false,", //
            "  \"loggerFqcn\" : \"org.apache.logging.log4j.spi.AbstractLogger\",", //
        };
        for (int i = 0; i < expected.length; i++) {
            final String line = lines.get(i);
            assertTrue(
                    line.contains(expected[i]),
                    "line " + i + " incorrect: [" + line + "], does not contain: [" + expected[i] + ']');
        }
        final String location = "testFlushAtEndOfBatch";
        assertFalse(lines.get(0).contains(location), "no location");
    }
}
