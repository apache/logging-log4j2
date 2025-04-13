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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.internal.FixedPreciseClock;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.apache.logging.log4j.util.StringMap;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests the RingBufferLogEvent class.
 */
@Tag("async")
class RingBufferLogEventTest {

    @Test
    void testToImmutable() {
        final LogEvent logEvent = new RingBufferLogEvent();
        assertThat(logEvent).isNotSameAs(logEvent.toImmutable());
    }

    /**
     * Reproduces <a href="https://issues.apache.org/jira/browse/LOG4J2-2816">LOG4J2-2816</a>.
     */
    @Test
    void testIsPopulated() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        assertThat(evt.isPopulated()).isFalse();

        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        evt.setValues(
                null,
                loggerName,
                marker,
                fqcn,
                level,
                data,
                t,
                (StringMap) evt.getContextData(),
                contextStack,
                -1,
                threadName,
                -1,
                location,
                new FixedPreciseClock(),
                new DummyNanoClock(1));

        assertThat(evt.isPopulated()).isTrue();

        evt.clear();

        assertThat(evt.isPopulated()).isFalse();
    }

    @Test
    void testGetLevelReturnsOffIfNullLevelSet() {
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
        evt.setValues(
                null,
                loggerName,
                marker,
                fqcn,
                level,
                data,
                t,
                (StringMap) evt.getContextData(),
                contextStack,
                -1,
                threadName,
                -1,
                location,
                new FixedPreciseClock(),
                new DummyNanoClock(1));
        assertThat(evt.getLevel()).isEqualTo(Level.OFF);
    }

    @Test
    void testGetMessageReturnsNonNullMessage() {
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
        evt.setValues(
                null,
                loggerName,
                marker,
                fqcn,
                level,
                data,
                t,
                (StringMap) evt.getContextData(),
                contextStack,
                -1,
                threadName,
                -1,
                location,
                new FixedPreciseClock(),
                new DummyNanoClock(1));
        assertThat(evt.getMessage()).isNotNull();
    }

    @Test
    void testGetMillisReturnsConstructorMillisForNormalMessage() {
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
        evt.setValues(
                null,
                loggerName,
                marker,
                fqcn,
                level,
                data,
                t,
                (StringMap) evt.getContextData(),
                contextStack,
                -1,
                threadName,
                -1,
                location,
                new FixedPreciseClock(123, 456),
                new DummyNanoClock(1));
        assertThat(evt.getTimeMillis()).isEqualTo(123);
        assertThat(evt.getInstant().getNanoOfMillisecond()).isEqualTo(456);
    }

    @Test
    void testSerializationDeserialization() {
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
        evt.setValues(
                null,
                loggerName,
                marker,
                fqcn,
                level,
                data,
                t,
                (StringMap) evt.getContextData(),
                contextStack,
                -1,
                threadName,
                -1,
                location,
                new FixedPreciseClock(12345, 678),
                new DummyNanoClock(1));
        ((StringMap) evt.getContextData()).putValue("key", "value");

        final RingBufferLogEvent other = SerialUtil.deserialize(SerialUtil.serialize(evt));
        assertThat(other.getLoggerName()).isEqualTo(loggerName);
        assertThat(other.getMarker()).isEqualTo(marker);
        assertThat(other.getLoggerFqcn()).isEqualTo(fqcn);
        assertThat(other.getLevel()).isEqualTo(level);
        assertThat(other.getMessage()).isEqualTo(data);
        assertThat(other.getThrown()).isNull();
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
    void testCreateMementoReturnsCopy() {
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
        evt.setValues(
                null,
                loggerName,
                marker,
                fqcn,
                level,
                data,
                t,
                (StringMap) evt.getContextData(),
                contextStack,
                -1,
                threadName,
                -1,
                location,
                new FixedPreciseClock(12345, 678),
                new DummyNanoClock(1));
        ((StringMap) evt.getContextData()).putValue("key", "value");

        final LogEvent actual = evt.createMemento();
        assertThat(actual.getLoggerName()).isEqualTo(evt.getLoggerName());
        assertThat(actual.getMarker()).isEqualTo(evt.getMarker());
        assertThat(actual.getLoggerFqcn()).isEqualTo(evt.getLoggerFqcn());
        assertThat(actual.getLevel()).isEqualTo(evt.getLevel());
        assertThat(actual.getMessage()).isEqualTo(evt.getMessage());
        assertThat(actual.getThrown()).isEqualTo(evt.getThrown());
        assertThat(actual.getContextMap()).isEqualTo(evt.getContextMap());
        assertThat(actual.getContextData()).isEqualTo(evt.getContextData());
        assertThat(actual.getContextStack()).isEqualTo(evt.getContextStack());
        assertThat(actual.getThreadName()).isEqualTo(evt.getThreadName());
        assertThat(actual.getTimeMillis()).isEqualTo(evt.getTimeMillis());
        assertThat(actual.getInstant().getNanoOfMillisecond())
                .isEqualTo(evt.getInstant().getNanoOfMillisecond());
        assertThat(actual.getSource()).isEqualTo(evt.getSource());
        assertThat(actual.getThrownProxy()).isEqualTo(evt.getThrownProxy());
    }

    @Test
    void testCreateMementoRetainsParametersAndFormat() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        // Initialize the event with parameters
        evt.swapParameters(new Object[10]);
        final String loggerName = "logger.name";
        final Marker marker = MarkerManager.getMarker("marked man");
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        final ReusableMessageFactory factory = new ReusableMessageFactory();
        final Message message = factory.newMessage("Hello {}!", "World");
        try {
            final Throwable t = new InternalError("not a real error");
            final ContextStack contextStack = new MutableThreadContextStack(Arrays.asList("a", "b"));
            final String threadName = "main";
            final StackTraceElement location = null;
            evt.setValues(
                    null,
                    loggerName,
                    marker,
                    fqcn,
                    level,
                    message,
                    t,
                    (StringMap) evt.getContextData(),
                    contextStack,
                    -1,
                    threadName,
                    -1,
                    location,
                    new FixedPreciseClock(12345, 678),
                    new DummyNanoClock(1));
            ((StringMap) evt.getContextData()).putValue("key", "value");

            final Message actual = evt.createMemento().getMessage();
            assertThat(actual.getFormat()).isEqualTo("Hello {}!");
            assertThat(actual.getParameters()).isEqualTo(new String[] {"World"});
            assertThat(actual.getFormattedMessage()).isEqualTo("Hello World!");
        } finally {
            ReusableMessageFactory.release(message);
        }
    }

    @Test
    void testMementoReuse() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        // Initialize the event with parameters
        evt.swapParameters(new Object[10]);
        final String loggerName = "logger.name";
        final Marker marker = MarkerManager.getMarker("marked man");
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        final ReusableMessageFactory factory = new ReusableMessageFactory();
        final Message message = factory.newMessage("Hello {}!", "World");
        try {
            final Throwable t = new InternalError("not a real error");
            final ContextStack contextStack = new MutableThreadContextStack(Arrays.asList("a", "b"));
            final String threadName = "main";
            final StackTraceElement location = null;
            evt.setValues(
                    null,
                    loggerName,
                    marker,
                    fqcn,
                    level,
                    message,
                    t,
                    (StringMap) evt.getContextData(),
                    contextStack,
                    -1,
                    threadName,
                    -1,
                    location,
                    new FixedPreciseClock(12345, 678),
                    new DummyNanoClock(1));
            ((StringMap) evt.getContextData()).putValue("key", "value");

            final Message memento1 = evt.memento();
            final Message memento2 = evt.memento();
            assertThat(memento1).isSameAs(memento2);
        } finally {
            ReusableMessageFactory.release(message);
        }
    }

    @Test
    void testMessageTextNeverThrowsNpe() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        assertThatCode(evt::getFormattedMessage).doesNotThrowAnyException();
    }

    @Test
    void testForEachParameterNothingSet() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        assertThatCode(() -> evt.forEachParameter(
                        (parameter, parameterIndex, state) -> fail("Should not have been called"), null))
                .doesNotThrowAnyException();
    }

    /**
     * Reproduces <a href="https://github.com/apache/logging-log4j2/issues/2234">#2234</a>.
     */
    @Test
    void testGettersAndClear() {

        // Create mock fields
        final long salt = (long) (Math.random() * 1_000L);
        final AsyncLogger asyncLogger = mock(AsyncLogger.class);
        final String loggerName = "LoggerName-" + salt;
        final Marker marker = MarkerManager.getMarker("marker-" + salt);
        final String fqcn = "a.b.c_" + salt;
        final Level level = Level.TRACE;
        final Message message = mock(Message.class);
        final Throwable throwable = mock(Throwable.class);
        final StringMap contextData = mock(StringMap.class);
        final ContextStack contextStack = mock(ContextStack.class);
        final String threadName = "threadName-" + salt;
        final StackTraceElement location = new RuntimeException().getStackTrace()[0];

        // Create the log event
        final Clock clock = mock(Clock.class);
        final NanoClock nanoClock = mock(NanoClock.class);
        final RingBufferLogEvent event = new RingBufferLogEvent();
        event.setValues(
                asyncLogger,
                loggerName,
                marker,
                fqcn,
                level,
                message,
                throwable,
                contextData,
                contextStack,
                -1,
                threadName,
                -1,
                location,
                clock,
                nanoClock);

        // Verify getters
        assertThat(event.getLoggerName()).isSameAs(loggerName);
        assertThat(event.getMarker()).isSameAs(marker);
        assertThat(event.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(event.getLevel()).isSameAs(level);
        assertThat(event.getMessage()).isSameAs(message);
        assertThat(event.getThrowable()).isSameAs(throwable);
        assertThat(event.getContextData()).isSameAs(contextData);
        assertThat(event.getContextStack()).isSameAs(contextStack);
        assertThat(event.getThreadName()).isSameAs(threadName);
        assertThat(event.getSource()).isSameAs(location);

        // Verify clear
        event.clear();
        assertThat(event.getLoggerName()).isNull();
        assertThat(event.getMarker()).isNull();
        assertThat(event.getLoggerFqcn()).isNull();
        assertThat(event.getLevel()).isEqualTo(Level.OFF);
        verify(message).getFormattedMessage();
        assertThat(event.getMessage())
                .isNotSameAs(message)
                .extracting(Message::getFormattedMessage, as(InstanceOfAssertFactories.STRING))
                .isEmpty();
        assertThat(event.getThrowable()).isNull();
        verify(contextData).isFrozen();
        verify(contextData).clear();
        assertThat(event.getContextData()).isSameAs(contextData);
        assertThat(event.getContextStack()).isNull();
        assertThat(event.getThreadName()).isNull();
        assertThat(event.getSource()).isNull();

        // Verify interaction exhaustion
        verifyNoMoreInteractions(asyncLogger, message, throwable, contextData, contextStack);
    }
}
