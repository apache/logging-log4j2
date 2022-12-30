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
package org.apache.logging.log4j.core.impl;

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

import static org.junit.jupiter.api.Assertions.*;

public class ImmutableLogEventTest {

    @Test
    public void testToImmutableSame() {
        final LogEvent logEvent = LogEvent.builder().get();
        assertSame(logEvent, logEvent.toImmutable());
    }

    @Test
    public void testToImmutableNotSame() {
        final LogEvent logEvent = LogEvent.builder().setMessage(new ReusableObjectMessage()).get();
        final LogEvent immutable = logEvent.toImmutable();
        assertSame(logEvent, immutable);
        assertFalse(immutable.getMessage() instanceof ReusableMessage);
    }

    @Test
    public void testNullLevelReplacedWithOFF() {
        final LogEvent evt = LogEvent.builder().setLevel(null).get();
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testTimestampGeneratedByClock() {
        final LogEvent evt = LogEvent.builder().setClock(new FixedTimeClock()).get();
        assertEquals(FixedTimeClock.FIXED_TIME, evt.getTimeMillis());
    }

    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributes() {
        final ContextDataFactory contextDataFactory = new DefaultContextDataFactory();
        final StringMap contextData = contextDataFactory.createContextData();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final LogEvent event = LogEvent.builder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .endOfBatch(true) //
                .includeLocation(true) //
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
                .get();

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

        final LogEvent event2 = event.copy();
        assertEquals(event2, event, "copy constructor builder");
        assertNotSame(event2, event);
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
        final LogEvent event = LogEvent.builder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .endOfBatch(true) //
                .includeLocation(true) //
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
                .get();

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

        final LogEvent event2 = event.copy();
        assertEquals(event2, event, "copy constructor builder");
        assertNotSame(event2, event);
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");
    }

    @Test
    public void testBuilderCorrectlyCopiesMutableLogEvent() {
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
        //event.setSource(stackTraceElement); // cannot be explicitly set
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
        //assertSame(stackTraceElement, event.getSource()); // don't invoke
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent e2 = event.copy();
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
        //assertSame(stackTraceElement, e2.getSource()); // don't invoke
        assertSame(threadName, e2.getThreadName());
        assertSame(exception, e2.getThrown());
        assertEquals(987654321L, e2.getTimeMillis());
        final StackTraceElement value = assertInstanceOf(ImmutableLogEvent.class, e2).getSourceOrNull();
        assertNull(value, "source in copy");
    }

    @Test
    public void testEquals() {
        final ContextDataFactory contextDataFactory = new DefaultContextDataFactory();
        final StringMap contextData = contextDataFactory.createContextData();
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
        final LogEvent event = LogEvent.builder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .endOfBatch(true) //
                .includeLocation(true) //
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
                .get();

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

        final LogEvent event2 = builder(event).get();
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

        final StringMap differentMap = contextDataFactory.emptyFrozenContextData();
        different("different contextMap", builder(event).setContextData(differentMap), event);
        different("null contextMap", builder(event).setContextData((StringMap) null), event);

        ThreadContext.push("abc");
        final ContextStack contextStack2 = ThreadContext.getImmutableStack();
        different("different contextStack", builder(event).setContextStack(contextStack2), event);
        different("null contextStack", builder(event).setContextStack(null), event);

        different("different EndOfBatch", builder(event).endOfBatch(false), event);
        different("different IncludeLocation", builder(event).includeLocation(false), event);

        different("different level", builder(event).setLevel(Level.INFO), event);
        different("null level", builder(event).setLevel(null), event);

        different("different fqcn", builder(event).setLoggerFqcn("different"), event);
        different("null fqcn", builder(event).setLoggerFqcn(null), event);

        different("different name", builder(event).setLoggerName("different"), event);
        assertThrows(NullPointerException.class, () -> different("null name", builder(event).setLoggerName(null), event));

        different("different marker", builder(event).setMarker(MarkerManager.getMarker("different")), event);
        different("null marker", builder(event).setMarker(null), event);

        different("different message", builder(event).setMessage(new ObjectMessage("different")), event);
        assertThrows(NullPointerException.class, () -> different("null message", builder(event).setMessage(null), event));

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

    private static LogEventBuilder builder(final LogEvent event) {
        return LogEvent.builderFrom(event);
    }

    private void different(final String reason, final LogEventBuilder builder, final LogEvent event) {
        final LogEvent other = builder.get();
        assertNotEquals(other, event, reason);
        assertNotEquals(other.hashCode(), event.hashCode(), reason + " hashCode");
    }

    @Test
    public void testToString() {
        // Throws an NPE in 2.6.2
        assertNotNull(LogEvent.builder().get().toString());
    }
}
