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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

public class RootThrowablePatternConverterTest {

    @Test
    public void testSuffix() {
        final String suffix = "suffix(test suffix)";
        final String[] options = {suffix};
        final RootThrowablePatternConverter converter = RootThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue(result.contains("test suffix"), "No suffix");
    }

    @Test
    public void testSuffixFromNormalPattern() {
        final String suffix = "suffix(%mdc{key})";
        ThreadContext.put("key", "test suffix ");
        final String[] options = {suffix};
        final RootThrowablePatternConverter converter = RootThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertTrue(result.contains("test suffix"), "No suffix");
    }

    @Test
    public void testSuffixWillIgnoreThrowablePattern() {
        final String suffix = "suffix(%xEx{suffix(inner suffix)})";
        final String[] options = {suffix};
        final RootThrowablePatternConverter converter = RootThrowablePatternConverter.newInstance(null, options);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        assertFalse(result.contains("inner suffix"), "Has unexpected suffix");
    }

    @Test
    public void testFull1() {
        final RootThrowablePatternConverter converter = RootThrowablePatternConverter.newInstance(null, null);
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("testLogger") //
                .setLoggerFqcn(this.getClass().getName()) //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("test exception")) //
                .setThrown(parent)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        // System.out.print(result);
        assertTrue(
                result.contains("Wrapped by: java.lang.IllegalArgumentException: IllegalArgument"),
                "Missing Exception");
        assertTrue(result.startsWith("java.lang.NullPointerException: null pointer"), "Incorrect start of msg");
    }

    /**
     * Sanity check for testFull1() above, makes sure that the way testFull1 is written matches actually throwing
     * exceptions.
     */
    @Test
    public void testFull2() {
        final RootThrowablePatternConverter converter = RootThrowablePatternConverter.newInstance(null, null);
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
                .setThrown(parent)
                .build();
        final StringBuilder sb = new StringBuilder();
        converter.format(event, sb);
        final String result = sb.toString();
        // System.out.print(result);
        assertTrue(
                result.contains("Wrapped by: java.lang.IllegalArgumentException: IllegalArgument"),
                "Missing Exception");
        assertTrue(result.startsWith("java.lang.NullPointerException: null pointer"), "Incorrect start of msg");
    }
}
