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
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ObjectInputStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 */
class LogEventTest {

    private static final Message MESSAGE = new SimpleMessage("This is a test");
    private static final TestClass TESTER = new TestClass();

    @Test
    void testSerialization() throws Exception {
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        final Exception parent = new IllegalStateException("Test");
        final Throwable child = new LoggingException("This is a test", parent);
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .setThrown(child) //
                .build();

        final byte[] data = SerialUtil.serialize(event1, event2);

        try (final ObjectInputStream ois = SerialUtil.getObjectInputStream(data)) {
            assertDoesNotThrow(ois::readObject, "Failed to deserialize event1");
            assertDoesNotThrow(ois::readObject, "Failed to deserialize event1");
        }
    }

    @Test
    void testNanoTimeIsNotSerialized1() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .setThreadName("this must be initialized or the test fails") //
                .setNanoTime(12345678L) //
                .build();

        final LogEvent expected = new Log4jLogEvent.Builder(event).build();
        final LogEvent actual = SerialUtil.deserialize(SerialUtil.serialize(event));

        assertNotEquals(expected, actual, "Different event: nanoTime");
        assertNotEquals(expected.getNanoTime(), actual.getNanoTime(), "Different nanoTime");
        assertEquals(0, actual.getNanoTime(), "deserialized nanoTime is zero");
    }

    @Test
    void testNanoTimeIsNotSerialized2() {
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .setThreadId(1) // this must be initialized or the test fails
                .setThreadName("this must be initialized or the test fails") //
                .setThreadPriority(2) // this must be initialized or the test fails
                .setNanoTime(0) //
                .build();

        final LogEvent expected = new Log4jLogEvent.Builder(event).build();
        final LogEvent actual = SerialUtil.deserialize(SerialUtil.serialize(event));
        assertEquals(expected, actual, "both zero nanoTime");
    }

    @Test
    @Disabled
    void testEquals() {
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        final LogEvent event3 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .build();
        assertNotEquals(event1, event2, "Events should not be equal");
        assertEquals(event2, event3, "Events should be equal");
    }

    @Test
    void testLocation() {
        final StackTraceElement ste = TESTER.getEventSource(this.getClass().getName());
        assertNotNull(ste, "No StackTraceElement");
        assertEquals(this.getClass().getName(), ste.getClassName(), "Incorrect event");
    }

    private static class TestClass {
        private static final String FQCN = TestClass.class.getName();

        public StackTraceElement getEventSource(final String loggerName) {
            final LogEvent event = Log4jLogEvent.newBuilder()
                    .setLoggerName(loggerName)
                    .setLoggerFqcn(FQCN)
                    .setLevel(Level.INFO)
                    .setMessage(MESSAGE)
                    .build();
            event.setIncludeLocation(true);
            return event.getSource();
        }
    }
}
