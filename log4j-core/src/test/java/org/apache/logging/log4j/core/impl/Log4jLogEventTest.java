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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import sun.misc.BASE64Decoder;

import static org.junit.Assert.*;

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

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ClockFactory.PROPERTY_NAME, FixedTimeClock.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
    }

    @Test
    public void testJavaIoSerializable() throws Exception {
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", null, Strings.EMPTY, Level.INFO, new SimpleMessage(
                "abc"), null);

        byte[] binary = serialize(evt);
        final Log4jLogEvent evt2 = deserialize(binary);

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFqcn(), evt2.getLoggerFqcn());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
        assertEquals(evt.getContextStack(), evt2.getContextStack());
        assertEquals(evt.getMessage(), evt2.getMessage());
        assertEquals(evt.getSource(), evt2.getSource());
        assertEquals(evt.getThreadName(), evt2.getThreadName());
        assertEquals(evt.getThrown(), evt2.getThrown());
        assertEquals(evt.isEndOfBatch(), evt2.isEndOfBatch());
        assertEquals(evt.isIncludeLocation(), evt2.isIncludeLocation());
    }

    @Test
    public void testJavaIoSerializableWithThrown() throws Exception {
        final Error thrown = new InternalError("test error");
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", null, Strings.EMPTY, Level.INFO, new SimpleMessage(
                "abc"), thrown);

        byte[] binary = serialize(evt);
        final Log4jLogEvent evt2 = deserialize(binary);

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFqcn(), evt2.getLoggerFqcn());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
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

    private byte[] serialize(Log4jLogEvent event) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(event);
        return arr.toByteArray();
    }

    private Log4jLogEvent deserialize(byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        final Log4jLogEvent result = (Log4jLogEvent) in.readObject();
        return result;
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
    public void testJavaIoSerializableWithUnknownThrowable() throws Exception {
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
        //
        // String base64 = new BASE64Encoder().encode(binary);
        // System.out.println("final String base64 = \"" + base64.replaceAll("\r\n", "\\\\r\\\\n\" +\r\n\"") + "\";");

        final String base64 = "rO0ABXNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnQk\r\n"
                + "TG9nRXZlbnRQcm94eZztD11w2ioWAgAOWgAMaXNFbmRPZkJhdGNoWgASaXNMb2NhdGlvblJlcXVp\r\n"
                + "cmVkSgAKdGltZU1pbGxpc0wACmNvbnRleHRNYXB0AA9MamF2YS91dGlsL01hcDtMAAxjb250ZXh0\r\n"
                + "U3RhY2t0ADVMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL1RocmVhZENvbnRleHQkQ29udGV4dFN0\r\n"
                + "YWNrO0wABWxldmVsdAAgTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9MZXZlbDtMAApsb2dnZXJG\r\n"
                + "UUNOdAASTGphdmEvbGFuZy9TdHJpbmc7TAAKbG9nZ2VyTmFtZXEAfgAETAAGbWFya2VydAAhTG9y\r\n"
                + "Zy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9NYXJrZXI7TAAHbWVzc2FnZXQAKkxvcmcvYXBhY2hlL2xv\r\n"
                + "Z2dpbmcvbG9nNGovbWVzc2FnZS9NZXNzYWdlO0wABnNvdXJjZXQAHUxqYXZhL2xhbmcvU3RhY2tU\r\n"
                + "cmFjZUVsZW1lbnQ7TAAKdGhyZWFkTmFtZXEAfgAETAAGdGhyb3dudAAVTGphdmEvbGFuZy9UaHJv\r\n"
                + "d2FibGU7TAALdGhyb3duUHJveHl0ADNMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1w\r\n"
                + "bC9UaHJvd2FibGVQcm94eTt4cAAAAAAAAEmWAtJzcgAeamF2YS51dGlsLkNvbGxlY3Rpb25zJEVt\r\n"
                + "cHR5TWFwWTYUhVrc59ACAAB4cHNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouVGhyZWFkQ29u\r\n"
                + "dGV4dCRFbXB0eVRocmVhZENvbnRleHRTdGFjawAAAAAAAAABAgAAeHBzcgAeb3JnLmFwYWNoZS5s\r\n"
                + "b2dnaW5nLmxvZzRqLkxldmVsAAAAAAAYIBoCAANJAAhpbnRMZXZlbEwABG5hbWVxAH4ABEwADXN0\r\n"
                + "YW5kYXJkTGV2ZWx0ACxMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL3NwaS9TdGFuZGFyZExldmVs\r\n"
                + "O3hwAAABkHQABElORk9+cgAqb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLnNwaS5TdGFuZGFyZExl\r\n"
                + "dmVsAAAAAAAAAAASAAB4cgAOamF2YS5sYW5nLkVudW0AAAAAAAAAABIAAHhwdAAESU5GT3QAAHQA\r\n"
                + "CXNvbWUudGVzdHBzcgAub3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLm1lc3NhZ2UuU2ltcGxlTWVz\r\n"
                + "c2FnZYt0TTBgt6KoAgABTAAHbWVzc2FnZXEAfgAEeHB0AANhYmNwdAAEbWFpbnNyAEVvcmcuYXBh\r\n"
                + "Y2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnRUZXN0JERlbGV0ZWRFeGNl\r\n"
                + "cHRpb24AAAAAAAAAAQIAAHhyABNqYXZhLmxhbmcuRXhjZXB0aW9u0P0fPho7HMQCAAB4cgATamF2\r\n"
                + "YS5sYW5nLlRocm93YWJsZdXGNSc5d7jLAwADTAAFY2F1c2VxAH4ACEwADWRldGFpbE1lc3NhZ2Vx\r\n"
                + "AH4ABFsACnN0YWNrVHJhY2V0AB5bTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDt4cHEAfgAh\r\n"
                + "dAAWT01HIEkndmUgYmVlbiBkZWxldGVkIXVyAB5bTGphdmEubGFuZy5TdGFja1RyYWNlRWxlbWVu\r\n"
                + "dDsCRio8PP0iOQIAAHhwAAAAGnNyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRhCcWaJjbd\r\n"
                + "hQIABEkACmxpbmVOdW1iZXJMAA5kZWNsYXJpbmdDbGFzc3EAfgAETAAIZmlsZU5hbWVxAH4ABEwA\r\n"
                + "Cm1ldGhvZE5hbWVxAH4ABHhwAAAAl3QANG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmlt\r\n"
                + "cGwuTG9nNGpMb2dFdmVudFRlc3R0ABZMb2c0akxvZ0V2ZW50VGVzdC5qYXZhdAAqdGVzdEphdmFJ\r\n"
                + "b1NlcmlhbGl6YWJsZVdpdGhVbmtub3duVGhyb3dhYmxlc3EAfgAl/////nQAJHN1bi5yZWZsZWN0\r\n"
                + "Lk5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbHQAHU5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbC5qYXZh\r\n"
                + "dAAHaW52b2tlMHNxAH4AJQAAACdxAH4AK3EAfgAsdAAGaW52b2tlc3EAfgAlAAAAGXQAKHN1bi5y\r\n"
                + "ZWZsZWN0LkRlbGVnYXRpbmdNZXRob2RBY2Nlc3NvckltcGx0ACFEZWxlZ2F0aW5nTWV0aG9kQWNj\r\n"
                + "ZXNzb3JJbXBsLmphdmFxAH4AL3NxAH4AJQAAAlV0ABhqYXZhLmxhbmcucmVmbGVjdC5NZXRob2R0\r\n"
                + "AAtNZXRob2QuamF2YXEAfgAvc3EAfgAlAAAAL3QAKW9yZy5qdW5pdC5ydW5uZXJzLm1vZGVsLkZy\r\n"
                + "YW1ld29ya01ldGhvZCQxdAAURnJhbWV3b3JrTWV0aG9kLmphdmF0ABFydW5SZWZsZWN0aXZlQ2Fs\r\n"
                + "bHNxAH4AJQAAAAx0ADNvcmcuanVuaXQuaW50ZXJuYWwucnVubmVycy5tb2RlbC5SZWZsZWN0aXZl\r\n"
                + "Q2FsbGFibGV0ABdSZWZsZWN0aXZlQ2FsbGFibGUuamF2YXQAA3J1bnNxAH4AJQAAACx0ACdvcmcu\r\n"
                + "anVuaXQucnVubmVycy5tb2RlbC5GcmFtZXdvcmtNZXRob2RxAH4AOHQAEWludm9rZUV4cGxvc2l2\r\n"
                + "ZWx5c3EAfgAlAAAAEXQAMm9yZy5qdW5pdC5pbnRlcm5hbC5ydW5uZXJzLnN0YXRlbWVudHMuSW52\r\n"
                + "b2tlTWV0aG9kdAARSW52b2tlTWV0aG9kLmphdmF0AAhldmFsdWF0ZXNxAH4AJQAAAQ90AB5vcmcu\r\n"
                + "anVuaXQucnVubmVycy5QYXJlbnRSdW5uZXJ0ABFQYXJlbnRSdW5uZXIuamF2YXQAB3J1bkxlYWZz\r\n"
                + "cQB+ACUAAABGdAAob3JnLmp1bml0LnJ1bm5lcnMuQmxvY2tKVW5pdDRDbGFzc1J1bm5lcnQAG0Js\r\n"
                + "b2NrSlVuaXQ0Q2xhc3NSdW5uZXIuamF2YXQACHJ1bkNoaWxkc3EAfgAlAAAAMnEAfgBKcQB+AEtx\r\n"
                + "AH4ATHNxAH4AJQAAAO50ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXIkM3EAfgBHcQB+\r\n"
                + "AD1zcQB+ACUAAAA/dAAgb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVyJDFxAH4AR3QACHNj\r\n"
                + "aGVkdWxlc3EAfgAlAAAA7HEAfgBGcQB+AEd0AAtydW5DaGlsZHJlbnNxAH4AJQAAADVxAH4ARnEA\r\n"
                + "fgBHdAAKYWNjZXNzJDAwMHNxAH4AJQAAAOV0ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5u\r\n"
                + "ZXIkMnEAfgBHcQB+AERzcQB+ACUAAAAadAAwb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMuc3Rh\r\n"
                + "dGVtZW50cy5SdW5CZWZvcmVzdAAPUnVuQmVmb3Jlcy5qYXZhcQB+AERzcQB+ACUAAAAbdAAvb3Jn\r\n"
                + "Lmp1bml0LmludGVybmFsLnJ1bm5lcnMuc3RhdGVtZW50cy5SdW5BZnRlcnN0AA5SdW5BZnRlcnMu\r\n"
                + "amF2YXEAfgBEc3EAfgAlAAABNXEAfgBGcQB+AEdxAH4APXNxAH4AJQAAADJ0ADpvcmcuZWNsaXBz\r\n"
                + "ZS5qZHQuaW50ZXJuYWwuanVuaXQ0LnJ1bm5lci5KVW5pdDRUZXN0UmVmZXJlbmNldAAYSlVuaXQ0\r\n"
                + "VGVzdFJlZmVyZW5jZS5qYXZhcQB+AD1zcQB+ACUAAAAmdAAzb3JnLmVjbGlwc2UuamR0LmludGVy\r\n"
                + "bmFsLmp1bml0LnJ1bm5lci5UZXN0RXhlY3V0aW9udAASVGVzdEV4ZWN1dGlvbi5qYXZhcQB+AD1z\r\n"
                + "cQB+ACUAAAHTdAA2b3JnLmVjbGlwc2UuamR0LmludGVybmFsLmp1bml0LnJ1bm5lci5SZW1vdGVU\r\n"
                + "ZXN0UnVubmVydAAVUmVtb3RlVGVzdFJ1bm5lci5qYXZhdAAIcnVuVGVzdHNzcQB+ACUAAAKrcQB+\r\n"
                + "AGdxAH4AaHEAfgBpc3EAfgAlAAABhnEAfgBncQB+AGhxAH4APXNxAH4AJQAAAMVxAH4AZ3EAfgBo\r\n"
                + "dAAEbWFpbnhzcgAxb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5UaHJvd2FibGVQ\r\n"
                + "cm94ednMMNWae6z6AgAHSQASY29tbW9uRWxlbWVudENvdW50TAAKY2F1c2VQcm94eXEAfgAJWwAS\r\n"
                + "ZXh0ZW5kZWRTdGFja1RyYWNldAA/W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBs\r\n"
                + "L0V4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7TAAQbG9jYWxpemVkTWVzc2FnZXEAfgAETAAHbWVz\r\n"
                + "c2FnZXEAfgAETAAEbmFtZXEAfgAEWwARc3VwcHJlc3NlZFByb3hpZXN0ADRbTG9yZy9hcGFjaGUv\r\n"
                + "bG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvVGhyb3dhYmxlUHJveHk7eHAAAAAAcHVyAD9bTG9yZy5h\r\n"
                + "cGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuRXh0ZW5kZWRTdGFja1RyYWNlRWxlbWVudDvK\r\n"
                + "z4gjpcfPvAIAAHhwAAAAGnNyADxvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4\r\n"
                + "dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnTh3s+6xraQBwIAAkwADmV4dHJhQ2xhc3NJbmZvdAA2TG9y\r\n"
                + "Zy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvRXh0ZW5kZWRDbGFzc0luZm87TAARc3Rh\r\n"
                + "Y2tUcmFjZUVsZW1lbnRxAH4AB3hwc3IANG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmlt\r\n"
                + "cGwuRXh0ZW5kZWRDbGFzc0luZm8AAAAAAAAAAQIAA1oABWV4YWN0TAAIbG9jYXRpb25xAH4ABEwA\r\n"
                + "B3ZlcnNpb25xAH4ABHhwAXQADXRlc3QtY2xhc3Nlcy90AAE/cQB+ACZzcQB+AHRzcQB+AHcAcQB+\r\n"
                + "AHp0AAgxLjYuMF80NXEAfgAqc3EAfgB0c3EAfgB3AHEAfgB6cQB+AH1xAH4ALnNxAH4AdHNxAH4A\r\n"
                + "dwBxAH4AenEAfgB9cQB+ADBzcQB+AHRzcQB+AHcAcQB+AHpxAH4AfXEAfgAzc3EAfgB0c3EAfgB3\r\n"
                + "AXQADmp1bml0LTQuMTEuamFycQB+AHpxAH4ANnNxAH4AdHNxAH4AdwF0AA5qdW5pdC00LjExLmph\r\n"
                + "cnEAfgB6cQB+ADpzcQB+AHRzcQB+AHcBdAAOanVuaXQtNC4xMS5qYXJxAH4AenEAfgA+c3EAfgB0\r\n"
                + "c3EAfgB3AXQADmp1bml0LTQuMTEuamFycQB+AHpxAH4AQXNxAH4AdHNxAH4AdwF0AA5qdW5pdC00\r\n"
                + "LjExLmphcnEAfgB6cQB+AEVzcQB+AHRzcQB+AHcBdAAOanVuaXQtNC4xMS5qYXJxAH4AenEAfgBJ\r\n"
                + "c3EAfgB0c3EAfgB3AXQADmp1bml0LTQuMTEuamFycQB+AHpxAH4ATXNxAH4AdHNxAH4AdwF0AA5q\r\n"
                + "dW5pdC00LjExLmphcnEAfgB6cQB+AE5zcQB+AHRzcQB+AHcBdAAOanVuaXQtNC4xMS5qYXJxAH4A\r\n"
                + "enEAfgBQc3EAfgB0c3EAfgB3AXQADmp1bml0LTQuMTEuamFycQB+AHpxAH4AU3NxAH4AdHNxAH4A\r\n"
                + "dwF0AA5qdW5pdC00LjExLmphcnEAfgB6cQB+AFVzcQB+AHRzcQB+AHcBdAAOanVuaXQtNC4xMS5q\r\n"
                + "YXJxAH4AenEAfgBXc3EAfgB0c3EAfgB3AXQADmp1bml0LTQuMTEuamFycQB+AHpxAH4AWXNxAH4A\r\n"
                + "dHNxAH4AdwF0AA5qdW5pdC00LjExLmphcnEAfgB6cQB+AFxzcQB+AHRzcQB+AHcBdAAOanVuaXQt\r\n"
                + "NC4xMS5qYXJxAH4AenEAfgBfc3EAfgB0c3EAfgB3AXQABC5jcC9xAH4AenEAfgBgc3EAfgB0c3EA\r\n"
                + "fgB3AXQABC5jcC9xAH4AenEAfgBjc3EAfgB0c3EAfgB3AXQABC5jcC9xAH4AenEAfgBmc3EAfgB0\r\n"
                + "c3EAfgB3AXQABC5jcC9xAH4AenEAfgBqc3EAfgB0c3EAfgB3AXQABC5jcC9xAH4AenEAfgBrc3EA\r\n"
                + "fgB0c3EAfgB3AXQABC5jcC9xAH4AenEAfgBscQB+ACJxAH4AInQARW9yZy5hcGFjaGUubG9nZ2lu\r\n"
                + "Zy5sb2c0ai5jb3JlLmltcGwuTG9nNGpMb2dFdmVudFRlc3QkRGVsZXRlZEV4Y2VwdGlvbnVyADRb\r\n"
                + "TG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuVGhyb3dhYmxlUHJveHk7+u0B4IWi\r\n"
                + "6zkCAAB4cAAAAAA=";

        byte[] binaryDecoded = new BASE64Decoder().decodeBuffer(base64);
        final Log4jLogEvent evt2 = deserialize(binaryDecoded);

        assertEquals(loggerFQN, evt2.getLoggerFqcn());
        assertEquals(level, evt2.getLevel());
        assertEquals(loggerName, evt2.getLoggerName());
        assertEquals(marker, evt2.getMarker());
        assertEquals(msg, evt2.getMessage());
        assertEquals(threadName, evt2.getThreadName());
        assertEquals(null, evt2.getThrown());
        assertEquals(this.getClass().getName() + "$DeletedException", evt2.getThrownProxy().getName());
        assertEquals(errorMessage, evt2.getThrownProxy().getMessage());
    }

    @Test
    public void testNullLevelReplacedWithOFF() throws Exception {
        final Marker marker = null;
        final Throwable t = null;
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", marker, Strings.EMPTY, NULL_LEVEL, new SimpleMessage(
                "abc"), t);
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testTimestampGeneratedByClock() {
        final Marker marker = null;
        final Throwable t = null;
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", marker, Strings.EMPTY, NULL_LEVEL, new SimpleMessage(
                "abc"), t);
        assertEquals(FixedTimeClock.FIXED_TIME, evt.getTimeMillis());

    }
}
