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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;

/**
 *
 */
public class ExtendedThrowablePatternConverterTest {

    @Test
    public void testSuffixFromNormalPattern() {
        final String suffix = "suffix(%mdc{key})";
        ThreadContext.put("key", "test suffix ");
        final String[] options = {suffix};
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue("No suffix", result.contains("test suffix"));
    }

    @Test
    public void testSuffix() {
        final String suffix = "suffix(test suffix)";
        final String[] options = {suffix};
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue("No suffix", result.contains("test suffix"));
    }

    @Test
    public void testSuffixWillIgnoreThrowablePattern() {
        final String suffix = "suffix(%xEx{suffix(inner suffix)})";
        final String[] options = {suffix};
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertFalse("Has unexpected suffix", result.contains("inner suffix"));
    }

    @Test
	public void testDeserializedLogEventWithThrowableProxyButNoThrowable() {
		final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, null);
		final Throwable originalThrowable = new Exception("something bad happened");
		final ThrowableProxy throwableProxy = new ThrowableProxy(originalThrowable);
		final Throwable deserializedThrowable = null;
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("")) //
                .setThrown(deserializedThrowable) //
                .setThrownProxy(throwableProxy) //
                .setTimeMillis(0).build();
		final StringBuilder sb = new StringBuilder();
		converter.format(event, sb);
		final String result = sb.toString();
		assertTrue(result, result.contains(originalThrowable.getMessage()));
		assertTrue(result, result.contains(originalThrowable.getStackTrace()[0].getMethodName()));
	}

    @Test
    public void testFiltering() {
        final String packages = "filters(org.junit, org.apache.maven, sun.reflect, java.lang.reflect)";
        final String[] options = {packages};
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue("No suppressed lines", result.contains(" suppressed "));
    }

    @Test
    public void testFull() {
        final ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, null);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
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
}
