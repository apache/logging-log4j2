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
import org.apache.logging.log4j.util.Supplier;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the AbstractLogger implementation of the Logger2 interface.
 */
public class LambdaLoggerTest {

    private static class LogEvent {
        @SuppressWarnings("unused")
        final String fqcn;
        final Level level;
        final Marker marker;
        final Message message;
        final Throwable throwable;

        public LogEvent(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
            this.fqcn = fqcn;
            this.level = level;
            this.marker = marker;
            this.message = message;
            this.throwable = t;
        }
    }

    private static class Logger2Impl extends AbstractLogger {
        private static final long serialVersionUID = 1L;

        boolean enabled = true;
        final List<LambdaLoggerTest.LogEvent> list = new ArrayList<>();

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final CharSequence message, final Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable t) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3,
                final Object p4) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3,
                final Object p4, final Object p5) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3,
                final Object p4, final Object p5, final Object p6) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3,
                final Object p4, final Object p5, final Object p6,
                final Object p7) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3,
                final Object p4, final Object p5, final Object p6,
                final Object p7, final Object p8) {
            return enabled;
        }

        @Override
        public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
                final Object p1, final Object p2, final Object p3,
                final Object p4, final Object p5, final Object p6,
                final Object p7, final Object p8, final Object p9) {
            return enabled;
        }

        @Override
        public void logMessage(final String fqcn, final Level level, final Marker marker, final Message message, final Throwable t) {
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
    final String stringMessage = "Hi";
    final Message message = new SimpleMessage("HiMessage");
    final Throwable throwable = new Error("I'm Bad");
    final Marker marker = MarkerManager.getMarker("test");

    private class MyMessageSupplier implements Supplier<Message> {
        public boolean invoked = false;

        @Override
        public Message get() {
            invoked = true;
            return message;
        }
    }

    final MyMessageSupplier messageSupplier = new MyMessageSupplier();

    private class MySupplier implements Supplier<String> {
        public boolean invoked = false;

        @Override
        public String get() {
            invoked = true;
            return stringMessage;
        }
    }

    final MySupplier supplier = new MySupplier();
    final MySupplier supplier2 = new MySupplier();
    final Supplier[] supplierArray1 = new Supplier[] {supplier};
    final Supplier[] supplierArray2 = new Supplier[] {supplier, supplier2};

    @Before
    public void beforeEachTest() {
        logger2.list.clear();
        supplier.invoked = false;
        messageSupplier.invoked = false;
    }

    @Test
    public void testDebugMarkerMessageSupplier() {
        logger2.disable().debug(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().debug(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testDebugMessageSupplier() {
        logger2.disable().debug(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().debug(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testDebugMarkerMessageSupplierThrowable() {
        logger2.disable().debug(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().debug(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testDebugMessageSupplierThrowable() {
        logger2.disable().debug(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().debug(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testDebugMarkerSupplier() {
        logger2.disable().debug(marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().debug(marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testDebugSupplier() {
        logger2.disable().debug(supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().debug(supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testDebugMarkerSupplierThrowable() {
        logger2.disable().debug(marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().debug(marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testDebugSupplierThrowable() {
        logger2.disable().debug(supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().debug(supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testDebugStringParamSupplier() {
        logger2.disable().debug("abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().debug("abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testDebugMarkerStringParamSupplier() {
        logger2.disable().debug(marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().debug(marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.DEBUG, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testErrorMarkerMessageSupplier() {
        logger2.disable().error(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().error(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testErrorMessageSupplier() {
        logger2.disable().error(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().error(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testErrorMarkerMessageSupplierThrowable() {
        logger2.disable().error(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().error(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testErrorMessageSupplierThrowable() {
        logger2.disable().error(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().error(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testErrorMarkerSupplier() {
        logger2.disable().error(marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().error(marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testErrorSupplier() {
        logger2.disable().error(supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().error(supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testErrorMarkerSupplierThrowable() {
        logger2.disable().error(marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().error(marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testErrorSupplierThrowable() {
        logger2.disable().error(supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().error(supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testErrorStringParamSupplier() {
        logger2.disable().error("abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().error("abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testErrorMarkerStringParamSupplier() {
        logger2.disable().error(marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().error(marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.ERROR, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testFatalMarkerMessageSupplier() {
        logger2.disable().fatal(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().fatal(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testFatalMessageSupplier() {
        logger2.disable().fatal(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().fatal(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testFatalMarkerMessageSupplierThrowable() {
        logger2.disable().fatal(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().fatal(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testFatalMessageSupplierThrowable() {
        logger2.disable().fatal(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().fatal(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testFatalMarkerSupplier() {
        logger2.disable().fatal(marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().fatal(marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testFatalSupplier() {
        logger2.disable().fatal(supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().fatal(supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testFatalMarkerSupplierThrowable() {
        logger2.disable().fatal(marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().fatal(marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testFatalSupplierThrowable() {
        logger2.disable().fatal(supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().fatal(supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testFatalStringParamSupplier() {
        logger2.disable().fatal("abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().fatal("abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testFatalStringParam2Suppliers() {
        logger2.disable().fatal("abc {}{}", supplierArray2);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);
        assertFalse(supplier2.invoked);

        logger2.enable().fatal("abc {}{}", supplierArray2);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);
        assertTrue(supplier2.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertEquals("abc HiHi", event.message.getFormattedMessage());
    }

    @Test
    public void testFatalMarkerStringParamSupplier() {
        logger2.disable().fatal(marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().fatal(marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.FATAL, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testInfoMarkerMessageSupplier() {
        logger2.disable().info(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().info(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testInfoMessageSupplier() {
        logger2.disable().info(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().info(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testInfoMarkerMessageSupplierThrowable() {
        logger2.disable().info(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().info(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testInfoMessageSupplierThrowable() {
        logger2.disable().info(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().info(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testInfoMarkerSupplier() {
        logger2.disable().info(marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().info(marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testInfoSupplier() {
        logger2.disable().info(supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().info(supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testInfoMarkerSupplierThrowable() {
        logger2.disable().info(marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().info(marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testInfoSupplierThrowable() {
        logger2.disable().info(supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().info(supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testInfoStringParamSupplier() {
        logger2.disable().info("abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().info("abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testInfoMarkerStringParamSupplier() {
        logger2.disable().info(marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().info(marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.INFO, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testTraceMarkerMessageSupplier() {
        logger2.disable().trace(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().trace(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testTraceMessageSupplier() {
        logger2.disable().trace(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().trace(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testTraceMarkerMessageSupplierThrowable() {
        logger2.disable().trace(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().trace(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testTraceMessageSupplierThrowable() {
        logger2.disable().trace(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().trace(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testTraceMarkerSupplier() {
        logger2.disable().trace(marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().trace(marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testTraceSupplier() {
        logger2.disable().trace(supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().trace(supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testTraceMarkerSupplierThrowable() {
        logger2.disable().trace(marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().trace(marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testTraceSupplierThrowable() {
        logger2.disable().trace(supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().trace(supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testTraceStringParamSupplier() {
        logger2.disable().trace("abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().trace("abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testTraceMarkerStringParamSupplier() {
        logger2.disable().trace(marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().trace(marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.TRACE, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testWarnMarkerMessageSupplier() {
        logger2.disable().warn(marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().warn(marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testWarnMessageSupplier() {
        logger2.disable().warn(messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().warn(messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testWarnMarkerMessageSupplierThrowable() {
        logger2.disable().warn(marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().warn(marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testWarnMessageSupplierThrowable() {
        logger2.disable().warn(messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().warn(messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testWarnMarkerSupplier() {
        logger2.disable().warn(marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().warn(marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testWarnSupplier() {
        logger2.disable().warn(supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().warn(supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testWarnMarkerSupplierThrowable() {
        logger2.disable().warn(marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().warn(marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testWarnSupplierThrowable() {
        logger2.disable().warn(supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().warn(supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testWarnStringParamSupplier() {
        logger2.disable().warn("abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().warn("abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testWarnMarkerStringParamSupplier() {
        logger2.disable().warn(marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().warn(marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testLogMarkerMessageSupplier() {
        logger2.disable().log(Level.WARN, marker, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().log(Level.WARN, marker, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(message, event.message);
        assertSame(marker, event.marker);
    }

    @Test
    public void testLogMessageSupplier() {
        logger2.disable().log(Level.WARN, messageSupplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().log(Level.WARN, messageSupplier);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(message, event.message);
    }

    @Test
    public void testLogMarkerMessageSupplierThrowable() {
        logger2.disable().log(Level.WARN, marker, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().log(Level.WARN, marker, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(marker, event.marker);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testLogMessageSupplierThrowable() {
        logger2.disable().log(Level.WARN, messageSupplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(messageSupplier.invoked);

        logger2.enable().log(Level.WARN, messageSupplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(messageSupplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(message, event.message);
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testLogMarkerSupplier() {
        logger2.disable().log(Level.WARN, marker, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().log(Level.WARN, marker, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(marker, event.marker);
    }

    @Test
    public void testLogSupplier() {
        logger2.disable().log(Level.WARN, supplier);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().log(Level.WARN, supplier);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
    }

    @Test
    public void testLogMarkerSupplierThrowable() {
        logger2.disable().log(Level.WARN, marker, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().log(Level.WARN, marker, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(marker, event.marker);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testLogSupplierThrowable() {
        logger2.disable().log(Level.WARN, supplier, throwable);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().log(Level.WARN, supplier, throwable);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(stringMessage, event.message.getFormattedMessage());
        assertSame(throwable, event.throwable);
    }

    @Test
    public void testLogStringParamSupplier() {
        logger2.disable().log(Level.WARN, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().log(Level.WARN, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

    @Test
    public void testLogMarkerStringParamSupplier() {
        logger2.disable().log(Level.WARN, marker, "abc {}", supplierArray1);
        assertTrue(logger2.list.isEmpty());
        assertFalse(supplier.invoked);

        logger2.enable().log(Level.WARN, marker, "abc {}", supplierArray1);
        assertEquals(1, logger2.list.size());
        assertTrue(supplier.invoked);

        final LogEvent event = logger2.list.get(0);
        assertEquals(Level.WARN, event.level);
        assertSame(marker, event.marker);
        assertEquals("abc Hi", event.message.getFormattedMessage());
    }

}
