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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals("logger", "a", event1.getLoggerName());
        assertEquals("level", Level.DEBUG, event1.getLevel());
        assertEquals("msg", new SimpleMessage("abc"), event1.getMessage());

        ReusableLogEventFactory.release(event1);
        final LogEvent event2 = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
        assertSame(event1, event2);

        assertEquals("logger", "b", event1.getLoggerName());
        assertEquals("level", Level.INFO, event1.getLevel());
        assertEquals("msg", new SimpleMessage("xyz"), event1.getMessage());
        assertEquals("logger", "b", event2.getLoggerName());
        assertEquals("level", Level.INFO, event2.getLevel());
        assertEquals("msg", new SimpleMessage("xyz"), event2.getMessage());
    }

    private LogEvent callCreateEvent(final ReusableLogEventFactory factory, final String logger, final Level level,
            final Message message, final Throwable thrown) {
        return factory.createEvent(logger, null, getClass().getName(), level, message, null, thrown);
    }

    @Test
    public void testCreateEventReturnsThreadLocalInstance() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent[] event1 = new LogEvent[1];
        final LogEvent[] event2 = new LogEvent[1];
        final Thread t1 = new Thread("THREAD 1") {
            @Override
            public void run() {
                event1[0] = callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null);
            }
        };
        final Thread t2 = new Thread("Thread 2") {
            @Override
            public void run() {
                event2[0] = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertNotNull(event1[0]);
        assertNotNull(event2[0]);
        assertNotSame(event1[0], event2[0]);
        assertEquals("logger", "a", event1[0].getLoggerName());
        assertEquals("level", Level.DEBUG, event1[0].getLevel());
        assertEquals("msg", new SimpleMessage("abc"), event1[0].getMessage());
        assertEquals("thread name", "THREAD 1", event1[0].getThreadName());
        assertEquals("tid", t1.getId(), event1[0].getThreadId());

        assertEquals("logger", "b", event2[0].getLoggerName());
        assertEquals("level", Level.INFO, event2[0].getLevel());
        assertEquals("msg", new SimpleMessage("xyz"), event2[0].getMessage());
        assertEquals("thread name", "Thread 2", event2[0].getThreadName());
        assertEquals("tid", t2.getId(), event2[0].getThreadId());
        ReusableLogEventFactory.release(event1[0]);
        ReusableLogEventFactory.release(event2[0]);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateEventInitFieldsProperly() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event = callCreateEvent(factory, "logger", Level.INFO, new SimpleMessage("xyz"), null);
        ReusableLogEventFactory.release(event);
        assertNotNull(event.getContextMap());
        assertNotNull(event.getContextData());
        assertNotNull(event.getContextStack());
    }

}