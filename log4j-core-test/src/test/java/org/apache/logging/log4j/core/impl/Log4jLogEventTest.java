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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.convert.Base64Converter;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.ClockFactoryTest;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
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
    @Tag("serialization")
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
    @Tag("serialization")
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
    @Tag("serialization")
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
    @Tag("serialization")
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
    @Tag("serialization")
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
                + "AAUAAAAASZYC0nNyADJvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGoudXRpbC5Tb3J0ZWRBcnJheVN0cmluZ01hcM80"
                + "Cd6qXjcaAwAEWgAJaW1tdXRhYmxlSQAEc2l6ZUkACXRocmVzaG9sZFsABGtleXN0ABNbTGphdmEvbGFuZy9TdHJp"
                + "bmc7eHABAAAAAAAAAAF1cgATW0xqYXZhLmxhbmcuU3RyaW5nO63SVufpHXtHAgAAeHAAAAAAdXIAA1tbQkv9GRVn"
                + "Z9s3AgAAeHAAAAAAeHNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouVGhyZWFkQ29udGV4dCRFbXB0eVRocmVh"
                + "ZENvbnRleHRTdGFjawAAAAAAAAABAgAAeHBzcgAeb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLkxldmVsAAAAAAAY"
                + "IBoCAANJAAhpbnRMZXZlbEwABG5hbWVxAH4ABEwADXN0YW5kYXJkTGV2ZWx0ACxMb3JnL2FwYWNoZS9sb2dnaW5n"
                + "L2xvZzRqL3NwaS9TdGFuZGFyZExldmVsO3hwAAABkHQABElORk9+cgAqb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRq"
                + "LnNwaS5TdGFuZGFyZExldmVsAAAAAAAAAAASAAB4cgAOamF2YS5sYW5nLkVudW0AAAAAAAAAABIAAHhwdAAESU5G"
                + "T3QAAHQACXNvbWUudGVzdHB0AANhYmNwcQB+ABp0AARtYWluc3IAMW9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5j"
                + "b3JlLmltcGwuVGhyb3dhYmxlUHJveHnZzDDVmnus+gIAB0kAEmNvbW1vbkVsZW1lbnRDb3VudEwACmNhdXNlUHJv"
                + "eHlxAH4AB1sAEmV4dGVuZGVkU3RhY2tUcmFjZXQAP1tMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1w"
                + "bC9FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW50O0wAEGxvY2FsaXplZE1lc3NhZ2VxAH4ABEwAB21lc3NhZ2VxAH4A"
                + "BEwABG5hbWVxAH4ABFsAEXN1cHByZXNzZWRQcm94aWVzdAA0W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29y"
                + "ZS9pbXBsL1Rocm93YWJsZVByb3h5O3hwAAAAAHB1cgA/W0xvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5p"
                + "bXBsLkV4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7ys+II6XHz7wCAAB4cAAAAE5zcgA8b3JnLmFwYWNoZS5sb2dn"
                + "aW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW504d7Pusa2kAcCAAJMAA5leHRyYUNs"
                + "YXNzSW5mb3QANkxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkQ2xhc3NJbmZvO0wA"
                + "EXN0YWNrVHJhY2VFbGVtZW50cQB+AAZ4cHNyADRvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4"
                + "dGVuZGVkQ2xhc3NJbmZvAAAAAAAAAAECAANaAAVleGFjdEwACGxvY2F0aW9ucQB+AARMAAd2ZXJzaW9ucQB+AAR4"
                + "cAB0AA10ZXN0LWNsYXNzZXMvdAABP3NyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRhCcWaJjbdhQIACEIA"
                + "BmZvcm1hdEkACmxpbmVOdW1iZXJMAA9jbGFzc0xvYWRlck5hbWVxAH4ABEwADmRlY2xhcmluZ0NsYXNzcQB+AARM"
                + "AAhmaWxlTmFtZXEAfgAETAAKbWV0aG9kTmFtZXEAfgAETAAKbW9kdWxlTmFtZXEAfgAETAANbW9kdWxlVmVyc2lv"
                + "bnEAfgAEeHABAAAA3nQAA2FwcHQANG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuTG9nNGpMb2dF"
                + "dmVudFRlc3R0ABZMb2c0akxvZ0V2ZW50VGVzdC5qYXZhdAAqdGVzdEphdmFJb1NlcmlhbGl6YWJsZVdpdGhVbmtu"
                + "b3duVGhyb3dhYmxlcHBzcQB+ACRzcQB+ACcAcQB+ACpxAH4AKnNxAH4AKwL////+cHQALWpkay5pbnRlcm5hbC5y"
                + "ZWZsZWN0Lk5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbHQAHU5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbC5qYXZhdAAH"
                + "aW52b2tlMHQACWphdmEuYmFzZXQACTE3LjAuMjAuMXNxAH4AJHEAfgAyc3EAfgArAgAAAE1wcQB+ADRxAH4ANXQA"
                + "Bmludm9rZXEAfgA3cQB+ADhzcQB+ACRzcQB+ACcAcQB+ACpxAH4AKnNxAH4AKwIAAAArcHQAMWpkay5pbnRlcm5h"
                + "bC5yZWZsZWN0LkRlbGVnYXRpbmdNZXRob2RBY2Nlc3NvckltcGx0ACFEZWxlZ2F0aW5nTWV0aG9kQWNjZXNzb3JJ"
                + "bXBsLmphdmFxAH4AO3EAfgA3cQB+ADhzcQB+ACRzcQB+ACcAcQB+ACpxAH4AKnNxAH4AKwIAAAI5cHQAGGphdmEu"
                + "bGFuZy5yZWZsZWN0Lk1ldGhvZHQAC01ldGhvZC5qYXZhcQB+ADtxAH4AN3EAfgA4c3EAfgAkc3EAfgAnAHQAIWp1"
                + "bml0LXBsYXRmb3JtLWNvbW1vbnMtMS4xNC40LmphcnQABjEuMTQuNHNxAH4AKwEAAAMScQB+AC10AC9vcmcuanVu"
                + "aXQucGxhdGZvcm0uY29tbW9ucy51dGlsLlJlZmxlY3Rpb25VdGlsc3QAFFJlZmxlY3Rpb25VdGlscy5qYXZhdAAM"
                + "aW52b2tlTWV0aG9kcHBzcQB+ACRzcQB+ACcAdAAhanVuaXQtcGxhdGZvcm0tY29tbW9ucy0xLjE0LjQuamFycQB+"
                + "AElzcQB+ACsBAAACAnEAfgAtdAA0b3JnLmp1bml0LnBsYXRmb3JtLmNvbW1vbnMuc3VwcG9ydC5SZWZsZWN0aW9u"
                + "U3VwcG9ydHQAFlJlZmxlY3Rpb25TdXBwb3J0LmphdmFxAH4ATXBwc3EAfgAkc3EAfgAnAHQAH2p1bml0LWp1cGl0"
                + "ZXItZW5naW5lLTUuMTQuNC5qYXJ0AAY1LjE0LjRzcQB+ACsBAAAAPHEAfgAtdAAzb3JnLmp1bml0Lmp1cGl0ZXIu"
                + "ZW5naW5lLmV4ZWN1dGlvbi5NZXRob2RJbnZvY2F0aW9udAAVTWV0aG9kSW52b2NhdGlvbi5qYXZhdAAHcHJvY2Vl"
                + "ZHBwc3EAfgAkc3EAfgAnAHQAH2p1bml0LWp1cGl0ZXItZW5naW5lLTUuMTQuNC5qYXJxAH4AV3NxAH4AKwEAAACD"
                + "cQB+AC10AFJvcmcuanVuaXQuanVwaXRlci5lbmdpbmUuZXhlY3V0aW9uLkludm9jYXRpb25JbnRlcmNlcHRvckNo"
                + "YWluJFZhbGlkYXRpbmdJbnZvY2F0aW9udAAfSW52b2NhdGlvbkludGVyY2VwdG9yQ2hhaW4uamF2YXEAfgBbcHBz"
                + "cQB+ACRzcQB+ACcAdAAfanVuaXQtanVwaXRlci1lbmdpbmUtNS4xNC40LmphcnEAfgBXc3EAfgArAQAAAKFxAH4A"
                + "LXQAM29yZy5qdW5pdC5qdXBpdGVyLmVuZ2luZS5leHRlbnNpb24uVGltZW91dEV4dGVuc2lvbnQAFVRpbWVvdXRF"
                + "eHRlbnNpb24uamF2YXQACWludGVyY2VwdHBwc3EAfgAkcQB+AGNzcQB+ACsBAAAAmHEAfgAtcQB+AGZxAH4AZ3QA"
                + "F2ludGVyY2VwdFRlc3RhYmxlTWV0aG9kcHBzcQB+ACRxAH4AY3NxAH4AKwEAAABbcQB+AC1xAH4AZnEAfgBndAAT"
                + "aW50ZXJjZXB0VGVzdE1ldGhvZHBwc3EAfgAkc3EAfgAnAHQAH2p1bml0LWp1cGl0ZXItZW5naW5lLTUuMTQuNC5q"
                + "YXJxAH4AV3NxAH4AKwEAAABwcQB+AC10AFpvcmcuanVuaXQuanVwaXRlci5lbmdpbmUuZXhlY3V0aW9uLkludGVy"
                + "Y2VwdGluZ0V4ZWN1dGFibGVJbnZva2VyJFJlZmxlY3RpdmVJbnRlcmNlcHRvckNhbGx0ACJJbnRlcmNlcHRpbmdF"
                + "eGVjdXRhYmxlSW52b2tlci5qYXZhdAAVbGFtYmRhJG9mVm9pZE1ldGhvZCQwcHBzcQB+ACRzcQB+ACcAdAAfanVu"
                + "aXQtanVwaXRlci1lbmdpbmUtNS4xNC40LmphcnEAfgBXc3EAfgArAQAAAF5xAH4ALXQAQG9yZy5qdW5pdC5qdXBp"
                + "dGVyLmVuZ2luZS5leGVjdXRpb24uSW50ZXJjZXB0aW5nRXhlY3V0YWJsZUludm9rZXJxAH4AdHQAD2xhbWJkYSRp"
                + "bnZva2UkMHBwc3EAfgAkc3EAfgAnAHQAH2p1bml0LWp1cGl0ZXItZW5naW5lLTUuMTQuNC5qYXJxAH4AV3NxAH4A"
                + "KwEAAABqcQB+AC10AFNvcmcuanVuaXQuanVwaXRlci5lbmdpbmUuZXhlY3V0aW9uLkludm9jYXRpb25JbnRlcmNl"
                + "cHRvckNoYWluJEludGVyY2VwdGVkSW52b2NhdGlvbnEAfgBhcQB+AFtwcHNxAH4AJHNxAH4AJwB0AB9qdW5pdC1q"
                + "dXBpdGVyLWVuZ2luZS01LjE0LjQuamFycQB+AFdzcQB+ACsBAAAAQHEAfgAtdAA9b3JnLmp1bml0Lmp1cGl0ZXIu"
                + "ZW5naW5lLmV4ZWN1dGlvbi5JbnZvY2F0aW9uSW50ZXJjZXB0b3JDaGFpbnEAfgBhcQB+AFtwcHNxAH4AJHEAfgCC"
                + "c3EAfgArAQAAAC1xAH4ALXEAfgCFcQB+AGF0AA5jaGFpbkFuZEludm9rZXBwc3EAfgAkcQB+AIJzcQB+ACsBAAAA"
                + "JXEAfgAtcQB+AIVxAH4AYXEAfgA7cHBzcQB+ACRxAH4Ad3NxAH4AKwEAAABdcQB+AC1xAH4AenEAfgB0cQB+ADtw"
                + "cHNxAH4AJHEAfgB3c3EAfgArAQAAAFdxAH4ALXEAfgB6cQB+AHRxAH4AO3Bwc3EAfgAkc3EAfgAnAHQAH2p1bml0"
                + "LWp1cGl0ZXItZW5naW5lLTUuMTQuNC5qYXJxAH4AV3NxAH4AKwEAAADdcQB+AC10ADxvcmcuanVuaXQuanVwaXRl"
                + "ci5lbmdpbmUuZGVzY3JpcHRvci5UZXN0TWV0aG9kVGVzdERlc2NyaXB0b3J0AB1UZXN0TWV0aG9kVGVzdERlc2Ny"
                + "aXB0b3IuamF2YXQAGWxhbWJkYSRpbnZva2VUZXN0TWV0aG9kJDRwcHNxAH4AJHNxAH4AJwB0ACBqdW5pdC1wbGF0"
                + "Zm9ybS1lbmdpbmUtMS4xNC40LmphcnQABjEuMTQuNHNxAH4AKwEAAABJcQB+AC10AEFvcmcuanVuaXQucGxhdGZv"
                + "cm0uZW5naW5lLnN1cHBvcnQuaGllcmFyY2hpY2FsLlRocm93YWJsZUNvbGxlY3RvcnQAF1Rocm93YWJsZUNvbGxl"
                + "Y3Rvci5qYXZhdAAHZXhlY3V0ZXBwc3EAfgAkcQB+AJBzcQB+ACsBAAAA2XEAfgAtcQB+AJNxAH4AlHQAEGludm9r"
                + "ZVRlc3RNZXRob2RwcHNxAH4AJHEAfgCQc3EAfgArAQAAAJ9xAH4ALXEAfgCTcQB+AJRxAH4AnXBwc3EAfgAkcQB+"
                + "AJBzcQB+ACsBAAAARnEAfgAtcQB+AJNxAH4AlHEAfgCdcHBzcQB+ACRzcQB+ACcAdAAganVuaXQtcGxhdGZvcm0t"
                + "ZW5naW5lLTEuMTQuNC5qYXJxAH4AmXNxAH4AKwEAAACdcQB+AC10ADtvcmcuanVuaXQucGxhdGZvcm0uZW5naW5l"
                + "LnN1cHBvcnQuaGllcmFyY2hpY2FsLk5vZGVUZXN0VGFza3QAEU5vZGVUZXN0VGFzay5qYXZhdAAbbGFtYmRhJGV4"
                + "ZWN1dGVSZWN1cnNpdmVseSQ2cHBzcQB+ACRxAH4Al3NxAH4AKwEAAABJcQB+AC1xAH4Am3EAfgCccQB+AJ1wcHNx"
                + "AH4AJHEAfgCmc3EAfgArAQAAAJNxAH4ALXEAfgCpcQB+AKp0ABtsYW1iZGEkZXhlY3V0ZVJlY3Vyc2l2ZWx5JDhw"
                + "cHNxAH4AJHNxAH4AJwB0ACBqdW5pdC1wbGF0Zm9ybS1lbmdpbmUtMS4xNC40LmphcnEAfgCZc3EAfgArAQAAAIlx"
                + "AH4ALXQAM29yZy5qdW5pdC5wbGF0Zm9ybS5lbmdpbmUuc3VwcG9ydC5oaWVyYXJjaGljYWwuTm9kZXQACU5vZGUu"
                + "amF2YXQABmFyb3VuZHBwc3EAfgAkcQB+AKZzcQB+ACsBAAAAkXEAfgAtcQB+AKlxAH4AqnQAG2xhbWJkYSRleGVj"
                + "dXRlUmVjdXJzaXZlbHkkOXBwc3EAfgAkcQB+AJdzcQB+ACsBAAAASXEAfgAtcQB+AJtxAH4AnHEAfgCdcHBzcQB+"
                + "ACRxAH4ApnNxAH4AKwEAAACQcQB+AC1xAH4AqXEAfgCqdAASZXhlY3V0ZVJlY3Vyc2l2ZWx5cHBzcQB+ACRxAH4A"
                + "pnNxAH4AKwEAAABlcQB+AC1xAH4AqXEAfgCqcQB+AJ1wcHNxAH4AJHNxAH4AJwBxAH4AKnEAfgAqc3EAfgArAgAA"
                + "BedwdAATamF2YS51dGlsLkFycmF5TGlzdHQADkFycmF5TGlzdC5qYXZhdAAHZm9yRWFjaHEAfgA3cQB+ADhzcQB+"
                + "ACRzcQB+ACcAdAAganVuaXQtcGxhdGZvcm0tZW5naW5lLTEuMTQuNC5qYXJxAH4AmXNxAH4AKwEAAAApcQB+AC10"
                + "AFhvcmcuanVuaXQucGxhdGZvcm0uZW5naW5lLnN1cHBvcnQuaGllcmFyY2hpY2FsLlNhbWVUaHJlYWRIaWVyYXJj"
                + "aGljYWxUZXN0RXhlY3V0b3JTZXJ2aWNldAAuU2FtZVRocmVhZEhpZXJhcmNoaWNhbFRlc3RFeGVjdXRvclNlcnZp"
                + "Y2UuamF2YXQACWludm9rZUFsbHBwc3EAfgAkcQB+AKZzcQB+ACsBAAAAoXEAfgAtcQB+AKlxAH4AqnEAfgCrcHBz"
                + "cQB+ACRxAH4Al3NxAH4AKwEAAABJcQB+AC1xAH4Am3EAfgCccQB+AJ1wcHNxAH4AJHEAfgCmc3EAfgArAQAAAJNx"
                + "AH4ALXEAfgCpcQB+AKpxAH4AsHBwc3EAfgAkcQB+ALJzcQB+ACsBAAAAiXEAfgAtcQB+ALVxAH4AtnEAfgC3cHBz"
                + "cQB+ACRxAH4ApnNxAH4AKwEAAACRcQB+AC1xAH4AqXEAfgCqcQB+ALpwcHNxAH4AJHEAfgCXc3EAfgArAQAAAElx"
                + "AH4ALXEAfgCbcQB+AJxxAH4AnXBwc3EAfgAkcQB+AKZzcQB+ACsBAAAAkHEAfgAtcQB+AKlxAH4AqnEAfgC/cHBz"
                + "cQB+ACRxAH4ApnNxAH4AKwEAAABlcQB+AC1xAH4AqXEAfgCqcQB+AJ1wcHNxAH4AJHEAfgDDc3EAfgArAgAABedw"
                + "cQB+AMVxAH4AxnEAfgDHcQB+ADdxAH4AOHNxAH4AJHEAfgDJc3EAfgArAQAAAClxAH4ALXEAfgDMcQB+AM1xAH4A"
                + "znBwc3EAfgAkcQB+AKZzcQB+ACsBAAAAoXEAfgAtcQB+AKlxAH4AqnEAfgCrcHBzcQB+ACRxAH4Al3NxAH4AKwEA"
                + "AABJcQB+AC1xAH4Am3EAfgCccQB+AJ1wcHNxAH4AJHEAfgCmc3EAfgArAQAAAJNxAH4ALXEAfgCpcQB+AKpxAH4A"
                + "sHBwc3EAfgAkcQB+ALJzcQB+ACsBAAAAiXEAfgAtcQB+ALVxAH4AtnEAfgC3cHBzcQB+ACRxAH4ApnNxAH4AKwEA"
                + "AACRcQB+AC1xAH4AqXEAfgCqcQB+ALpwcHNxAH4AJHEAfgCXc3EAfgArAQAAAElxAH4ALXEAfgCbcQB+AJxxAH4A"
                + "nXBwc3EAfgAkcQB+AKZzcQB+ACsBAAAAkHEAfgAtcQB+AKlxAH4AqnEAfgC/cHBzcQB+ACRxAH4ApnNxAH4AKwEA"
                + "AABlcQB+AC1xAH4AqXEAfgCqcQB+AJ1wcHNxAH4AJHEAfgDJc3EAfgArAQAAACNxAH4ALXEAfgDMcQB+AM10AAZz"
                + "dWJtaXRwcHNxAH4AJHNxAH4AJwB0ACBqdW5pdC1wbGF0Zm9ybS1lbmdpbmUtMS4xNC40LmphcnEAfgCZc3EAfgAr"
                + "AQAAADlxAH4ALXQAR29yZy5qdW5pdC5wbGF0Zm9ybS5lbmdpbmUuc3VwcG9ydC5oaWVyYXJjaGljYWwuSGllcmFy"
                + "Y2hpY2FsVGVzdEV4ZWN1dG9ydAAdSGllcmFyY2hpY2FsVGVzdEV4ZWN1dG9yLmphdmFxAH4AnXBwc3EAfgAkc3EA"
                + "fgAnAHQAIGp1bml0LXBsYXRmb3JtLWVuZ2luZS0xLjE0LjQuamFycQB+AJlzcQB+ACsBAAAANnEAfgAtdABFb3Jn"
                + "Lmp1bml0LnBsYXRmb3JtLmVuZ2luZS5zdXBwb3J0LmhpZXJhcmNoaWNhbC5IaWVyYXJjaGljYWxUZXN0RW5naW5l"
                + "dAAbSGllcmFyY2hpY2FsVGVzdEVuZ2luZS5qYXZhcQB+AJ1wcHNxAH4AJHNxAH4AJwB0ACJqdW5pdC1wbGF0Zm9y"
                + "bS1sYXVuY2hlci0xLjE0LjQuamFydAAGMS4xNC40c3EAfgArAQAAAOZxAH4ALXQAPG9yZy5qdW5pdC5wbGF0Zm9y"
                + "bS5sYXVuY2hlci5jb3JlLkVuZ2luZUV4ZWN1dGlvbk9yY2hlc3RyYXRvcnQAIEVuZ2luZUV4ZWN1dGlvbk9yY2hl"
                + "c3RyYXRvci5qYXZhdAANZXhlY3V0ZUVuZ2luZXBwc3EAfgAkcQB+AQNzcQB+ACsBAAAAzHEAfgAtcQB+AQdxAH4B"
                + "CHQAE2ZhaWxPckV4ZWN1dGVFbmdpbmVwcHNxAH4AJHEAfgEDc3EAfgArAQAAAKxxAH4ALXEAfgEHcQB+AQhxAH4A"
                + "nXBwc3EAfgAkcQB+AQNzcQB+ACsBAAAAZXEAfgAtcQB+AQdxAH4BCHEAfgCdcHBzcQB+ACRxAH4BA3NxAH4AKwEA"
                + "AABAcQB+AC1xAH4BB3EAfgEIdAAQbGFtYmRhJGV4ZWN1dGUkMHBwc3EAfgAkcQB+AQNzcQB+ACsBAAAAlnEAfgAt"
                + "cQB+AQdxAH4BCHQAFndpdGhJbnRlcmNlcHRlZFN0cmVhbXNwcHNxAH4AJHEAfgEDc3EAfgArAQAAAD9xAH4ALXEA"
                + "fgEHcQB+AQhxAH4AnXBwc3EAfgAkc3EAfgAnAHQAImp1bml0LXBsYXRmb3JtLWxhdW5jaGVyLTEuMTQuNC5qYXJx"
                + "AH4BBXNxAH4AKwEAAABtcQB+AC10ADBvcmcuanVuaXQucGxhdGZvcm0ubGF1bmNoZXIuY29yZS5EZWZhdWx0TGF1"
                + "bmNoZXJ0ABREZWZhdWx0TGF1bmNoZXIuamF2YXEAfgCdcHBzcQB+ACRxAH4BGnNxAH4AKwEAAABbcQB+AC1xAH4B"
                + "HXEAfgEecQB+AJ1wcHNxAH4AJHNxAH4AJwB0ACJqdW5pdC1wbGF0Zm9ybS1sYXVuY2hlci0xLjE0LjQuamFycQB+"
                + "AQVzcQB+ACsBAAAAL3EAfgAtdAAzb3JnLmp1bml0LnBsYXRmb3JtLmxhdW5jaGVyLmNvcmUuRGVsZWdhdGluZ0xh"
                + "dW5jaGVydAAXRGVsZWdhdGluZ0xhdW5jaGVyLmphdmFxAH4AnXBwc3EAfgAkc3EAfgAnAHQAImp1bml0LXBsYXRm"
                + "b3JtLWxhdW5jaGVyLTEuMTQuNC5qYXJxAH4BBXNxAH4AKwEAAAAncQB+AC10ADVvcmcuanVuaXQucGxhdGZvcm0u"
                + "bGF1bmNoZXIuY29yZS5JbnRlcmNlcHRpbmdMYXVuY2hlcnQAGUludGVyY2VwdGluZ0xhdW5jaGVyLmphdmF0ABBs"
                + "YW1iZGEkZXhlY3V0ZSQxcHBzcQB+ACRzcQB+ACcBdAAianVuaXQtcGxhdGZvcm0tbGF1bmNoZXItMS4xNC40Lmph"
                + "cnEAfgEFc3EAfgArAQAAABlxAH4ALXQATm9yZy5qdW5pdC5wbGF0Zm9ybS5sYXVuY2hlci5jb3JlLkNsYXNzcGF0"
                + "aEFsaWdubWVudENoZWNraW5nTGF1bmNoZXJJbnRlcmNlcHRvcnQAMkNsYXNzcGF0aEFsaWdubWVudENoZWNraW5n"
                + "TGF1bmNoZXJJbnRlcmNlcHRvci5qYXZhcQB+AGhwcHNxAH4AJHNxAH4AJwF0ACJqdW5pdC1wbGF0Zm9ybS1sYXVu"
                + "Y2hlci0xLjE0LjQuamFycQB+AQVzcQB+ACsBAAAAJnEAfgAtcQB+AStxAH4BLHEAfgCdcHBzcQB+ACRzcQB+ACcB"
                + "dAAianVuaXQtcGxhdGZvcm0tbGF1bmNoZXItMS4xNC40LmphcnEAfgEFc3EAfgArAQAAAC9xAH4ALXEAfgElcQB+"
                + "ASZxAH4AnXBwc3EAfgAkc3EAfgAnAXQAIXN1cmVmaXJlLWp1bml0LXBsYXRmb3JtLTMuNS4yLmphcnQABTMuNS4y"
                + "c3EAfgArAQAAADhxAH4ALXQANG9yZy5hcGFjaGUubWF2ZW4uc3VyZWZpcmUuanVuaXRwbGF0Zm9ybS5MYXp5TGF1"
                + "bmNoZXJ0ABFMYXp5TGF1bmNoZXIuamF2YXEAfgCdcHBzcQB+ACRzcQB+ACcBdAAhc3VyZWZpcmUtanVuaXQtcGxh"
                + "dGZvcm0tMy41LjIuamFycQB+AT9zcQB+ACsBAAAAuHEAfgAtdAA9b3JnLmFwYWNoZS5tYXZlbi5zdXJlZmlyZS5q"
                + "dW5pdHBsYXRmb3JtLkpVbml0UGxhdGZvcm1Qcm92aWRlcnQAGkpVbml0UGxhdGZvcm1Qcm92aWRlci5qYXZhcQB+"
                + "AJ1wcHNxAH4AJHNxAH4AJwF0ACFzdXJlZmlyZS1qdW5pdC1wbGF0Zm9ybS0zLjUuMi5qYXJxAH4BP3NxAH4AKwEA"
                + "AACUcQB+AC1xAH4BR3EAfgFIdAAOaW52b2tlQWxsVGVzdHNwcHNxAH4AJHNxAH4AJwF0ACFzdXJlZmlyZS1qdW5p"
                + "dC1wbGF0Zm9ybS0zLjUuMi5qYXJxAH4BP3NxAH4AKwEAAAB4cQB+AC1xAH4BR3EAfgFIcQB+ADtwcHNxAH4AJHNx"
                + "AH4AJwF0ABlzdXJlZmlyZS1ib290ZXItMy41LjIuamFydAAFMy41LjJzcQB+ACsBAAABgXEAfgAtdAAtb3JnLmFw"
                + "YWNoZS5tYXZlbi5zdXJlZmlyZS5ib290ZXIuRm9ya2VkQm9vdGVydAARRm9ya2VkQm9vdGVyLmphdmF0ABJydW5T"
                + "dWl0ZXNJblByb2Nlc3NwcHNxAH4AJHNxAH4AJwF0ABlzdXJlZmlyZS1ib290ZXItMy41LjIuamFycQB+AVVzcQB+"
                + "ACsBAAAAonEAfgAtcQB+AVdxAH4BWHEAfgCdcHBzcQB+ACRzcQB+ACcBdAAZc3VyZWZpcmUtYm9vdGVyLTMuNS4y"
                + "LmphcnEAfgFVc3EAfgArAQAAAftxAH4ALXEAfgFXcQB+AVh0AANydW5wcHNxAH4AJHNxAH4AJwF0ABlzdXJlZmly"
                + "ZS1ib290ZXItMy41LjIuamFycQB+AVVzcQB+ACsBAAAB73EAfgAtcQB+AVdxAH4BWHQABG1haW5wcHQAFk9NRyBJ"
                + "J3ZlIGJlZW4gZGVsZXRlZCFxAH4BaHQARW9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuTG9nNGpM"
                + "b2dFdmVudFRlc3QkRGVsZXRlZEV4Y2VwdGlvbnVyADRbTG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmlt"
                + "cGwuVGhyb3dhYmxlUHJveHk7+u0B4IWi6zkCAAB4cAAAAABxAH4AGnEAfgAadXIAAltCrPMX+AYIVOACAAB4cAAA"
                + "AGms7QAFc3IALm9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5tZXNzYWdlLlNpbXBsZU1lc3NhZ2WLdE0wYLeiqAMA"
                + "AUwAB21lc3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZzt4cHQAA2FiY3h4";

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
    @Tag("serialization")
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
                + "AAUAAAAASZYC0nNyADJvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGoudXRpbC5Tb3J0ZWRBcnJheVN0cmluZ01hcM80"
                + "Cd6qXjcaAwAEWgAJaW1tdXRhYmxlSQAEc2l6ZUkACXRocmVzaG9sZFsABGtleXN0ABNbTGphdmEvbGFuZy9TdHJp"
                + "bmc7eHABAAAAAAAAAAF1cgATW0xqYXZhLmxhbmcuU3RyaW5nO63SVufpHXtHAgAAeHAAAAAAdXIAA1tbQkv9GRVn"
                + "Z9s3AgAAeHAAAAAAeHNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouVGhyZWFkQ29udGV4dCRFbXB0eVRocmVh"
                + "ZENvbnRleHRTdGFjawAAAAAAAAABAgAAeHBzcgAeb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLkxldmVsAAAAAAAY"
                + "IBoCAANJAAhpbnRMZXZlbEwABG5hbWVxAH4ABEwADXN0YW5kYXJkTGV2ZWx0ACxMb3JnL2FwYWNoZS9sb2dnaW5n"
                + "L2xvZzRqL3NwaS9TdGFuZGFyZExldmVsO3hwAAABkHQABElORk9+cgAqb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRq"
                + "LnNwaS5TdGFuZGFyZExldmVsAAAAAAAAAAASAAB4cgAOamF2YS5sYW5nLkVudW0AAAAAAAAAABIAAHhwdAAESU5G"
                + "T3B0AAlzb21lLnRlc3RwdAADYWJjcHQAAHQABG1haW5wcQB+ABxxAH4AHHVyAAJbQqzzF/gGCFTgAgAAeHAAAABY"
                + "rO0ABXNyAENvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnRUZXN0JERlbGV0"
                + "ZWRNZXNzYWdlAAAAAAAAAAECAAB4cHg=";

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
}
