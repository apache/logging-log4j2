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

package org.apache.logging.log4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.MessageSupplier;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the AbstractLogger implementation of the Logger2 interface.
 */
public class Logger2Test {

    private static class LogEvent {
        @SuppressWarnings("unused")
        final String fqcn;
        @SuppressWarnings("unused")
        final Level level;
        final Marker marker;
        final Message message;
        final Throwable throwable;

        public LogEvent(String fqcn, Level level, Marker marker, Message message, Throwable t) {
            this.fqcn = fqcn;
            this.level = level;
            this.marker = marker;
            this.message = message;
            this.throwable = t;
        }
    }

    class Logger2Impl extends AbstractLogger {
        private static final long serialVersionUID = 1L;

        boolean enabled = true;
        final List<Logger2Test.LogEvent> list = new ArrayList<Logger2Test.LogEvent>();

        @Override
        public boolean isEnabled(Level level, Marker marker, Message message, Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, Object message, Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message) {
            return enabled;
        }

        @Override
        public boolean isEnabled(Level level, Marker marker, String message, Object... params) {
            return enabled;
        }

        @Override
        public void logMessage(String fqcn, Level level, Marker marker, Message message, Throwable t) {
            list.add(new LogEvent(fqcn, level, marker, message, t));
        }

        @Override
        public Level getLevel() {
            return null;
        }

        public AbstractLogger disable() {
            enabled = false;
            return this;
        }

        public AbstractLogger enable() {
            enabled = true;
            return this;
        }
    }

    final Logger2Impl logger2 = new Logger2Impl();
    final Message message = new SimpleMessage("HiMessage");
    final Throwable throwable = new Error("I'm Bad");
    final Marker marker = MarkerManager.getMarker("test");

    class MyMessageSupplier implements MessageSupplier {
        public int count = 0;

        @Override
        public Message get() {
            count++;
            return message;
        }
    };

    final MyMessageSupplier messageSupplier = new MyMessageSupplier();

    @Before
    public void beforeEachTest() {
        logger2.list.clear();
        messageSupplier.count = 0;
    }

    @Test
    public void testDebugMarkerMessageSupplier() {
        logger2.disable().debug(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertEquals(0, messageSupplier.count);

        logger2.enable().debug(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertEquals(1, messageSupplier.count);

        LogEvent event = logger2.list.get(0);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testDebugMessageSupplier() {
        logger2.disable().debug(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertEquals(0, messageSupplier.count);

        logger2.enable().debug(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertEquals(1, messageSupplier.count);

        LogEvent event = logger2.list.get(0);
        assertSame(message, event.message);
    }

    @Test
    public void testDebugMarkerMessageSupplierThrowable() {
        logger2.disable().debug(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertEquals(0, messageSupplier.count);

        logger2.enable().debug(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertEquals(1, messageSupplier.count);

        LogEvent event = logger2.list.get(0);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testDebugMessageSupplierThrowable() {
        logger2.disable().debug(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertEquals(0, messageSupplier.count);

        logger2.enable().debug(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertEquals(1, messageSupplier.count);

        LogEvent event = logger2.list.get(0);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

}
