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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests {@link WriterAppender}.
 */
class WriterAppenderTest {

    private static final String TEST_MSG = "FOO ERROR";

    private String testMethodName;

    @BeforeEach
    void setUp(final TestInfo testInfo) {
        testMethodName = testInfo.getTestMethod().map(Method::getName).orElseGet(testInfo::getDisplayName);
    }

    private String getName(final Writer writer) {
        return writer.getClass().getSimpleName() + "." + testMethodName;
    }

    private void test(final ByteArrayOutputStream out, final Writer writer) {
        final String name = getName(writer);
        addAppender(writer, name);
        final Logger logger = LogManager.getLogger(name);
        logger.error(TEST_MSG);
        final String actual = out.toString();
        assertThat(actual, containsString(TEST_MSG));
    }

    private void test(final Writer writer) {
        final String name = getName(writer);
        addAppender(writer, name);
        final Logger logger = LogManager.getLogger(name);
        logger.error(TEST_MSG);
        final String actual = writer.toString();
        assertThat(actual, containsString(TEST_MSG));
    }

    private void addAppender(final Writer writer, final String writerName) {
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        final PatternLayout layout = PatternLayout.createDefaultLayout(config);
        final Appender appender = WriterAppender.createAppender(layout, null, writer, writerName, false, true);
        appender.start();
        config.addAppender(appender);
        ConfigurationTestUtils.updateLoggers(appender, config);
    }

    @Test
    void testWriterAppenderToCharArrayWriter() {
        test(new CharArrayWriter());
    }

    @Test
    void testWriterAppenderToOutputStreamWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(out);
        test(out, writer);
    }

    @Test
    void testWriterAppenderToPrintWriter() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new PrintWriter(out);
        test(out, writer);
    }

    @Test
    void testWriterAppenderToStringWriter() {
        test(new StringWriter());
    }

    @Test
    void testBuilder() {
        // This should compile
        WriterAppender.newBuilder()
                .setTarget(new StringWriter())
                .setName("testWriterAppender")
                .build();
    }
}
