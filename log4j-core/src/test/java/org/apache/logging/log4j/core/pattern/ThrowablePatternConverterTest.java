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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThrowablePatternConverterTest {

    private final static class LocalizedException extends Exception {

        private static final long serialVersionUID = 1L;

        @Override
        public String getLocalizedMessage() {
            return "I am localized.";
        }
    }

    /**
     * TODO: Needs better a better exception? NumberFormatException is NOT helpful.
     */
    @Test(expected = Exception.class)
    public void testBadShortOption() {
        final String[] options = { "short.UNKNOWN" };
        ThrowablePatternConverter.newInstance(options);
    }

    @Test
    public void testFull() {
        final String[] options = { "full" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
        Throwable parent;
        try {
            try {
                throw new NullPointerException("null pointer");
            } catch (final NullPointerException e) {
                throw new IllegalArgumentException("IllegalArgument", e);
            }
        } catch (final IllegalArgumentException e) {
            parent = e;
        }
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        // System.out.print(result);
        assertTrue("Incorrect start of msg", result.startsWith("java.lang.IllegalArgumentException: IllegalArgument"));
        assertTrue("Missing nested exception", result.contains("java.lang.NullPointerException: null pointer"));
    }

    @Test
    public void testShortClassName() {
        final String packageName = "org.apache.logging.log4j.core.pattern.";
        final String[] options = { "short.className" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
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
        assertEquals("The class names should be same", packageName + "ThrowablePatternConverterTest", result);
    }

    @Test
    public void testShortFileName() {
        final String[] options = { "short.fileName" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
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
        assertEquals("The file names should be same", "ThrowablePatternConverterTest.java", result);
    }

    @Test
    public void testShortLineNumber() {
        final String[] options = { "short.lineNumber" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final StackTraceElement top = parent.getStackTrace()[0];
        final int expectedLineNumber = top.getLineNumber();

        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue("The line numbers should be same", expectedLineNumber == Integer.parseInt(result));
    }

    @Test
    public void testShortLocalizedMessage() {
        final String[] options = { "short.localizedMessage" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
        final Throwable parent = new LocalizedException();
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent).build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertEquals("The messages should be same", "I am localized.", result);
    }

    @Test
    public void testShortMessage() {
        final String[] options = { "short.message" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
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
        assertEquals("The messages should be same", "IllegalArgument", result);
    }

    @Test
    public void testShortMethodName() {
        final String[] options = { "short.methodName" };
        final ThrowablePatternConverter converter = ThrowablePatternConverter.newInstance(options);
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
        assertEquals("The method names should be same", "testShortMethodName", result);
    }

}
