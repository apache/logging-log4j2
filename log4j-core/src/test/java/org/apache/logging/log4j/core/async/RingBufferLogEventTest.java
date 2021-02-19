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

package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.time.internal.FixedPreciseClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterConsumer;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.apache.logging.log4j.util.StringMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests the RingBufferLogEvent class.
 */
@Category(AsyncLoggers.class)
public class RingBufferLogEventTest {

    @Test
    public void testToImmutable() {
        final LogEvent logEvent = new RingBufferLogEvent();
        assertThat(logEvent.toImmutable()).isNotSameAs(logEvent);
    }
    
    @Test
    public void testGetLevelReturnsOffIfNullLevelSet() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location, new FixedPreciseClock(), new DummyNanoClock(1));
        assertThat(evt.getLevel()).isEqualTo(Level.OFF);
    }

    @Test
    public void testGetMessageReturnsNonNullMessage() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location, new FixedPreciseClock(), new DummyNanoClock(1));
        assertThat(evt.getMessage()).isNotNull();
    }

    @Test
    public void testGetMillisReturnsConstructorMillisForNormalMessage() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location, new FixedPreciseClock(123, 456), new DummyNanoClock(1));
        assertThat(evt.getTimeMillis()).isEqualTo(123);
        assertThat(evt.getInstant().getNanoOfMillisecond()).isEqualTo(456);
    }

    @Test
    public void testSerializationDeserialization() throws IOException, ClassNotFoundException {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = "logger.name";
        final Marker marker = null;
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        final Message data = new SimpleMessage("message");
        final Throwable t = new InternalError("not a real error");
        final ContextStack contextStack = null;
        final String threadName = "main";
        final StackTraceElement location = null;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location,
                new FixedPreciseClock(12345, 678), new DummyNanoClock(1));
        ((StringMap) evt.getContextData()).putValue("key", "value");

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(evt);

        final ObjectInputStream in = new FilteredObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final RingBufferLogEvent other = (RingBufferLogEvent) in.readObject();
        assertThat(other.getLoggerName()).isEqualTo(loggerName);
        assertThat(other.getMarker()).isEqualTo(marker);
        assertThat(other.getLoggerFqcn()).isEqualTo(fqcn);
        assertThat(other.getLevel()).isEqualTo(level);
        assertThat(other.getMessage()).isEqualTo(data);
        assertThat(other.getThrown()).describedAs("null after serialization").isNull();
        assertThat(other.getThrownProxy()).isEqualTo(new ThrowableProxy(t));
        assertThat(other.getContextData()).isEqualTo(evt.getContextData());
        assertThat(other.getContextStack()).isEqualTo(contextStack);
        assertThat(other.getThreadName()).isEqualTo(threadName);
        assertThat(other.getSource()).isEqualTo(location);
        assertThat(other.getTimeMillis()).isEqualTo(12345);
        assertThat(other.getInstant().getNanoOfMillisecond()).isEqualTo(678);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateMementoReturnsCopy() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = "logger.name";
        final Marker marker = MarkerManager.getMarker("marked man");
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        final Message data = new SimpleMessage("message");
        final Throwable t = new InternalError("not a real error");
        final ContextStack contextStack = new MutableThreadContextStack(Arrays.asList("a", "b"));
        final String threadName = "main";
        final StackTraceElement location = null;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, (StringMap) evt.getContextData(),
                contextStack, -1, threadName, -1, location, new FixedPreciseClock(12345, 678), new DummyNanoClock(1));
        ((StringMap) evt.getContextData()).putValue("key", "value");

        final LogEvent actual = evt.createMemento();
        assertThat(actual.getLoggerName()).isEqualTo(evt.getLoggerName());
        assertThat(actual.getMarker()).isEqualTo(evt.getMarker());
        assertThat(actual.getLoggerFqcn()).isEqualTo(evt.getLoggerFqcn());
        assertThat(actual.getLevel()).isEqualTo(evt.getLevel());
        assertThat(actual.getMessage()).isEqualTo(evt.getMessage());
        assertThat(actual.getThrown()).isEqualTo(evt.getThrown());
        assertThat(actual.getContextData()).isEqualTo(evt.getContextData());
        assertThat(actual.getContextStack()).isEqualTo(evt.getContextStack());
        assertThat(actual.getThreadName()).isEqualTo(evt.getThreadName());
        assertThat(actual.getTimeMillis()).isEqualTo(evt.getTimeMillis());
        assertThat(actual.getInstant().getNanoOfMillisecond()).isEqualTo(evt.getInstant().getNanoOfMillisecond());
        assertThat(actual.getSource()).isEqualTo(evt.getSource());
        assertThat(actual.getThrownProxy()).isEqualTo(evt.getThrownProxy());
    }

    @Test
    public void testCreateMementoRetainsParametersAndFormat() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        // Initialize the event with parameters
        evt.swapParameters(new Object[10]);
        final String loggerName = "logger.name";
        final Marker marker = MarkerManager.getMarker("marked man");
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        ReusableMessageFactory factory = new ReusableMessageFactory();
        Message message = factory.newMessage("Hello {}!", "World");
        try {
            final Throwable t = new InternalError("not a real error");
            final ContextStack contextStack = new MutableThreadContextStack(Arrays.asList("a", "b"));
            final String threadName = "main";
            final StackTraceElement location = null;
            evt.setValues(null, loggerName, marker, fqcn, level, message, t, (StringMap) evt.getContextData(),
                    contextStack, -1, threadName, -1, location, new FixedPreciseClock(12345, 678), new DummyNanoClock(1));
            ((StringMap) evt.getContextData()).putValue("key", "value");

            final Message actual = evt.createMemento().getMessage();
            assertThat(actual.getFormat()).isEqualTo("Hello {}!");
            assertThat(actual.getParameters()).isEqualTo(new String[]{"World"});
            assertThat(actual.getFormattedMessage()).isEqualTo("Hello World!");
        } finally {
            ReusableMessageFactory.release(message);
        }
    }

    @Test
    public void testMementoReuse() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        // Initialize the event with parameters
        evt.swapParameters(new Object[10]);
        final String loggerName = "logger.name";
        final Marker marker = MarkerManager.getMarker("marked man");
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        ReusableMessageFactory factory = new ReusableMessageFactory();
        Message message = factory.newMessage("Hello {}!", "World");
        try {
            final Throwable t = new InternalError("not a real error");
            final ContextStack contextStack = new MutableThreadContextStack(Arrays.asList("a", "b"));
            final String threadName = "main";
            final StackTraceElement location = null;
            evt.setValues(null, loggerName, marker, fqcn, level, message, t, (StringMap) evt.getContextData(),
                    contextStack, -1, threadName, -1, location, new FixedPreciseClock(12345, 678), new DummyNanoClock(1));
            ((StringMap) evt.getContextData()).putValue("key", "value");

            final Message memento1 = evt.memento();
            final Message memento2 = evt.memento();
            assertThat(memento1).isSameAs(memento2);
        } finally {
            ReusableMessageFactory.release(message);
        }
    }

    @Test
    public void testMessageTextNeverThrowsNpe() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        try {
            evt.getFormattedMessage();
        } catch (final NullPointerException e) {
            fail("the messageText field was not set");
        }
    }

    @Test
    public void testForEachParameterNothingSet() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        evt.forEachParameter(new ParameterConsumer<Void>() {
            @Override
            public void accept(Object parameter, int parameterIndex, Void state) {
                fail("Should not have been called");
            }
        }, null);
    }
}
