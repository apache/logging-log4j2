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
package org.apache.logging.log4j.core.pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

/**
 *
 */
public class ExtendedThrowablePatternConverterTest {

    @Test
    public void testFull() {
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = new Log4jLogEvent("testLogger", null, this.getClass().getName(), Level.DEBUG,
            new SimpleMessage("test exception"), parent);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        parent.printStackTrace(pw);
        String result = sb.toString();
        result = result.replaceAll(" ~?\\[.*\\]", Strings.EMPTY);
        final String expected = sw.toString().replaceAll("\r", Strings.EMPTY);
        assertEquals(expected, result);
    }

    @Test
    public void testFiltering() {
        final String packages = "filters(org.junit, org.apache.maven, sun.reflect, java.lang.reflect)";
        final String[] options = {packages};
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = new Log4jLogEvent("testLogger", null, this.getClass().getName(), Level.DEBUG,
            new SimpleMessage("test exception"), parent);
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue("No suppressed lines", result.contains(" suppressed "));
    }
}
