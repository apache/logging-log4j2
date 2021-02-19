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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
        assertThat(event2).isNotSameAs(event1);
        ReusableLogEventFactory.release(event1);
        ReusableLogEventFactory.release(event2);
    }

    @Test
    public void testCreateEventReturnsSameInstance() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event1 = callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null);
        ReusableLogEventFactory.release(event1);
        final LogEvent event2 = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
        assertThat(event2).isSameAs(event1);

        ReusableLogEventFactory.release(event2);
        final LogEvent event3 = callCreateEvent(factory, "c", Level.INFO, new SimpleMessage("123"), null);
        assertThat(event3).isSameAs(event2);
        ReusableLogEventFactory.release(event3);
    }

    @Test
    public void testCreateEventOverwritesFields() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event1 = callCreateEvent(factory, "a", Level.DEBUG, new SimpleMessage("abc"), null);
        assertThat(event1.getLoggerName()).describedAs("logger").isEqualTo("a");
        assertThat(event1.getLevel()).describedAs("level").isEqualTo(Level.DEBUG);
        assertThat(event1.getMessage()).describedAs("msg").isEqualTo(new SimpleMessage("abc"));

        ReusableLogEventFactory.release(event1);
        final LogEvent event2 = callCreateEvent(factory, "b", Level.INFO, new SimpleMessage("xyz"), null);
        assertThat(event2).isSameAs(event1);

        assertThat(event1.getLoggerName()).describedAs("logger").isEqualTo("b");
        assertThat(event1.getLevel()).describedAs("level").isEqualTo(Level.INFO);
        assertThat(event1.getMessage()).describedAs("msg").isEqualTo(new SimpleMessage("xyz"));
        assertThat(event2.getLoggerName()).describedAs("logger").isEqualTo("b");
        assertThat(event2.getLevel()).describedAs("level").isEqualTo(Level.INFO);
        assertThat(event2.getMessage()).describedAs("msg").isEqualTo(new SimpleMessage("xyz"));
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
        assertThat(event1[0]).isNotNull();
        assertThat(event2[0]).isNotNull();
        assertThat(event2[0]).isNotSameAs(event1[0]);
        assertThat(event1[0].getLoggerName()).describedAs("logger").isEqualTo("a");
        assertThat(event1[0].getLevel()).describedAs("level").isEqualTo(Level.DEBUG);
        assertThat(event1[0].getMessage()).describedAs("msg").isEqualTo(new SimpleMessage("abc"));
        assertThat(event1[0].getThreadName()).describedAs("thread name").isEqualTo("THREAD 1");
        assertThat(event1[0].getThreadId()).describedAs("tid").isEqualTo(t1.getId());

        assertThat(event2[0].getLoggerName()).describedAs("logger").isEqualTo("b");
        assertThat(event2[0].getLevel()).describedAs("level").isEqualTo(Level.INFO);
        assertThat(event2[0].getMessage()).describedAs("msg").isEqualTo(new SimpleMessage("xyz"));
        assertThat(event2[0].getThreadName()).describedAs("thread name").isEqualTo("Thread 2");
        assertThat(event2[0].getThreadId()).describedAs("tid").isEqualTo(t2.getId());
        ReusableLogEventFactory.release(event1[0]);
        ReusableLogEventFactory.release(event2[0]);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCreateEventInitFieldsProperly() throws Exception {
        final ReusableLogEventFactory factory = new ReusableLogEventFactory();
        final LogEvent event = callCreateEvent(factory, "logger", Level.INFO, new SimpleMessage("xyz"), null);
        try {
            assertThat(event.getContextData()).isNotNull();
            assertThat(event.getContextStack()).isNotNull();
        } finally {
            ReusableLogEventFactory.release(event);
        }
    }

}
