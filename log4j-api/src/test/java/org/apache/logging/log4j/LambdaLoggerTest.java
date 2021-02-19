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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
            this.message = (message instanceof ReusableMessage) ?
                    ((ReusableMessage) message).memento() :
                    message;
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
    final Supplier<?>[] supplierArray1 = new Supplier<?>[] {supplier};
    final Supplier<?>[] supplierArray2 = new Supplier<?>[] {supplier, supplier2};

    @BeforeEach
    public void beforeEachTest() {
        logger2.list.clear();
        supplier.invoked = false;
        messageSupplier.invoked = false;
    }

    @Test
    public void testDebugMarkerMessageSupplier() {
        logger2.disable().debug(marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().debug(marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testDebugMessageSupplier() {
        logger2.disable().debug(messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().debug(messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testDebugMarkerMessageSupplierThrowable() {
        logger2.disable().debug(marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().debug(marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testDebugMessageSupplierThrowable() {
        logger2.disable().debug(messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().debug(messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testDebugMarkerSupplier() {
        logger2.disable().debug(marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().debug(marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testDebugSupplier() {
        logger2.disable().debug(supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().debug(supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testDebugMarkerSupplierThrowable() {
        logger2.disable().debug(marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().debug(marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testDebugSupplierThrowable() {
        logger2.disable().debug(supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().debug(supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testDebugStringParamSupplier() {
        logger2.disable().debug("abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().debug("abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testDebugMarkerStringParamSupplier() {
        logger2.disable().debug(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().debug(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.DEBUG);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testErrorMarkerMessageSupplier() {
        logger2.disable().error(marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().error(marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testErrorMessageSupplier() {
        logger2.disable().error(messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().error(messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testErrorMarkerMessageSupplierThrowable() {
        logger2.disable().error(marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().error(marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testErrorMessageSupplierThrowable() {
        logger2.disable().error(messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().error(messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testErrorMarkerSupplier() {
        logger2.disable().error(marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().error(marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testErrorSupplier() {
        logger2.disable().error(supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().error(supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testErrorMarkerSupplierThrowable() {
        logger2.disable().error(marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().error(marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testErrorSupplierThrowable() {
        logger2.disable().error(supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().error(supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testErrorStringParamSupplier() {
        logger2.disable().error("abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().error("abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testErrorMarkerStringParamSupplier() {
        logger2.disable().error(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().error(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.ERROR);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testFatalMarkerMessageSupplier() {
        logger2.disable().fatal(marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().fatal(marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testFatalMessageSupplier() {
        logger2.disable().fatal(messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().fatal(messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testFatalMarkerMessageSupplierThrowable() {
        logger2.disable().fatal(marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().fatal(marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testFatalMessageSupplierThrowable() {
        logger2.disable().fatal(messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().fatal(messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testFatalMarkerSupplier() {
        logger2.disable().fatal(marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().fatal(marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testFatalSupplier() {
        logger2.disable().fatal(supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().fatal(supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testFatalMarkerSupplierThrowable() {
        logger2.disable().fatal(marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().fatal(marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testFatalSupplierThrowable() {
        logger2.disable().fatal(supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().fatal(supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testFatalStringParamSupplier() {
        logger2.disable().fatal("abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().fatal("abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testFatalStringParam2Suppliers() {
        logger2.disable().fatal("abc {}{}", supplierArray2);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();
        assertThat(supplier2.invoked).isFalse();

        logger2.enable().fatal("abc {}{}", supplierArray2);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();
        assertThat(supplier2.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc HiHi");
    }

    @Test
    public void testFatalMarkerStringParamSupplier() {
        logger2.disable().fatal(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().fatal(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.FATAL);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testInfoMarkerMessageSupplier() {
        logger2.disable().info(marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().info(marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testInfoMessageSupplier() {
        logger2.disable().info(messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().info(messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testInfoMarkerMessageSupplierThrowable() {
        logger2.disable().info(marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().info(marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testInfoMessageSupplierThrowable() {
        logger2.disable().info(messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().info(messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testInfoMarkerSupplier() {
        logger2.disable().info(marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().info(marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testInfoSupplier() {
        logger2.disable().info(supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().info(supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testInfoMarkerSupplierThrowable() {
        logger2.disable().info(marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().info(marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testInfoSupplierThrowable() {
        logger2.disable().info(supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().info(supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testInfoStringParamSupplier() {
        logger2.disable().info("abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().info("abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testInfoMarkerStringParamSupplier() {
        logger2.disable().info(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().info(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.INFO);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testTraceMarkerMessageSupplier() {
        logger2.disable().trace(marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().trace(marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testTraceMessageSupplier() {
        logger2.disable().trace(messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().trace(messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testTraceMarkerMessageSupplierThrowable() {
        logger2.disable().trace(marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().trace(marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testTraceMessageSupplierThrowable() {
        logger2.disable().trace(messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().trace(messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testTraceMarkerSupplier() {
        logger2.disable().trace(marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().trace(marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testTraceSupplier() {
        logger2.disable().trace(supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().trace(supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testTraceMarkerSupplierThrowable() {
        logger2.disable().trace(marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().trace(marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testTraceSupplierThrowable() {
        logger2.disable().trace(supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().trace(supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testTraceStringParamSupplier() {
        logger2.disable().trace("abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().trace("abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testTraceMarkerStringParamSupplier() {
        logger2.disable().trace(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().trace(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.TRACE);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testWarnMarkerMessageSupplier() {
        logger2.disable().warn(marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().warn(marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testWarnMessageSupplier() {
        logger2.disable().warn(messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().warn(messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testWarnMarkerMessageSupplierThrowable() {
        logger2.disable().warn(marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().warn(marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testWarnMessageSupplierThrowable() {
        logger2.disable().warn(messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().warn(messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testWarnMarkerSupplier() {
        logger2.disable().warn(marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().warn(marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testWarnSupplier() {
        logger2.disable().warn(supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().warn(supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testWarnMarkerSupplierThrowable() {
        logger2.disable().warn(marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().warn(marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testWarnSupplierThrowable() {
        logger2.disable().warn(supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().warn(supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testWarnStringParamSupplier() {
        logger2.disable().warn("abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().warn("abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testWarnMarkerStringParamSupplier() {
        logger2.disable().warn(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().warn(marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testLogMarkerMessageSupplier() {
        logger2.disable().log(Level.WARN, marker, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, marker, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message).isSameAs(message);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testLogMessageSupplier() {
        logger2.disable().log(Level.WARN, messageSupplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, messageSupplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message).isSameAs(message);
    }

    @Test
    public void testLogMarkerMessageSupplierThrowable() {
        logger2.disable().log(Level.WARN, marker, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, marker, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testLogMessageSupplierThrowable() {
        logger2.disable().log(Level.WARN, messageSupplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(messageSupplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, messageSupplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(messageSupplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message).isSameAs(message);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testLogMarkerSupplier() {
        logger2.disable().log(Level.WARN, marker, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, marker, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.marker).isSameAs(marker);
    }

    @Test
    public void testLogSupplier() {
        logger2.disable().log(Level.WARN, supplier);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, supplier);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
    }

    @Test
    public void testLogMarkerSupplierThrowable() {
        logger2.disable().log(Level.WARN, marker, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, marker, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testLogSupplierThrowable() {
        logger2.disable().log(Level.WARN, supplier, throwable);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, supplier, throwable);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isSameAs(stringMessage);
        assertThat(event.throwable).isSameAs(throwable);
    }

    @Test
    public void testLogStringParamSupplier() {
        logger2.disable().log(Level.WARN, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

    @Test
    public void testLogMarkerStringParamSupplier() {
        logger2.disable().log(Level.WARN, marker, "abc {}", supplierArray1);
        assertThat(logger2.list.isEmpty()).isTrue();
        assertThat(supplier.invoked).isFalse();

        logger2.enable().log(Level.WARN, marker, "abc {}", supplierArray1);
        assertThat(logger2.list.size()).isEqualTo(1);
        assertThat(supplier.invoked).isTrue();

        final LogEvent event = logger2.list.get(0);
        assertThat(event.level).isEqualTo(Level.WARN);
        assertThat(event.marker).isSameAs(marker);
        assertThat(event.message.getFormattedMessage()).isEqualTo("abc Hi");
    }

}
