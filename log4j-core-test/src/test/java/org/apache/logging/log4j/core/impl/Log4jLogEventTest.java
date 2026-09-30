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

import static org.apache.logging.log4j.test.junit.SerialUtil.deserialize;
import static org.apache.logging.log4j.test.junit.SerialUtil.serialize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ObjectStreamException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.convert.Base64Converter;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.ClockFactoryTest;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.UnserializableMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class Log4jLogEventTest {

    /** Helper class */
    public static class FixedTimeClock implements Clock {
        public static final long FIXED_TIME = 1234567890L;

        /*
         * (non-Javadoc)
         *
         * @see org.apache.logging.log4j.core.helpers.Clock#currentTimeMillis()
         */
        @Override
        public long currentTimeMillis() {
            return FIXED_TIME;
        }
    }

    @BeforeAll
    static void beforeClass() {
        System.setProperty(ClockFactory.PROPERTY_NAME, FixedTimeClock.class.getName());
    }

    @AfterAll
    static void afterClass() throws IllegalAccessException {
        ClockFactoryTest.resetClocks();
    }

    @Test
    void testToImmutableSame() {
        final LogEvent logEvent = new Log4jLogEvent();
        assertSame(logEvent, logEvent.toImmutable());
    }

    @Test
    void testToImmutableNotSame() {
        final LogEvent logEvent = new Log4jLogEvent.Builder()
                .setMessage(new ReusableObjectMessage())
                .build();
        final LogEvent immutable = logEvent.toImmutable();
        assertSame(logEvent, immutable);
        assertFalse(immutable.getMessage() instanceof ReusableMessage);
    }

    @Test
    @Tag(Tags.SERIALIZATION)
    void testJavaIoSerializable() {
        final Log4jLogEvent evt = Log4jLogEvent.newBuilder() //
                .setLoggerName("some.test") //
                .setLoggerFqcn(Strings.EMPTY) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("abc")) //
                .build();

        final byte[] binary = serialize(evt);
        final Log4jLogEvent evt2 = deserialize(binary);

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFqcn(), evt2.getLoggerFqcn());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
        assertEquals(evt.getContextData(), evt2.getContextData());
        assertEquals(evt.getContextStack(), evt2.getContextStack());
        assertEquals(evt.getMessage(), evt2.getMessage());
        assertEquals(evt.getSource(), evt2.getSource());
        assertEquals(evt.getThreadName(), evt2.getThreadName());
        assertEquals(evt.getThrown(), evt2.getThrown());
        assertEquals(evt.isEndOfBatch(), evt2.isEndOfBatch());
        assertEquals(evt.isIncludeLocation(), evt2.isIncludeLocation());
    }

    @Test
    @Tag(Tags.SERIALIZATION)
    void testJavaIoSerializableWithUnserializableMessage() {
        final Log4jLogEvent evt = Log4jLogEvent.newBuilder() //
                .setLoggerName("some.test") //
                .setLoggerFqcn(Strings.EMPTY) //
                .setLevel(Level.INFO) //
                .setMessage(new UnserializableMessage("abc")) //
                .build();

        final Log4jLogEvent evt2 = deserialize(serialize(evt));

        assertEquals(new SimpleMessage("abc"), evt2.getMessage());
    }

    @Test
    @Tag(Tags.SERIALIZATION)
    void testJavaIoSerializableWithClassNotOnAllowlist() {
        final Log4jLogEvent evt = Log4jLogEvent.newBuilder() //
                .setLoggerName("some.test") //
                .setLoggerFqcn(Strings.EMPTY) //
                .setLevel(Level.INFO) //
                .setMessage(new ObjectMessage(URI.create("https://logging.apache.org/"))) //
                .build();

        final byte[] binary = serialize(evt);
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> deserialize(binary));
        assertInstanceOf(ObjectStreamException.class, e.getCause());
    }

    @Test
    @Tag(Tags.SERIALIZATION)
    void testJavaIoSerializableWithThrown() {
        final Error thrown = new InternalError("test error");
        final Log4jLogEvent evt = Log4jLogEvent.newBuilder() //
                .setLoggerName("some.test") //
                .setLoggerFqcn(Strings.EMPTY) //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("abc")) //
                .setThrown(thrown) //
                .build();

        final byte[] binary = serialize(evt);
        final Log4jLogEvent evt2 = deserialize(binary);

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFqcn(), evt2.getLoggerFqcn());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
        assertEquals(evt.getContextData(), evt2.getContextData());
        assertEquals(evt.getContextStack(), evt2.getContextStack());
        assertEquals(evt.getMessage(), evt2.getMessage());
        assertEquals(evt.getSource(), evt2.getSource());
        assertEquals(evt.getThreadName(), evt2.getThreadName());
        assertNull(evt2.getThrown());
        assertNotNull(evt2.getThrownProxy());
        assertEquals(evt.getThrownProxy(), evt2.getThrownProxy());
        assertEquals(evt.isEndOfBatch(), evt2.isEndOfBatch());
        assertEquals(evt.isIncludeLocation(), evt2.isIncludeLocation());
    }

    // DO NOT REMOVE THIS COMMENT:
    // UNCOMMENT WHEN GENERATING SERIALIZED EVENT FOR #testJavaIoSerializableWithUnknownThrowable
    // public static class DeletedException extends Exception {
    // private static final long serialVersionUID = 1L;
    // public DeletedException(String msg) {
    // super(msg);
    // }
    // };

    @Test
    @Tag(Tags.SERIALIZATION)
    void testJavaIoSerializableWithUnknownThrowable() {
        final String loggerName = "some.test";
        final Marker marker = null;
        final String loggerFQN = Strings.EMPTY;
        final Level level = Level.INFO;
        final Message msg = new SimpleMessage("abc");
        final String threadName = Thread.currentThread().getName();
        final String errorMessage = "OMG I've been deleted!";

        // DO NOT DELETE THIS COMMENT:
        // UNCOMMENT TO RE-GENERATE SERIALIZED EVENT WHEN UPDATING THIS TEST.
        // final Exception thrown = new DeletedException(errorMessage);
        // final Log4jLogEvent evt = new Log4jLogEvent(loggerName, marker, loggerFQN, level, msg, thrown);
        // final byte[] binary = serialize(evt);
        // final String base64Str = java.util.Base64.getEncoder().encodeToString(binary);
        // System.out.println("final String base64 = \"" + base64Str.replaceAll(".{88}", "$0\" + \"") + "\";");

        final String base64 = "rO0ABXNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnQkTG9nRXZlbnRQ"
                + "cm94eYgtmn+yXsP9AwATWgAMaXNFbmRPZkJhdGNoWgASaXNMb2NhdGlvblJlcXVpcmVkSQARbmFub09mTWlsbGlz"
                + "ZWNvbmRKAAh0aHJlYWRJZEkADnRocmVhZFByaW9yaXR5SgAKdGltZU1pbGxpc0wAC2NvbnRleHREYXRhdAApTG9y"
                + "Zy9hcGFjaGUvbG9nZ2luZy9sb2c0ai91dGlsL1N0cmluZ01hcDtMAAxjb250ZXh0U3RhY2t0ADVMb3JnL2FwYWNo"
                + "ZS9sb2dnaW5nL2xvZzRqL1RocmVhZENvbnRleHQkQ29udGV4dFN0YWNrO0wABWxldmVsdAAgTG9yZy9hcGFjaGUv"
                + "bG9nZ2luZy9sb2c0ai9MZXZlbDtMAApsb2dnZXJGUUNOdAASTGphdmEvbGFuZy9TdHJpbmc7TAAKbG9nZ2VyTmFt"
                + "ZXEAfgAETAAGbWFya2VydAAhTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9NYXJrZXI7TAANbWVzc2FnZVN0cmlu"
                + "Z3EAfgAETAAGc291cmNldAAdTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDtMAAZzcGFuSWRxAH4ABEwACnRo"
                + "cmVhZE5hbWVxAH4ABEwAC3Rocm93blByb3h5dAAzTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwv"
                + "VGhyb3dhYmxlUHJveHk7TAAKdHJhY2VGbGFnc3EAfgAETAAHdHJhY2VJZHEAfgAEeHAAAAAAAAAAAAAAAAAAAQAA"
                + "AAUAAAAASZYC0nNyADJvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGoudXRpbC5Tb3J0ZWRBcnJheVN0cmluZ01hcLA3"
                + "yJFz7CvcAwACWgAJaW1tdXRhYmxlSQAJdGhyZXNob2xkeHABAAAAAXcIAAAAAQAAAAB4c3IAPm9yZy5hcGFjaGUu"
                + "bG9nZ2luZy5sb2c0ai5UaHJlYWRDb250ZXh0JEVtcHR5VGhyZWFkQ29udGV4dFN0YWNrAAAAAAAAAAECAAB4cHNy"
                + "AB5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouTGV2ZWwAAAAAABggGgIAA0kACGludExldmVsTAAEbmFtZXEAfgAE"
                + "TAANc3RhbmRhcmRMZXZlbHQALExvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovc3BpL1N0YW5kYXJkTGV2ZWw7eHAA"
                + "AAGQdAAESU5GT35yACpvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouc3BpLlN0YW5kYXJkTGV2ZWwAAAAAAAAAABIA"
                + "AHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AARJTkZPdAAAdAAJc29tZS50ZXN0cHQAA2FiY3BxAH4A"
                + "FXQABG1haW5zcgAxb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5UaHJvd2FibGVQcm94ednMMNWa"
                + "e6z6AgAHSQASY29tbW9uRWxlbWVudENvdW50TAAKY2F1c2VQcm94eXEAfgAHWwASZXh0ZW5kZWRTdGFja1RyYWNl"
                + "dAA/W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7"
                + "TAAQbG9jYWxpemVkTWVzc2FnZXEAfgAETAAHbWVzc2FnZXEAfgAETAAEbmFtZXEAfgAEWwARc3VwcHJlc3NlZFBy"
                + "b3hpZXN0ADRbTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvVGhyb3dhYmxlUHJveHk7eHAAAAAA"
                + "cHVyAD9bTG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuRXh0ZW5kZWRTdGFja1RyYWNlRWxlbWVu"
                + "dDvKz4gjpcfPvAIAAHhwAAAATnNyADxvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVk"
                + "U3RhY2tUcmFjZUVsZW1lbnTh3s+6xraQBwIAAkwADmV4dHJhQ2xhc3NJbmZvdAA2TG9yZy9hcGFjaGUvbG9nZ2lu"
                + "Zy9sb2c0ai9jb3JlL2ltcGwvRXh0ZW5kZWRDbGFzc0luZm87TAARc3RhY2tUcmFjZUVsZW1lbnRxAH4ABnhwc3IA"
                + "NG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuRXh0ZW5kZWRDbGFzc0luZm8AAAAAAAAAAQIAA1oA"
                + "BWV4YWN0TAAIbG9jYXRpb25xAH4ABEwAB3ZlcnNpb25xAH4ABHhwAHQADXRlc3QtY2xhc3Nlcy90AAE/c3IAG2ph"
                + "dmEubGFuZy5TdGFja1RyYWNlRWxlbWVudGEJxZomNt2FAgAIQgAGZm9ybWF0SQAKbGluZU51bWJlckwAD2NsYXNz"
                + "TG9hZGVyTmFtZXEAfgAETAAOZGVjbGFyaW5nQ2xhc3NxAH4ABEwACGZpbGVOYW1lcQB+AARMAAptZXRob2ROYW1l"
                + "cQB+AARMAAptb2R1bGVOYW1lcQB+AARMAA1tb2R1bGVWZXJzaW9ucQB+AAR4cAEAAAC6dAADYXBwdAA0b3JnLmFw"
                + "YWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5Mb2c0akxvZ0V2ZW50VGVzdHQAFkxvZzRqTG9nRXZlbnRUZXN0"
                + "LmphdmF0ACp0ZXN0SmF2YUlvU2VyaWFsaXphYmxlV2l0aFVua25vd25UaHJvd2FibGVwcHNxAH4AH3NxAH4AIgBx"
                + "AH4AJXEAfgAlc3EAfgAmAv////5wdAAtamRrLmludGVybmFsLnJlZmxlY3QuTmF0aXZlTWV0aG9kQWNjZXNzb3JJ"
                + "bXBsdAAdTmF0aXZlTWV0aG9kQWNjZXNzb3JJbXBsLmphdmF0AAdpbnZva2UwdAAJamF2YS5iYXNldAAJMTcuMC4y"
                + "MC4xc3EAfgAfcQB+AC1zcQB+ACYCAAAATXBxAH4AL3EAfgAwdAAGaW52b2tlcQB+ADJxAH4AM3NxAH4AH3NxAH4A"
                + "IgBxAH4AJXEAfgAlc3EAfgAmAgAAACtwdAAxamRrLmludGVybmFsLnJlZmxlY3QuRGVsZWdhdGluZ01ldGhvZEFj"
                + "Y2Vzc29ySW1wbHQAIURlbGVnYXRpbmdNZXRob2RBY2Nlc3NvckltcGwuamF2YXEAfgA2cQB+ADJxAH4AM3NxAH4A"
                + "H3NxAH4AIgBxAH4AJXEAfgAlc3EAfgAmAgAAAjlwdAAYamF2YS5sYW5nLnJlZmxlY3QuTWV0aG9kdAALTWV0aG9k"
                + "LmphdmFxAH4ANnEAfgAycQB+ADNzcQB+AB9zcQB+ACIAdAAhanVuaXQtcGxhdGZvcm0tY29tbW9ucy0xLjE0LjQu"
                + "amFydAAGMS4xNC40c3EAfgAmAQAAAxJxAH4AKHQAL29yZy5qdW5pdC5wbGF0Zm9ybS5jb21tb25zLnV0aWwuUmVm"
                + "bGVjdGlvblV0aWxzdAAUUmVmbGVjdGlvblV0aWxzLmphdmF0AAxpbnZva2VNZXRob2RwcHNxAH4AH3NxAH4AIgB0"
                + "ACFqdW5pdC1wbGF0Zm9ybS1jb21tb25zLTEuMTQuNC5qYXJxAH4ARHNxAH4AJgEAAAICcQB+ACh0ADRvcmcuanVu"
                + "aXQucGxhdGZvcm0uY29tbW9ucy5zdXBwb3J0LlJlZmxlY3Rpb25TdXBwb3J0dAAWUmVmbGVjdGlvblN1cHBvcnQu"
                + "amF2YXEAfgBIcHBzcQB+AB9zcQB+ACIAdAAfanVuaXQtanVwaXRlci1lbmdpbmUtNS4xNC40LmphcnQABjUuMTQu"
                + "NHNxAH4AJgEAAAA8cQB+ACh0ADNvcmcuanVuaXQuanVwaXRlci5lbmdpbmUuZXhlY3V0aW9uLk1ldGhvZEludm9j"
                + "YXRpb250ABVNZXRob2RJbnZvY2F0aW9uLmphdmF0AAdwcm9jZWVkcHBzcQB+AB9zcQB+ACIAdAAfanVuaXQtanVw"
                + "aXRlci1lbmdpbmUtNS4xNC40LmphcnEAfgBSc3EAfgAmAQAAAINxAH4AKHQAUm9yZy5qdW5pdC5qdXBpdGVyLmVu"
                + "Z2luZS5leGVjdXRpb24uSW52b2NhdGlvbkludGVyY2VwdG9yQ2hhaW4kVmFsaWRhdGluZ0ludm9jYXRpb250AB9J"
                + "bnZvY2F0aW9uSW50ZXJjZXB0b3JDaGFpbi5qYXZhcQB+AFZwcHNxAH4AH3NxAH4AIgB0AB9qdW5pdC1qdXBpdGVy"
                + "LWVuZ2luZS01LjE0LjQuamFycQB+AFJzcQB+ACYBAAAAoXEAfgAodAAzb3JnLmp1bml0Lmp1cGl0ZXIuZW5naW5l"
                + "LmV4dGVuc2lvbi5UaW1lb3V0RXh0ZW5zaW9udAAVVGltZW91dEV4dGVuc2lvbi5qYXZhdAAJaW50ZXJjZXB0cHBz"
                + "cQB+AB9xAH4AXnNxAH4AJgEAAACYcQB+AChxAH4AYXEAfgBidAAXaW50ZXJjZXB0VGVzdGFibGVNZXRob2RwcHNx"
                + "AH4AH3EAfgBec3EAfgAmAQAAAFtxAH4AKHEAfgBhcQB+AGJ0ABNpbnRlcmNlcHRUZXN0TWV0aG9kcHBzcQB+AB9z"
                + "cQB+ACIAdAAfanVuaXQtanVwaXRlci1lbmdpbmUtNS4xNC40LmphcnEAfgBSc3EAfgAmAQAAAHBxAH4AKHQAWm9y"
                + "Zy5qdW5pdC5qdXBpdGVyLmVuZ2luZS5leGVjdXRpb24uSW50ZXJjZXB0aW5nRXhlY3V0YWJsZUludm9rZXIkUmVm"
                + "bGVjdGl2ZUludGVyY2VwdG9yQ2FsbHQAIkludGVyY2VwdGluZ0V4ZWN1dGFibGVJbnZva2VyLmphdmF0ABVsYW1i"
                + "ZGEkb2ZWb2lkTWV0aG9kJDBwcHNxAH4AH3NxAH4AIgB0AB9qdW5pdC1qdXBpdGVyLWVuZ2luZS01LjE0LjQuamFy"
                + "cQB+AFJzcQB+ACYBAAAAXnEAfgAodABAb3JnLmp1bml0Lmp1cGl0ZXIuZW5naW5lLmV4ZWN1dGlvbi5JbnRlcmNl"
                + "cHRpbmdFeGVjdXRhYmxlSW52b2tlcnEAfgBvdAAPbGFtYmRhJGludm9rZSQwcHBzcQB+AB9zcQB+ACIAdAAfanVu"
                + "aXQtanVwaXRlci1lbmdpbmUtNS4xNC40LmphcnEAfgBSc3EAfgAmAQAAAGpxAH4AKHQAU29yZy5qdW5pdC5qdXBp"
                + "dGVyLmVuZ2luZS5leGVjdXRpb24uSW52b2NhdGlvbkludGVyY2VwdG9yQ2hhaW4kSW50ZXJjZXB0ZWRJbnZvY2F0"
                + "aW9ucQB+AFxxAH4AVnBwc3EAfgAfc3EAfgAiAHQAH2p1bml0LWp1cGl0ZXItZW5naW5lLTUuMTQuNC5qYXJxAH4A"
                + "UnNxAH4AJgEAAABAcQB+ACh0AD1vcmcuanVuaXQuanVwaXRlci5lbmdpbmUuZXhlY3V0aW9uLkludm9jYXRpb25J"
                + "bnRlcmNlcHRvckNoYWlucQB+AFxxAH4AVnBwc3EAfgAfcQB+AH1zcQB+ACYBAAAALXEAfgAocQB+AIBxAH4AXHQA"
                + "DmNoYWluQW5kSW52b2tlcHBzcQB+AB9xAH4AfXNxAH4AJgEAAAAlcQB+AChxAH4AgHEAfgBccQB+ADZwcHNxAH4A"
                + "H3EAfgByc3EAfgAmAQAAAF1xAH4AKHEAfgB1cQB+AG9xAH4ANnBwc3EAfgAfcQB+AHJzcQB+ACYBAAAAV3EAfgAo"
                + "cQB+AHVxAH4Ab3EAfgA2cHBzcQB+AB9zcQB+ACIAdAAfanVuaXQtanVwaXRlci1lbmdpbmUtNS4xNC40LmphcnEA"
                + "fgBSc3EAfgAmAQAAAN1xAH4AKHQAPG9yZy5qdW5pdC5qdXBpdGVyLmVuZ2luZS5kZXNjcmlwdG9yLlRlc3RNZXRo"
                + "b2RUZXN0RGVzY3JpcHRvcnQAHVRlc3RNZXRob2RUZXN0RGVzY3JpcHRvci5qYXZhdAAZbGFtYmRhJGludm9rZVRl"
                + "c3RNZXRob2QkNHBwc3EAfgAfc3EAfgAiAHQAIGp1bml0LXBsYXRmb3JtLWVuZ2luZS0xLjE0LjQuamFydAAGMS4x"
                + "NC40c3EAfgAmAQAAAElxAH4AKHQAQW9yZy5qdW5pdC5wbGF0Zm9ybS5lbmdpbmUuc3VwcG9ydC5oaWVyYXJjaGlj"
                + "YWwuVGhyb3dhYmxlQ29sbGVjdG9ydAAXVGhyb3dhYmxlQ29sbGVjdG9yLmphdmF0AAdleGVjdXRlcHBzcQB+AB9x"
                + "AH4Ai3NxAH4AJgEAAADZcQB+AChxAH4AjnEAfgCPdAAQaW52b2tlVGVzdE1ldGhvZHBwc3EAfgAfcQB+AItzcQB+"
                + "ACYBAAAAn3EAfgAocQB+AI5xAH4Aj3EAfgCYcHBzcQB+AB9xAH4Ai3NxAH4AJgEAAABGcQB+AChxAH4AjnEAfgCP"
                + "cQB+AJhwcHNxAH4AH3NxAH4AIgB0ACBqdW5pdC1wbGF0Zm9ybS1lbmdpbmUtMS4xNC40LmphcnEAfgCUc3EAfgAm"
                + "AQAAAJ1xAH4AKHQAO29yZy5qdW5pdC5wbGF0Zm9ybS5lbmdpbmUuc3VwcG9ydC5oaWVyYXJjaGljYWwuTm9kZVRl"
                + "c3RUYXNrdAARTm9kZVRlc3RUYXNrLmphdmF0ABtsYW1iZGEkZXhlY3V0ZVJlY3Vyc2l2ZWx5JDZwcHNxAH4AH3EA"
                + "fgCSc3EAfgAmAQAAAElxAH4AKHEAfgCWcQB+AJdxAH4AmHBwc3EAfgAfcQB+AKFzcQB+ACYBAAAAk3EAfgAocQB+"
                + "AKRxAH4ApXQAG2xhbWJkYSRleGVjdXRlUmVjdXJzaXZlbHkkOHBwc3EAfgAfc3EAfgAiAHQAIGp1bml0LXBsYXRm"
                + "b3JtLWVuZ2luZS0xLjE0LjQuamFycQB+AJRzcQB+ACYBAAAAiXEAfgAodAAzb3JnLmp1bml0LnBsYXRmb3JtLmVu"
                + "Z2luZS5zdXBwb3J0LmhpZXJhcmNoaWNhbC5Ob2RldAAJTm9kZS5qYXZhdAAGYXJvdW5kcHBzcQB+AB9xAH4AoXNx"
                + "AH4AJgEAAACRcQB+AChxAH4ApHEAfgCldAAbbGFtYmRhJGV4ZWN1dGVSZWN1cnNpdmVseSQ5cHBzcQB+AB9xAH4A"
                + "knNxAH4AJgEAAABJcQB+AChxAH4AlnEAfgCXcQB+AJhwcHNxAH4AH3EAfgChc3EAfgAmAQAAAJBxAH4AKHEAfgCk"
                + "cQB+AKV0ABJleGVjdXRlUmVjdXJzaXZlbHlwcHNxAH4AH3EAfgChc3EAfgAmAQAAAGVxAH4AKHEAfgCkcQB+AKVx"
                + "AH4AmHBwc3EAfgAfc3EAfgAiAHEAfgAlcQB+ACVzcQB+ACYCAAAF53B0ABNqYXZhLnV0aWwuQXJyYXlMaXN0dAAO"
                + "QXJyYXlMaXN0LmphdmF0AAdmb3JFYWNocQB+ADJxAH4AM3NxAH4AH3NxAH4AIgB0ACBqdW5pdC1wbGF0Zm9ybS1l"
                + "bmdpbmUtMS4xNC40LmphcnEAfgCUc3EAfgAmAQAAAClxAH4AKHQAWG9yZy5qdW5pdC5wbGF0Zm9ybS5lbmdpbmUu"
                + "c3VwcG9ydC5oaWVyYXJjaGljYWwuU2FtZVRocmVhZEhpZXJhcmNoaWNhbFRlc3RFeGVjdXRvclNlcnZpY2V0AC5T"
                + "YW1lVGhyZWFkSGllcmFyY2hpY2FsVGVzdEV4ZWN1dG9yU2VydmljZS5qYXZhdAAJaW52b2tlQWxscHBzcQB+AB9x"
                + "AH4AoXNxAH4AJgEAAAChcQB+AChxAH4ApHEAfgClcQB+AKZwcHNxAH4AH3EAfgCSc3EAfgAmAQAAAElxAH4AKHEA"
                + "fgCWcQB+AJdxAH4AmHBwc3EAfgAfcQB+AKFzcQB+ACYBAAAAk3EAfgAocQB+AKRxAH4ApXEAfgCrcHBzcQB+AB9x"
                + "AH4ArXNxAH4AJgEAAACJcQB+AChxAH4AsHEAfgCxcQB+ALJwcHNxAH4AH3EAfgChc3EAfgAmAQAAAJFxAH4AKHEA"
                + "fgCkcQB+AKVxAH4AtXBwc3EAfgAfcQB+AJJzcQB+ACYBAAAASXEAfgAocQB+AJZxAH4Al3EAfgCYcHBzcQB+AB9x"
                + "AH4AoXNxAH4AJgEAAACQcQB+AChxAH4ApHEAfgClcQB+ALpwcHNxAH4AH3EAfgChc3EAfgAmAQAAAGVxAH4AKHEA"
                + "fgCkcQB+AKVxAH4AmHBwc3EAfgAfcQB+AL5zcQB+ACYCAAAF53BxAH4AwHEAfgDBcQB+AMJxAH4AMnEAfgAzc3EA"
                + "fgAfcQB+AMRzcQB+ACYBAAAAKXEAfgAocQB+AMdxAH4AyHEAfgDJcHBzcQB+AB9xAH4AoXNxAH4AJgEAAAChcQB+"
                + "AChxAH4ApHEAfgClcQB+AKZwcHNxAH4AH3EAfgCSc3EAfgAmAQAAAElxAH4AKHEAfgCWcQB+AJdxAH4AmHBwc3EA"
                + "fgAfcQB+AKFzcQB+ACYBAAAAk3EAfgAocQB+AKRxAH4ApXEAfgCrcHBzcQB+AB9xAH4ArXNxAH4AJgEAAACJcQB+"
                + "AChxAH4AsHEAfgCxcQB+ALJwcHNxAH4AH3EAfgChc3EAfgAmAQAAAJFxAH4AKHEAfgCkcQB+AKVxAH4AtXBwc3EA"
                + "fgAfcQB+AJJzcQB+ACYBAAAASXEAfgAocQB+AJZxAH4Al3EAfgCYcHBzcQB+AB9xAH4AoXNxAH4AJgEAAACQcQB+"
                + "AChxAH4ApHEAfgClcQB+ALpwcHNxAH4AH3EAfgChc3EAfgAmAQAAAGVxAH4AKHEAfgCkcQB+AKVxAH4AmHBwc3EA"
                + "fgAfcQB+AMRzcQB+ACYBAAAAI3EAfgAocQB+AMdxAH4AyHQABnN1Ym1pdHBwc3EAfgAfc3EAfgAiAHQAIGp1bml0"
                + "LXBsYXRmb3JtLWVuZ2luZS0xLjE0LjQuamFycQB+AJRzcQB+ACYBAAAAOXEAfgAodABHb3JnLmp1bml0LnBsYXRm"
                + "b3JtLmVuZ2luZS5zdXBwb3J0LmhpZXJhcmNoaWNhbC5IaWVyYXJjaGljYWxUZXN0RXhlY3V0b3J0AB1IaWVyYXJj"
                + "aGljYWxUZXN0RXhlY3V0b3IuamF2YXEAfgCYcHBzcQB+AB9zcQB+ACIAdAAganVuaXQtcGxhdGZvcm0tZW5naW5l"
                + "LTEuMTQuNC5qYXJxAH4AlHNxAH4AJgEAAAA2cQB+ACh0AEVvcmcuanVuaXQucGxhdGZvcm0uZW5naW5lLnN1cHBv"
                + "cnQuaGllcmFyY2hpY2FsLkhpZXJhcmNoaWNhbFRlc3RFbmdpbmV0ABtIaWVyYXJjaGljYWxUZXN0RW5naW5lLmph"
                + "dmFxAH4AmHBwc3EAfgAfc3EAfgAiAHQAImp1bml0LXBsYXRmb3JtLWxhdW5jaGVyLTEuMTQuNC5qYXJ0AAYxLjE0"
                + "LjRzcQB+ACYBAAAA5nEAfgAodAA8b3JnLmp1bml0LnBsYXRmb3JtLmxhdW5jaGVyLmNvcmUuRW5naW5lRXhlY3V0"
                + "aW9uT3JjaGVzdHJhdG9ydAAgRW5naW5lRXhlY3V0aW9uT3JjaGVzdHJhdG9yLmphdmF0AA1leGVjdXRlRW5naW5l"
                + "cHBzcQB+AB9xAH4A/nNxAH4AJgEAAADMcQB+AChxAH4BAnEAfgEDdAATZmFpbE9yRXhlY3V0ZUVuZ2luZXBwc3EA"
                + "fgAfcQB+AP5zcQB+ACYBAAAArHEAfgAocQB+AQJxAH4BA3EAfgCYcHBzcQB+AB9xAH4A/nNxAH4AJgEAAABlcQB+"
                + "AChxAH4BAnEAfgEDcQB+AJhwcHNxAH4AH3EAfgD+c3EAfgAmAQAAAEBxAH4AKHEAfgECcQB+AQN0ABBsYW1iZGEk"
                + "ZXhlY3V0ZSQwcHBzcQB+AB9xAH4A/nNxAH4AJgEAAACWcQB+AChxAH4BAnEAfgEDdAAWd2l0aEludGVyY2VwdGVk"
                + "U3RyZWFtc3Bwc3EAfgAfcQB+AP5zcQB+ACYBAAAAP3EAfgAocQB+AQJxAH4BA3EAfgCYcHBzcQB+AB9zcQB+ACIA"
                + "dAAianVuaXQtcGxhdGZvcm0tbGF1bmNoZXItMS4xNC40LmphcnEAfgEAc3EAfgAmAQAAAG1xAH4AKHQAMG9yZy5q"
                + "dW5pdC5wbGF0Zm9ybS5sYXVuY2hlci5jb3JlLkRlZmF1bHRMYXVuY2hlcnQAFERlZmF1bHRMYXVuY2hlci5qYXZh"
                + "cQB+AJhwcHNxAH4AH3EAfgEVc3EAfgAmAQAAAFtxAH4AKHEAfgEYcQB+ARlxAH4AmHBwc3EAfgAfc3EAfgAiAHQA"
                + "Imp1bml0LXBsYXRmb3JtLWxhdW5jaGVyLTEuMTQuNC5qYXJxAH4BAHNxAH4AJgEAAAAvcQB+ACh0ADNvcmcuanVu"
                + "aXQucGxhdGZvcm0ubGF1bmNoZXIuY29yZS5EZWxlZ2F0aW5nTGF1bmNoZXJ0ABdEZWxlZ2F0aW5nTGF1bmNoZXIu"
                + "amF2YXEAfgCYcHBzcQB+AB9zcQB+ACIAdAAianVuaXQtcGxhdGZvcm0tbGF1bmNoZXItMS4xNC40LmphcnEAfgEA"
                + "c3EAfgAmAQAAACdxAH4AKHQANW9yZy5qdW5pdC5wbGF0Zm9ybS5sYXVuY2hlci5jb3JlLkludGVyY2VwdGluZ0xh"
                + "dW5jaGVydAAZSW50ZXJjZXB0aW5nTGF1bmNoZXIuamF2YXQAEGxhbWJkYSRleGVjdXRlJDFwcHNxAH4AH3NxAH4A"
                + "IgF0ACJqdW5pdC1wbGF0Zm9ybS1sYXVuY2hlci0xLjE0LjQuamFycQB+AQBzcQB+ACYBAAAAGXEAfgAodABOb3Jn"
                + "Lmp1bml0LnBsYXRmb3JtLmxhdW5jaGVyLmNvcmUuQ2xhc3NwYXRoQWxpZ25tZW50Q2hlY2tpbmdMYXVuY2hlcklu"
                + "dGVyY2VwdG9ydAAyQ2xhc3NwYXRoQWxpZ25tZW50Q2hlY2tpbmdMYXVuY2hlckludGVyY2VwdG9yLmphdmFxAH4A"
                + "Y3Bwc3EAfgAfc3EAfgAiAXQAImp1bml0LXBsYXRmb3JtLWxhdW5jaGVyLTEuMTQuNC5qYXJxAH4BAHNxAH4AJgEA"
                + "AAAmcQB+AChxAH4BJnEAfgEncQB+AJhwcHNxAH4AH3NxAH4AIgF0ACJqdW5pdC1wbGF0Zm9ybS1sYXVuY2hlci0x"
                + "LjE0LjQuamFycQB+AQBzcQB+ACYBAAAAL3EAfgAocQB+ASBxAH4BIXEAfgCYcHBzcQB+AB9zcQB+ACIBdAAhc3Vy"
                + "ZWZpcmUtanVuaXQtcGxhdGZvcm0tMy41LjIuamFydAAFMy41LjJzcQB+ACYBAAAAOHEAfgAodAA0b3JnLmFwYWNo"
                + "ZS5tYXZlbi5zdXJlZmlyZS5qdW5pdHBsYXRmb3JtLkxhenlMYXVuY2hlcnQAEUxhenlMYXVuY2hlci5qYXZhcQB+"
                + "AJhwcHNxAH4AH3NxAH4AIgF0ACFzdXJlZmlyZS1qdW5pdC1wbGF0Zm9ybS0zLjUuMi5qYXJxAH4BOnNxAH4AJgEA"
                + "AAC4cQB+ACh0AD1vcmcuYXBhY2hlLm1hdmVuLnN1cmVmaXJlLmp1bml0cGxhdGZvcm0uSlVuaXRQbGF0Zm9ybVBy"
                + "b3ZpZGVydAAaSlVuaXRQbGF0Zm9ybVByb3ZpZGVyLmphdmFxAH4AmHBwc3EAfgAfc3EAfgAiAXQAIXN1cmVmaXJl"
                + "LWp1bml0LXBsYXRmb3JtLTMuNS4yLmphcnEAfgE6c3EAfgAmAQAAAJRxAH4AKHEAfgFCcQB+AUN0AA5pbnZva2VB"
                + "bGxUZXN0c3Bwc3EAfgAfc3EAfgAiAXQAIXN1cmVmaXJlLWp1bml0LXBsYXRmb3JtLTMuNS4yLmphcnEAfgE6c3EA"
                + "fgAmAQAAAHhxAH4AKHEAfgFCcQB+AUNxAH4ANnBwc3EAfgAfc3EAfgAiAXQAGXN1cmVmaXJlLWJvb3Rlci0zLjUu"
                + "Mi5qYXJ0AAUzLjUuMnNxAH4AJgEAAAGBcQB+ACh0AC1vcmcuYXBhY2hlLm1hdmVuLnN1cmVmaXJlLmJvb3Rlci5G"
                + "b3JrZWRCb290ZXJ0ABFGb3JrZWRCb290ZXIuamF2YXQAEnJ1blN1aXRlc0luUHJvY2Vzc3Bwc3EAfgAfc3EAfgAi"
                + "AXQAGXN1cmVmaXJlLWJvb3Rlci0zLjUuMi5qYXJxAH4BUHNxAH4AJgEAAACicQB+AChxAH4BUnEAfgFTcQB+AJhw"
                + "cHNxAH4AH3NxAH4AIgF0ABlzdXJlZmlyZS1ib290ZXItMy41LjIuamFycQB+AVBzcQB+ACYBAAAB+3EAfgAocQB+"
                + "AVJxAH4BU3QAA3J1bnBwc3EAfgAfc3EAfgAiAXQAGXN1cmVmaXJlLWJvb3Rlci0zLjUuMi5qYXJxAH4BUHNxAH4A"
                + "JgEAAAHvcQB+AChxAH4BUnEAfgFTdAAEbWFpbnBwdAAWT01HIEkndmUgYmVlbiBkZWxldGVkIXEAfgFjdABFb3Jn"
                + "LmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5Mb2c0akxvZ0V2ZW50VGVzdCREZWxldGVkRXhjZXB0aW9u"
                + "dXIANFtMb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5UaHJvd2FibGVQcm94eTv67QHghaLrOQIA"
                + "AHhwAAAAAHEAfgAVcQB+ABV1cgACW0Ks8xf4BghU4AIAAHhwAAAAaaztAAVzcgAub3JnLmFwYWNoZS5sb2dnaW5n"
                + "LmxvZzRqLm1lc3NhZ2UuU2ltcGxlTWVzc2FnZYt0TTBgt6KoAwABTAAHbWVzc2FnZXQAEkxqYXZhL2xhbmcvU3Ry"
                + "aW5nO3hwdAADYWJjeHg=";

        final byte[] binaryDecoded = Base64Converter.parseBase64Binary(base64);
        final Log4jLogEvent evt2 = deserialize(binaryDecoded);

        assertEquals(loggerFQN, evt2.getLoggerFqcn());
        assertEquals(level, evt2.getLevel());
        assertEquals(loggerName, evt2.getLoggerName());
        assertEquals(marker, evt2.getMarker());
        assertEquals(msg, evt2.getMessage());
        assertEquals(threadName, evt2.getThreadName());
        assertNull(evt2.getThrown());
        assertEquals(
                this.getClass().getName() + "$DeletedException",
                evt2.getThrownProxy().getName());
        assertEquals(errorMessage, evt2.getThrownProxy().getMessage());
    }

    // DO NOT REMOVE THIS COMMENT:
    // UNCOMMENT WHEN GENERATING SERIALIZED EVENT FOR #testJavaIoSerializableWithUnknownMessage
    // public static class DeletedMessage implements Message {
    // private static final long serialVersionUID = 1L;
    // public String getFormattedMessage() { return "abc"; }
    // public String getFormat() { return "abc"; }
    // public Object[] getParameters() { return null; }
    // public Throwable getThrowable() { return null; }
    // };

    /**
     * The class of the message is allowed by the deserialization filter, but not available to the reader:
     * the message degrades to a {@link SimpleMessage} with the formatted message.
     */
    @Test
    @Tag(Tags.SERIALIZATION)
    void testJavaIoSerializableWithUnknownMessage() {
        final String loggerName = "some.test";
        final Level level = Level.INFO;

        // DO NOT DELETE THIS COMMENT:
        // UNCOMMENT TO RE-GENERATE SERIALIZED EVENT WHEN UPDATING THIS TEST.
        // final Log4jLogEvent evt = Log4jLogEvent.newBuilder()
        //         .setLoggerName(loggerName)
        //         .setLevel(level)
        //         .setMessage(new DeletedMessage())
        //         .build();
        // final byte[] binary = serialize(evt);
        // final String base64Str = java.util.Base64.getEncoder().encodeToString(binary);
        // System.out.println("final String base64 = \"" + base64Str.replaceAll(".{88}", "$0\" + \"") + "\";");

        final String base64 = "rO0ABXNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnQkTG9nRXZlbnRQ"
                + "cm94eYgtmn+yXsP9AwATWgAMaXNFbmRPZkJhdGNoWgASaXNMb2NhdGlvblJlcXVpcmVkSQARbmFub09mTWlsbGlz"
                + "ZWNvbmRKAAh0aHJlYWRJZEkADnRocmVhZFByaW9yaXR5SgAKdGltZU1pbGxpc0wAC2NvbnRleHREYXRhdAApTG9y"
                + "Zy9hcGFjaGUvbG9nZ2luZy9sb2c0ai91dGlsL1N0cmluZ01hcDtMAAxjb250ZXh0U3RhY2t0ADVMb3JnL2FwYWNo"
                + "ZS9sb2dnaW5nL2xvZzRqL1RocmVhZENvbnRleHQkQ29udGV4dFN0YWNrO0wABWxldmVsdAAgTG9yZy9hcGFjaGUv"
                + "bG9nZ2luZy9sb2c0ai9MZXZlbDtMAApsb2dnZXJGUUNOdAASTGphdmEvbGFuZy9TdHJpbmc7TAAKbG9nZ2VyTmFt"
                + "ZXEAfgAETAAGbWFya2VydAAhTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9NYXJrZXI7TAANbWVzc2FnZVN0cmlu"
                + "Z3EAfgAETAAGc291cmNldAAdTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDtMAAZzcGFuSWRxAH4ABEwACnRo"
                + "cmVhZE5hbWVxAH4ABEwAC3Rocm93blByb3h5dAAzTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwv"
                + "VGhyb3dhYmxlUHJveHk7TAAKdHJhY2VGbGFnc3EAfgAETAAHdHJhY2VJZHEAfgAEeHAAAAAAAAAAAAAAAAAAAQAA"
                + "AAUAAAAASZYC0nNyADJvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGoudXRpbC5Tb3J0ZWRBcnJheVN0cmluZ01hcLA3"
                + "yJFz7CvcAwACWgAJaW1tdXRhYmxlSQAJdGhyZXNob2xkeHABAAAAAXcIAAAAAQAAAAB4c3IAPm9yZy5hcGFjaGUu"
                + "bG9nZ2luZy5sb2c0ai5UaHJlYWRDb250ZXh0JEVtcHR5VGhyZWFkQ29udGV4dFN0YWNrAAAAAAAAAAECAAB4cHNy"
                + "AB5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouTGV2ZWwAAAAAABggGgIAA0kACGludExldmVsTAAEbmFtZXEAfgAE"
                + "TAANc3RhbmRhcmRMZXZlbHQALExvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovc3BpL1N0YW5kYXJkTGV2ZWw7eHAA"
                + "AAGQdAAESU5GT35yACpvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouc3BpLlN0YW5kYXJkTGV2ZWwAAAAAAAAAABIA"
                + "AHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AARJTkZPcHQACXNvbWUudGVzdHB0AANhYmNwdAAAdAAE"
                + "bWFpbnBxAH4AF3EAfgAXdXIAAltCrPMX+AYIVOACAAB4cAAAAFis7QAFc3IAQ29yZy5hcGFjaGUubG9nZ2luZy5s"
                + "b2c0ai5jb3JlLmltcGwuTG9nNGpMb2dFdmVudFRlc3QkRGVsZXRlZE1lc3NhZ2UAAAAAAAAAAQIAAHhweA==";

        final Log4jLogEvent evt2 = deserialize(Base64Converter.parseBase64Binary(base64));

        assertEquals(loggerName, evt2.getLoggerName());
        assertEquals(level, evt2.getLevel());
        assertEquals(new SimpleMessage("abc"), evt2.getMessage());
    }

    @Test
    void testNullLevelReplacedWithOFF() {
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt =
                Log4jLogEvent.newBuilder().setLevel(NULL_LEVEL).build();
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    void testTimestampGeneratedByClock() {
        final LogEvent evt = Log4jLogEvent.newBuilder().build();
        assertEquals(FixedTimeClock.FIXED_TIME, evt.getTimeMillis());
    }

    @Test
    void testInitiallyDummyNanoClock() {
        assertInstanceOf(DummyNanoClock.class, Log4jLogEvent.getNanoClock());
        assertEquals(0, Log4jLogEvent.getNanoClock().nanoTime(), "initial dummy nanotime");
    }

    @Test
    void testNanoTimeGeneratedByNanoClock() {
        Log4jLogEvent.setNanoClock(new DummyNanoClock(123));
        verifyNanoTimeWithAllConstructors(123);
        Log4jLogEvent.setNanoClock(new DummyNanoClock(87654));
        verifyNanoTimeWithAllConstructors(87654);
    }

    @SuppressWarnings("deprecation")
    private void verifyNanoTimeWithAllConstructors(final long expected) {
        assertEquals(expected, Log4jLogEvent.getNanoClock().nanoTime());

        assertEquals(expected, new Log4jLogEvent().getNanoTime(), "No-arg constructor");
        assertEquals(expected, new Log4jLogEvent(98).getNanoTime(), "1-arg constructor");
        assertEquals(expected, new Log4jLogEvent("l", null, "a", null, null, null).getNanoTime(), "6-arg constructor");
        assertEquals(
                expected, new Log4jLogEvent("l", null, "a", null, null, null, null).getNanoTime(), "7-arg constructor");
        assertEquals(
                expected,
                new Log4jLogEvent("l", null, "a", null, null, null, null, null, null, null, 0).getNanoTime(),
                "11-arg constructor");
        assertEquals(
                expected,
                Log4jLogEvent.createEvent("l", null, "a", null, null, null, null, null, null, null, null, 0)
                        .getNanoTime(),
                "12-arg factory method");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testBuilderCorrectlyCopiesAllEventAttributes() {
        final StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn(fqcn) //
                .setLoggerName(name) //
                .setMarker(marker) //
                .setMessage(message) //
                .setNanoTime(1234567890L) //
                .setSource(stackTraceElement) //
                .setThreadName(threadName) //
                .setThrown(exception) //
                .setTimeMillis(987654321L)
                .build();

        assertEquals(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        assertSame(stackTraceElement, event.getSource());
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(event2, event, "copy constructor builder");
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");
    }

    @Test
    void testBuilderCorrectlyCopiesAllEventAttributesInclContextData() {
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn(fqcn) //
                .setLoggerName(name) //
                .setMarker(marker) //
                .setMessage(message) //
                .setNanoTime(1234567890L) //
                .setSource(stackTraceElement) //
                .setThreadName(threadName) //
                .setThrown(exception) //
                .setTimeMillis(987654321L)
                .build();

        assertSame(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        assertSame(stackTraceElement, event.getSource());
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(event2, event, "copy constructor builder");
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");
    }

    @Test
    void testBuilderCorrectlyCopiesMutableLogEvent() throws Exception {
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final MutableLogEvent event = new MutableLogEvent();
        event.setContextData(contextData);
        event.setContextStack(contextStack);
        event.setEndOfBatch(true);
        event.setIncludeLocation(true);
        // event.setSource(stackTraceElement); // cannot be explicitly set
        event.setLevel(Level.FATAL);
        event.setLoggerFqcn(fqcn);
        event.setLoggerName(name);
        event.setMarker(marker);
        event.setMessage(message);
        event.setNanoTime(1234567890L);
        event.setThreadName(threadName);
        event.setThrown(exception);
        event.setTimeMillis(987654321L);

        assertSame(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        // assertSame(stackTraceElement, event.getSource()); // don't invoke
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent e2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(contextData, e2.getContextData());
        assertSame(contextStack, e2.getContextStack());
        assertTrue(e2.isEndOfBatch());
        assertTrue(e2.isIncludeLocation());
        assertSame(Level.FATAL, e2.getLevel());
        assertSame(fqcn, e2.getLoggerFqcn());
        assertSame(name, e2.getLoggerName());
        assertSame(marker, e2.getMarker());
        assertSame(message, e2.getMessage());
        assertEquals(1234567890L, e2.getNanoTime());
        // assertSame(stackTraceElement, e2.getSource()); // don't invoke
        assertSame(threadName, e2.getThreadName());
        assertSame(exception, e2.getThrown());
        assertEquals(987654321L, e2.getTimeMillis());

        // use reflection to get value of source field in log event copy:
        // invoking the getSource() method would initialize the field
        final Field fieldSource = Log4jLogEvent.class.getDeclaredField("source");
        fieldSource.setAccessible(true);
        final Object value = fieldSource.get(e2);
        assertNull(value, "source in copy");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testEquals() {
        final StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("A", "B");
        ThreadContext.push("first");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextData(contextData) //
                .setContextStack(contextStack) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn(fqcn) //
                .setLoggerName(name) //
                .setMarker(marker) //
                .setMessage(message) //
                .setNanoTime(1234567890L) //
                .setSource(stackTraceElement) //
                .setThreadName(threadName) //
                .setThrown(exception) //
                .setTimeMillis(987654321L)
                .build();

        assertEquals(contextData, event.getContextData());
        assertSame(contextStack, event.getContextStack());
        assertTrue(event.isEndOfBatch());
        assertTrue(event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        assertSame(stackTraceElement, event.getSource());
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent event2 = builder(event).build();
        assertEquals(event2, event, "copy constructor builder");
        assertEquals(event2.hashCode(), event.hashCode(), "same hashCode");

        assertEquals(contextData, event2.getContextData());
        assertSame(contextStack, event2.getContextStack());
        assertTrue(event2.isEndOfBatch());
        assertTrue(event2.isIncludeLocation());
        assertSame(Level.FATAL, event2.getLevel());
        assertSame(fqcn, event2.getLoggerFqcn());
        assertSame(name, event2.getLoggerName());
        assertSame(marker, event2.getMarker());
        assertSame(message, event2.getMessage());
        assertEquals(1234567890L, event2.getNanoTime());
        assertSame(stackTraceElement, event2.getSource());
        assertSame(threadName, event2.getThreadName());
        assertSame(exception, event2.getThrown());
        assertEquals(987654321L, event2.getTimeMillis());

        final StringMap differentMap = ContextDataFactory.emptyFrozenContextData();
        different("different contextMap", builder(event).setContextData(differentMap), event);
        different("null contextMap", builder(event).setContextData(null), event);

        ThreadContext.push("abc");
        final ContextStack contextStack2 = ThreadContext.getImmutableStack();
        different("different contextStack", builder(event).setContextStack(contextStack2), event);
        different("null contextStack", builder(event).setContextStack(null), event);

        different("different EndOfBatch", builder(event).setEndOfBatch(false), event);
        different("different IncludeLocation", builder(event).setIncludeLocation(false), event);

        different("different level", builder(event).setLevel(Level.INFO), event);
        different("null level", builder(event).setLevel(null), event);

        different("different fqcn", builder(event).setLoggerFqcn("different"), event);
        different("null fqcn", builder(event).setLoggerFqcn(null), event);

        different("different name", builder(event).setLoggerName("different"), event);
        assertThrows(
                NullPointerException.class,
                () -> different("null name", builder(event).setLoggerName(null), event));

        different("different marker", builder(event).setMarker(MarkerManager.getMarker("different")), event);
        different("null marker", builder(event).setMarker(null), event);

        different("different message", builder(event).setMessage(new ObjectMessage("different")), event);
        assertThrows(
                NullPointerException.class,
                () -> different("null message", builder(event).setMessage(null), event));

        different("different nanoTime", builder(event).setNanoTime(135), event);
        different("different milliTime", builder(event).setTimeMillis(137), event);

        final StackTraceElement stack2 = new StackTraceElement("XXX", "YYY", "file", 123);
        different("different source", builder(event).setSource(stack2), event);
        different("null source", builder(event).setSource(null), event);

        different("different threadname", builder(event).setThreadName("different"), event);
        different("null threadname", builder(event).setThreadName(null), event);

        different("different exception", builder(event).setThrown(new Error("Boo!")), event);
        different("null exception", builder(event).setThrown(null), event);
    }

    private static Log4jLogEvent.Builder builder(final LogEvent event) {
        return new Log4jLogEvent.Builder(event);
    }

    private void different(final String reason, final Log4jLogEvent.Builder builder, final LogEvent event) {
        final LogEvent other = builder.build();
        assertNotEquals(other, event, reason);
        assertNotEquals(other.hashCode(), event.hashCode(), reason + " hashCode");
    }

    @Test
    void testToString() {
        // Throws an NPE in 2.6.2
        assertNotNull(new Log4jLogEvent().toString());
    }

    @Test
    public void testCustomLegacyLogEventDefaultBehavior() {
        // Create an anonymous/stub class implementing LogEvent without implementing getTraceId/getSpanId/getTraceFlags
        final LogEvent legacyEvent = new LogEvent() {
            private static final long serialVersionUID = 1L;

            @Override
            public LogEvent toImmutable() {
                return this;
            }

            @Override
            @Deprecated
            public Map<String, String> getContextMap() {
                return java.util.Collections.emptyMap();
            }

            @Override
            public ReadOnlyStringMap getContextData() {
                return ContextDataFactory.emptyFrozenContextData();
            }

            @Override
            public ThreadContext.ContextStack getContextStack() {
                return ThreadContext.EMPTY_STACK;
            }

            @Override
            public String getLoggerFqcn() {
                return null;
            }

            @Override
            public Level getLevel() {
                return Level.INFO;
            }

            @Override
            public String getLoggerName() {
                return "LegacyLogger";
            }

            @Override
            public Marker getMarker() {
                return null;
            }

            @Override
            public Message getMessage() {
                return new SimpleMessage("Legacy msg");
            }

            @Override
            public long getTimeMillis() {
                return 0;
            }

            @Override
            public Instant getInstant() {
                return new MutableInstant();
            }

            @Override
            public StackTraceElement getSource() {
                return null;
            }

            @Override
            public String getThreadName() {
                return "main";
            }

            @Override
            public long getThreadId() {
                return 1;
            }

            @Override
            public int getThreadPriority() {
                return 5;
            }

            @Override
            public Throwable getThrown() {
                return null;
            }

            @Override
            @Deprecated
            public ThrowableProxy getThrownProxy() {
                return null;
            }

            @Override
            public boolean isEndOfBatch() {
                return false;
            }

            @Override
            public boolean isIncludeLocation() {
                return false;
            }

            @Override
            public void setEndOfBatch(boolean endOfBatch) {}

            @Override
            public void setIncludeLocation(boolean locationRequired) {}

            @Override
            public long getNanoTime() {
                return 0;
            }
        };

        assertNull(legacyEvent.getTraceId());
        assertNull(legacyEvent.getSpanId());
        assertNull(legacyEvent.getTraceFlags());
    }

    @Test
    void testTracingFieldsInBuilderAndCopy() {
        final Log4jLogEvent originalEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("TestLogger")
                .setLevel(Level.DEBUG)
                .setMessage(new org.apache.logging.log4j.message.SimpleMessage("Test message"))
                .setTraceId("4bf92f3577b34da6a3ce929d0e0e4736")
                .setSpanId("00f067aa0ba902b7")
                .setTraceFlags("01")
                .build();

        assertThat(originalEvent.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(originalEvent.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(originalEvent.getTraceFlags()).isEqualTo("01");

        final Log4jLogEvent copiedEvent = new Log4jLogEvent.Builder(originalEvent).build();
        assertThat(copiedEvent.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(copiedEvent.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(copiedEvent.getTraceFlags()).isEqualTo("01");
    }

    @Test
    @Tag(Tags.SERIALIZATION)
    void testTracingFieldsSerialization() throws Exception {
        final Log4jLogEvent originalEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("SerializationLogger")
                .setMessage(new org.apache.logging.log4j.message.SimpleMessage("dummy message"))
                .setTraceId("trace-serialize-123")
                .setSpanId("span-serialize-456")
                .setTraceFlags("01")
                .build();

        final byte[] serializedBytes = serialize(originalEvent);

        final Log4jLogEvent deserializedEvent = deserialize(serializedBytes);

        assertThat(deserializedEvent.getTraceId()).isEqualTo("trace-serialize-123");
        assertThat(deserializedEvent.getSpanId()).isEqualTo("span-serialize-456");
        assertThat(deserializedEvent.getTraceFlags()).isEqualTo("01");
    }
}
