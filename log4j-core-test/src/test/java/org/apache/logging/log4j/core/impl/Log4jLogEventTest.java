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
package org.apache.logging.log4j.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Base64;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.util.FixedTimeClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.Test;

public class Log4jLogEventTest {

    private static final Base64.Decoder decoder = Base64.getDecoder();

    @Test
    public void testToImmutableSame() {
        final LogEvent logEvent = Log4jLogEvent.newBuilder().build();
        assertSame(logEvent, logEvent.toImmutable());
    }

    @Test
    public void testToImmutableNotSame() {
        final LogEvent logEvent = new Log4jLogEvent.Builder()
                .setMessage(new ReusableObjectMessage())
                .build();
        final LogEvent immutable = logEvent.toImmutable();
        assertSame(logEvent, immutable);
        assertFalse(immutable.getMessage() instanceof ReusableMessage);
    }

    @Test
    public void testNullLevelReplacedWithOFF() throws Exception {
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt =
                Log4jLogEvent.newBuilder().setLevel(NULL_LEVEL).build();
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testTimestampGeneratedByClock() {
        final LogEvent evt =
                Log4jLogEvent.newBuilder().setClock(new FixedTimeClock()).build();
        assertEquals(FixedTimeClock.FIXED_TIME, evt.getTimeMillis());
    }

    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributes() {
        final StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn(fqcn) //
                .setLoggerName(name) //
                .setMarker(marker) //
                .setMessage(message) //
                .setNanoTime(1234567890L) //
                .setSource(stackTraceElement) //
                .setThreadName(threadName) //
                .setThrown(exception) //
                .setTimeMillis(987654321L)
                .build();

        assertEquals(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        assertSame(stackTraceElement, event.getSource());
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(event2, event, "copy constructor builder");
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");
    }

    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributesInclContextData() {
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn(fqcn) //
                .setLoggerName(name) //
                .setMarker(marker) //
                .setMessage(message) //
                .setNanoTime(1234567890L) //
                .setSource(stackTraceElement) //
                .setThreadName(threadName) //
                .setThrown(exception) //
                .setTimeMillis(987654321L)
                .build();

        assertSame(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        assertSame(stackTraceElement, event.getSource());
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(event2, event, "copy constructor builder");
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");
    }

    @Test
    public void testBuilderCorrectlyCopiesMutableLogEvent() throws Exception {
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final MutableLogEvent event = new MutableLogEvent();
        event.setContextData(contextData);
        event.setContextStack(contextStack);
        event.setEndOfBatch(true);
        event.setIncludeLocation(true);
        // event.setSource(stackTraceElement); // cannot be explicitly set
        event.setLevel(Level.FATAL);
        event.setLoggerFqcn(fqcn);
        event.setLoggerName(name);
        event.setMarker(marker);
        event.setMessage(message);
        event.setNanoTime(1234567890L);
        event.setThreadName(threadName);
        event.setThrown(exception);
        event.setTimeMillis(987654321L);

        assertSame(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        // assertSame(stackTraceElement, event.getSource()); // don't invoke
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent e2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(contextData, e2.getContextData());
        assertSame(contextStack, e2.getContextStack());
        assertTrue(e2.isEndOfBatch());
        assertTrue(e2.isIncludeLocation());
        assertSame(Level.FATAL, e2.getLevel());
        assertSame(fqcn, e2.getLoggerFqcn());
        assertSame(name, e2.getLoggerName());
        assertSame(marker, e2.getMarker());
        assertSame(message, e2.getMessage());
        assertEquals(1234567890L, e2.getNanoTime());
        // assertSame(stackTraceElement, e2.getSource()); // don't invoke
        assertSame(threadName, e2.getThreadName());
        assertSame(exception, e2.getThrown());
        assertEquals(987654321L, e2.getTimeMillis());

        // use reflection to get value of source field in log event copy:
        // invoking the getSource() method would initialize the field
        final Field fieldSource = Log4jLogEvent.class.getDeclaredField("source");
        fieldSource.setAccessible(true);
        final Object value = fieldSource.get(e2);
        assertNull(value, "source in copy");
    }

    @Test
    public void testEquals() {
        final StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("A", "B");
        ThreadContext.push("first");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn(fqcn) //
                .setLoggerName(name) //
                .setMarker(marker) //
                .setMessage(message) //
                .setNanoTime(1234567890L) //
                .setSource(stackTraceElement) //
                .setThreadName(threadName) //
                .setThrown(exception) //
                .setTimeMillis(987654321L)
                .build();

        assertEquals(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        assertSame(stackTraceElement, event.getSource());
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent event2 = builder(event).build();
        assertEquals(event2, event, "copy constructor builder");
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");

        assertEquals(contextData, event2.getContextData());
        assertSame(contextStack, event2.getContextStack());
        assertTrue(event2.isEndOfBatch());
        assertTrue(event2.isIncludeLocation());
        assertSame(Level.FATAL, event2.getLevel());
        assertSame(fqcn, event2.getLoggerFqcn());
        assertSame(name, event2.getLoggerName());
        assertSame(marker, event2.getMarker());
        assertSame(message, event2.getMessage());
        assertEquals(1234567890L, event2.getNanoTime());
        assertSame(stackTraceElement, event2.getSource());
        assertSame(threadName, event2.getThreadName());
        assertSame(exception, event2.getThrown());
        assertEquals(987654321L, event2.getTimeMillis());

        final StringMap differentMap = ContextDataFactory.emptyFrozenContextData();
        different("different contextMap", builder(event).setContextData(differentMap), event);
        different("null contextMap", builder(event).setContextData(null), event);

        ThreadContext.push("abc");
        final ContextStack contextStack2 = ThreadContext.getImmutableStack();
        different("different contextStack", builder(event).setContextStack(contextStack2), event);
        different("null contextStack", builder(event).setContextStack(null), event);

        different("different EndOfBatch", builder(event).setEndOfBatch(false), event);
        different("different IncludeLocation", builder(event).setIncludeLocation(false), event);

        different("different level", builder(event).setLevel(Level.INFO), event);
        different("null level", builder(event).setLevel(null), event);

        different("different fqcn", builder(event).setLoggerFqcn("different"), event);
        different("null fqcn", builder(event).setLoggerFqcn(null), event);

        different("different name", builder(event).setLoggerName("different"), event);
        assertThrows(
                NullPointerException.class,
                () -> different("null name", builder(event).setLoggerName(null), event));

        different("different marker", builder(event).setMarker(MarkerManager.getMarker("different")), event);
        different("null marker", builder(event).setMarker(null), event);

        different("different message", builder(event).setMessage(new ObjectMessage("different")), event);
        assertThrows(
                NullPointerException.class,
                () -> different("null message", builder(event).setMessage(null), event));

        different("different nanoTime", builder(event).setNanoTime(135), event);
        different("different milliTime", builder(event).setTimeMillis(137), event);

        final StackTraceElement stack2 = new StackTraceElement("XXX", "YYY", "file", 123);
        different("different source", builder(event).setSource(stack2), event);
        different("null source", builder(event).setSource(null), event);

        different("different threadname", builder(event).setThreadName("different"), event);
        different("null threadname", builder(event).setThreadName(null), event);

        different("different exception", builder(event).setThrown(new Error("Boo!")), event);
        different("null exception", builder(event).setThrown(null), event);
    }

    private static Log4jLogEvent.Builder builder(final LogEvent event) {
        return new Log4jLogEvent.Builder(event);
    }

    private void different(final String reason, final Log4jLogEvent.Builder builder, final LogEvent event) {
        final LogEvent other = builder.build();
        assertNotEquals(other, event, reason);
        assertNotEquals(other.hashCode(), event.hashCode(), reason + " hashCode");
    }

    @Test
    public void testToString() {
        // Throws an NPE in 2.6.2
        assertNotNull(Log4jLogEvent.newBuilder().build().toString());
    }
}
