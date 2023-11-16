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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests the ReusableLogEventFactory class.
 */
public class ReusableLogEventFactoryTest {

    @Test
    public void testCreateEventReturnsDifferentInstanceIfNotReleased() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event1 = callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null);
        final LogEvent event2 = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
        assertNotSame(event1, event2);
        ReusableLogEventFactory.release(event1);
        ReusableLogEventFactory.release(event2);
    }

    @Test
    public void testCreateEventReturnsSameInstance() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event1 = callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null);
        ReusableLogEventFactory.release(event1);
        final LogEvent event2 = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
        assertSame(event1, event2);

        ReusableLogEventFactory.release(event2);
        final LogEvent event3 = callCreateEvent(factory, "c", Level.INFO, new SimpleMessage("123"), null);
        assertSame(event2, event3);
        ReusableLogEventFactory.release(event3);
    }

    @Test
    public void testCreateEventOverwritesFields() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event1 = callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null);
        assertEquals("a", event1.getLoggerName(), "logger");
        assertEquals(Level.DEBUG, event1.getLevel(), "level");
        assertEquals(new SimpleMessage("abc"), event1.getMessage(), "msg");

        ReusableLogEventFactory.release(event1);
        final LogEvent event2 = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
        assertSame(event1, event2);

        assertEquals("b", event1.getLoggerName(), "logger");
        assertEquals(Level.INFO, event1.getLevel(), "level");
        assertEquals(new SimpleMessage("xyz"), event1.getMessage(), "msg");
        assertEquals("b", event2.getLoggerName(), "logger");
        assertEquals(Level.INFO, event2.getLevel(), "level");
        assertEquals(new SimpleMessage("xyz"), event2.getMessage(), "msg");
    }

    private LogEvent callCreateEvent(
            final ReusableLogEventFactory factory,
            final String logger,
            final Level level,
            final Message message,
            final Throwable thrown) {
        return factory.createEvent(logger, null, getClass().getName(), level, message, null, thrown);
    }

    @Test
    public void testCreateEventReturnsThreadLocalInstance() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final AtomicReference<LogEvent> event1 = new AtomicReference<>();
        final AtomicReference<LogEvent> event2 = new AtomicReference<>();
        final Thread t1 = new Thread("THREAD 1") {
            @Override
            public void run() {
                event1.set(callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null));
            }
        };
        final Thread t2 = new Thread("Thread 2") {
            @Override
            public void run() {
                event2.set(callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null));
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertNotNull(event1.get());
        assertNotNull(event2.get());
        assertNotSame(event1.get(), event2.get());
        assertEquals("a", event1.get().getLoggerName(), "logger");
        assertEquals(Level.DEBUG, event1.get().getLevel(), "level");
        assertEquals(new SimpleMessage("abc"), event1.get().getMessage(), "msg");
        assertEquals("THREAD 1", event1.get().getThreadName(), "thread name");
        assertEquals(t1.getId(), event1.get().getThreadId(), "tid");

        assertEquals("b", event2.get().getLoggerName(), "logger");
        assertEquals(Level.INFO, event2.get().getLevel(), "level");
        assertEquals(new SimpleMessage("xyz"), event2.get().getMessage(), "msg");
        assertEquals("Thread 2", event2.get().getThreadName(), "thread name");
        assertEquals(t2.getId(), event2.get().getThreadId(), "tid");
        ReusableLogEventFactory.release(event1.get());
        ReusableLogEventFactory.release(event2.get());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateEventInitFieldsProperly() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event = callCreateEvent(factory, "logger", Level.INFO, new SimpleMessage("xyz"), null);
        try {
            assertNotNull(event.getContextMap());
            assertNotNull(event.getContextData());
            assertNotNull(event.getContextStack());
        } finally {
            ReusableLogEventFactory.release(event);
        }
    }
}
