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
package org.apache.logging.log4j.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class LogEventTest {

    private static Message MESSAGE = new SimpleMessage("This is a test");
    private static TestClass TESTER = new TestClass();

    @Test
    public void testSerialization() throws Exception {
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

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(event1);
        oos.writeObject(event2);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new FilteredObjectInputStream(bais);
        try {
            ois.readObject();
        } catch (final IOException ioe) {
            fail("Exception processing event1");
        }
        try {
            ois.readObject();
        } catch (final IOException ioe) {
            fail("Exception processing event2");
        }
    }

    @Test
    public void testNanoTimeIsNotSerialized1() throws Exception {
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .setThreadName("this must be initialized or the test fails") //
                .setNanoTime(12345678L) //
                .build();
        final LogEvent copy = new Log4jLogEvent.Builder(event1).build();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(event1);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new FilteredObjectInputStream(bais);

        final LogEvent actual = (LogEvent) ois.readObject();
        assertNotEquals(copy, actual, "Different event: nanoTime");
        assertNotEquals(copy.getNanoTime(), actual.getNanoTime(), "Different nanoTime");
        assertEquals(0, actual.getNanoTime(), "deserialized nanoTime is zero");
    }

    @Test
    public void testNanoTimeIsNotSerialized2() throws Exception {
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName()) //
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world!")) //
                .setThreadId(1) // this must be initialized or the test fails
                .setThreadName("this must be initialized or the test fails") //
                .setThreadPriority(2) // this must be initialized or the test fails
                .setNanoTime(0) //
                .build();
        final LogEvent event2 = new Log4jLogEvent.Builder(event1).build();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(event1);

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new FilteredObjectInputStream(bais);

        final LogEvent actual = (LogEvent) ois.readObject();
        assertEquals(event2, actual, "both zero nanoTime");
    }

    @Test
    @Disabled
    public void testEquals() {
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
    public void testLocation() {
        final StackTraceElement ste = TESTER.getEventSource(this.getClass().getName());
        assertNotNull(ste, "No StackTraceElement");
        assertEquals(this.getClass().getName(), ste.getClassName(), "Incorrect event");
    }

    private static class TestClass {
        private static final String FQCN = TestClass.class.getName();

        public StackTraceElement getEventSource(final String loggerName) {
            final LogEvent event = Log4jLogEvent.newBuilder().setLoggerName(loggerName)
                    .setLoggerFqcn(FQCN).setLevel(Level.INFO).setMessage(MESSAGE).build();
            event.setIncludeLocation(true);
            return event.getSource();
        }
    }
}
