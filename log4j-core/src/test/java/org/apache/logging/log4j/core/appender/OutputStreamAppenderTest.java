/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Tests {@link OutputStreamAppender}.
 */
public class OutputStreamAppenderTest {

    private static final String TEST_MSG = "FOO ERROR";

    @Rule
    public TestName testName = new TestName();

    private String getName(final OutputStream out) {
        return out.getClass().getSimpleName() + "." + testName.getMethodName();
    }

    /**
     * Tests that you can add an output stream appender dynamically.
     */
    private void addAppender(final OutputStream outputStream, final String outputStreamName) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        final PatternLayout layout = PatternLayout.createDefaultLayout(config);
        final Appender appender = OutputStreamAppender.createAppender(layout, null, outputStream, outputStreamName, false, true);
        appender.start();
        config.addAppender(appender);
        ConfigurationTestUtils.updateLoggers(appender, config);
    }

    @Test
    public void testOutputStreamAppenderToBufferedOutputStream() throws SQLException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final OutputStream os = new BufferedOutputStream(out);
        final String name = getName(out);
        final Logger logger = LogManager.getLogger(name);
        addAppender(os, name);
        logger.error(TEST_MSG);
        final String actual = out.toString();
        Assert.assertTrue(actual, actual.contains(TEST_MSG));
    }

    @Test
    public void testOutputStreamAppenderToByteArrayOutputStream() throws SQLException {
        final OutputStream out = new ByteArrayOutputStream();
        final String name = getName(out);
        final Logger logger = LogManager.getLogger(name);
        addAppender(out, name);
        logger.error(TEST_MSG);
        final String actual = out.toString();
        Assert.assertTrue(actual, actual.contains(TEST_MSG));
    }

    /**
     * Validates that the code pattern we use to add an appender on the fly
     * works with a basic appender that is not the new OutputStream appender or
     * new Writer appender.
     */
    @Test
    public void testUpdatePatternWithFileAppender() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        // @formatter:off
        final Appender appender = FileAppender.newBuilder()
            .withFileName("target/" + getClass().getName() + ".log")
            .withAppend(false)
            .withName("File")
            .withIgnoreExceptions(false)
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
}
