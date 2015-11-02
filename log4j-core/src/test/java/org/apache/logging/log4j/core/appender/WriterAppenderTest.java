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

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
 * Tests {@link WriterAppender}.
 */
public class WriterAppenderTest {

    private static final String TEST_MSG = "FOO ERROR";

    @Rule
    public TestName testName = new TestName();

    private String getName(final Writer writer) {
        return writer.getClass().getSimpleName() + "." + testName.getMethodName();
    }

    private void test(final ByteArrayOutputStream out, final Writer writer) throws SQLException {
        final String name = getName(writer);
        addAppender(writer, name);
        final Logger logger = LogManager.getLogger(name);
        logger.error(TEST_MSG);
        final String actual = out.toString();
        Assert.assertTrue(actual, actual.contains(TEST_MSG));
    }

    private void test(final Writer writer) throws SQLException {
        final String name = getName(writer);
        addAppender(writer, name);
        final Logger logger = LogManager.getLogger(name);
        logger.error(TEST_MSG);
        final String actual = writer.toString();
        Assert.assertTrue(actual, actual.contains(TEST_MSG));
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
    public void testWriterAppenderToCharArrayWriter() throws SQLException {
        test(new CharArrayWriter());
    }

    @Test
    public void testWriterAppenderToOutputStreamWriter() throws SQLException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(out);
        test(out, writer);
    }

    @Test
    public void testWriterAppenderToPrintWriter() throws SQLException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new PrintWriter(out);
        test(out, writer);
    }

    @Test
    public void testWriterAppenderToStringWriter() throws SQLException {
        test(new StringWriter());
    }

}
