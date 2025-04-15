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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.NoMarkerFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests {@link OutputStreamAppender}.
 */
class OutputStreamAppenderTest {

    private static final String TEST_MSG = "FOO ERROR";

    public String testName;

    private String getName(final OutputStream out) {
        return out.getClass().getSimpleName() + "." + testName;
    }

    /**
     * Tests that you can add an output stream appender dynamically.
     */
    private void addAppender(final OutputStream outputStream, final String outputStreamName) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        final PatternLayout layout = PatternLayout.createDefaultLayout(config);
        final Appender appender =
                OutputStreamAppender.createAppender(layout, null, outputStream, outputStreamName, false, true);
        appender.start();
        config.addAppender(appender);
        ConfigurationTestUtils.updateLoggers(appender, config);
    }

    @Test
    void testBuildFilter() {
        final NoMarkerFilter filter = NoMarkerFilter.newBuilder().build();
        // @formatter:off
        final OutputStreamAppender.Builder builder =
                OutputStreamAppender.newBuilder().setName("test").setFilter(filter);
        // @formatter:on
        assertEquals(filter, builder.getFilter());
        final OutputStreamAppender appender = builder.build();
        assertEquals(filter, appender.getFilter());
    }

    @Test
    void testOutputStreamAppenderToBufferedOutputStream() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStream os = new BufferedOutputStream(out);
        final String name = getName(out);
        final Logger logger = LogManager.getLogger(name);
        addAppender(os, name);
        logger.error(TEST_MSG);
        final String actual = out.toString();
        assertTrue(actual.contains(TEST_MSG), actual);
    }

    @Test
    void testOutputStreamAppenderToByteArrayOutputStream() {
        final OutputStream out = new ByteArrayOutputStream();
        final String name = getName(out);
        final Logger logger = LogManager.getLogger(name);
        addAppender(out, name);
        logger.error(TEST_MSG);
        final String actual = out.toString();
        assertTrue(actual.contains(TEST_MSG), actual);
    }

    /**
     * Validates that the code pattern we use to add an appender on the fly
     * works with a basic appender that is not the new OutputStream appender or
     * new Writer appender.
     */
    @Test
    void testUpdatePatternWithFileAppender() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        // @formatter:off
        final Appender appender = FileAppender.newBuilder()
                .withFileName("target/" + getClass().getName() + ".log")
                .withAppend(false)
                .setName("File")
                .setIgnoreExceptions(false)
                .withBufferedIo(false)
                .withBufferSize(4000)
                .setConfiguration(config)
                .build();
        // @formatter:on
        appender.start();
        config.addAppender(appender);
        ConfigurationTestUtils.updateLoggers(appender, config);
        LogManager.getLogger().error("FOO MSG");
    }

    @BeforeEach
    public void setup(TestInfo testInfo) {
        Optional<Method> testMethod = testInfo.getTestMethod();
        testMethod.ifPresent(method -> this.testName = method.getName());
    }
}
