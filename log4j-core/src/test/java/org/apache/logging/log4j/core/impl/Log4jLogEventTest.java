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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.ClockFactoryTest;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public static void afterClass() throws IllegalAccessException {
        ClockFactoryTest.resetClocks();
    }

    @Test
    public void testJavaIoSerializable() throws Exception {
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

    private byte[] serialize(final Log4jLogEvent event) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(event);
        return arr.toByteArray();
    }

    private Log4jLogEvent deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
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
        // String base64Str = DatatypeConverter.printBase64Binary(binary);
        // System.out.println("final String base64 = \"" + base64Str.replaceAll("\r\n", "\\\\r\\\\n\" +\r\n\"") +
        // "\";");
        
         final String base64 = "rO0ABXNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnQkTG9nRXZlbnRQcm94eZztD11w2ioWAgANWgAMaXNFbmRPZkJhdGNoWgASaXNMb2NhdGlvblJlcXVpcmVkSgAKdGltZU1pbGxpc0wACmNvbnRleHRNYXB0AA9MamF2YS91dGlsL01hcDtMAAxjb250ZXh0U3RhY2t0ADVMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL1RocmVhZENvbnRleHQkQ29udGV4dFN0YWNrO0wABWxldmVsdAAgTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9MZXZlbDtMAApsb2dnZXJGUUNOdAASTGphdmEvbGFuZy9TdHJpbmc7TAAKbG9nZ2VyTmFtZXEAfgAETAAGbWFya2VydAAhTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9NYXJrZXI7TAAHbWVzc2FnZXQAKkxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovbWVzc2FnZS9NZXNzYWdlO0wABnNvdXJjZXQAHUxqYXZhL2xhbmcvU3RhY2tUcmFjZUVsZW1lbnQ7TAAKdGhyZWFkTmFtZXEAfgAETAALdGhyb3duUHJveHl0ADNMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1wbC9UaHJvd2FibGVQcm94eTt4cAAAAAAAAEmWAtJzcgAeamF2YS51dGlsLkNvbGxlY3Rpb25zJEVtcHR5TWFwWTYUhVrc59ACAAB4cHNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouVGhyZWFkQ29udGV4dCRFbXB0eVRocmVhZENvbnRleHRTdGFjawAAAAAAAAABAgAAeHBzcgAeb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLkxldmVsAAAAAAAYIBoCAANJAAhpbnRMZXZlbEwABG5hbWVxAH4ABEwADXN0YW5kYXJkTGV2ZWx0ACxMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL3NwaS9TdGFuZGFyZExldmVsO3hwAAABkHQABElORk9+cgAqb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLnNwaS5TdGFuZGFyZExldmVsAAAAAAAAAAASAAB4cgAOamF2YS5sYW5nLkVudW0AAAAAAAAAABIAAHhwdAAESU5GT3QAAHQACXNvbWUudGVzdHBzcgAub3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLm1lc3NhZ2UuU2ltcGxlTWVzc2FnZYt0TTBgt6KoAgABTAAHbWVzc2FnZXEAfgAEeHB0AANhYmNwdAAEbWFpbnNyADFvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h52cww1Zp7rPoCAAdJABJjb21tb25FbGVtZW50Q291bnRMAApjYXVzZVByb3h5cQB+AAhbABJleHRlbmRlZFN0YWNrVHJhY2V0AD9bTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvRXh0ZW5kZWRTdGFja1RyYWNlRWxlbWVudDtMABBsb2NhbGl6ZWRNZXNzYWdlcQB+AARMAAdtZXNzYWdlcQB+AARMAARuYW1lcQB+AARbABFzdXBwcmVzc2VkUHJveGllc3QANFtMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1wbC9UaHJvd2FibGVQcm94eTt4cAAAAABwdXIAP1tMb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW50O8rPiCOlx8+8AgAAeHAAAAAac3IAPG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuRXh0ZW5kZWRTdGFja1RyYWNlRWxlbWVudOHez7rGtpAHAgACTAAOZXh0cmFDbGFzc0luZm90ADZMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1wbC9FeHRlbmRlZENsYXNzSW5mbztMABFzdGFja1RyYWNlRWxlbWVudHEAfgAHeHBzcgA0b3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZENsYXNzSW5mbwAAAAAAAAABAgADWgAFZXhhY3RMAAhsb2NhdGlvbnEAfgAETAAHdmVyc2lvbnEAfgAEeHABdAANdGVzdC1jbGFzc2VzL3QAAT9zcgAbamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50YQnFmiY23YUCAARJAApsaW5lTnVtYmVyTAAOZGVjbGFyaW5nQ2xhc3NxAH4ABEwACGZpbGVOYW1lcQB+AARMAAptZXRob2ROYW1lcQB+AAR4cAAAAJh0ADRvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnRUZXN0dAAWTG9nNGpMb2dFdmVudFRlc3QuamF2YXQAKnRlc3RKYXZhSW9TZXJpYWxpemFibGVXaXRoVW5rbm93blRocm93YWJsZXNxAH4AInNxAH4AJQBxAH4AKHQACDEuNy4wXzU1c3EAfgAp/////nQAJHN1bi5yZWZsZWN0Lk5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbHB0AAdpbnZva2Uwc3EAfgAic3EAfgAlAHEAfgAocQB+ADBzcQB+ACn/////cQB+ADJwdAAGaW52b2tlc3EAfgAic3EAfgAlAHEAfgAocQB+ADBzcQB+ACn/////dAAoc3VuLnJlZmxlY3QuRGVsZWdhdGluZ01ldGhvZEFjY2Vzc29ySW1wbHBxAH4AN3NxAH4AInNxAH4AJQBxAH4AKHEAfgAwc3EAfgAp/////3QAGGphdmEubGFuZy5yZWZsZWN0Lk1ldGhvZHBxAH4AN3NxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAAL3QAKW9yZy5qdW5pdC5ydW5uZXJzLm1vZGVsLkZyYW1ld29ya01ldGhvZCQxdAAURnJhbWV3b3JrTWV0aG9kLmphdmF0ABFydW5SZWZsZWN0aXZlQ2FsbHNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAADHQAM29yZy5qdW5pdC5pbnRlcm5hbC5ydW5uZXJzLm1vZGVsLlJlZmxlY3RpdmVDYWxsYWJsZXQAF1JlZmxlY3RpdmVDYWxsYWJsZS5qYXZhdAADcnVuc3EAfgAic3EAfgAlAXQADmp1bml0LTQuMTEuamFycQB+AChzcQB+ACkAAAAsdAAnb3JnLmp1bml0LnJ1bm5lcnMubW9kZWwuRnJhbWV3b3JrTWV0aG9kcQB+AEV0ABFpbnZva2VFeHBsb3NpdmVseXNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAAEXQAMm9yZy5qdW5pdC5pbnRlcm5hbC5ydW5uZXJzLnN0YXRlbWVudHMuSW52b2tlTWV0aG9kdAARSW52b2tlTWV0aG9kLmphdmF0AAhldmFsdWF0ZXNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAABD3QAHm9yZy5qdW5pdC5ydW5uZXJzLlBhcmVudFJ1bm5lcnQAEVBhcmVudFJ1bm5lci5qYXZhdAAHcnVuTGVhZnNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAARnQAKG9yZy5qdW5pdC5ydW5uZXJzLkJsb2NrSlVuaXQ0Q2xhc3NSdW5uZXJ0ABtCbG9ja0pVbml0NENsYXNzUnVubmVyLmphdmF0AAhydW5DaGlsZHNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAAMnEAfgBmcQB+AGdxAH4AaHNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAA7nQAIG9yZy5qdW5pdC5ydW5uZXJzLlBhcmVudFJ1bm5lciQzcQB+AGBxAH4ATXNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAAP3QAIG9yZy5qdW5pdC5ydW5uZXJzLlBhcmVudFJ1bm5lciQxcQB+AGB0AAhzY2hlZHVsZXNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAA7HEAfgBfcQB+AGB0AAtydW5DaGlsZHJlbnNxAH4AInNxAH4AJQF0AA5qdW5pdC00LjExLmphcnEAfgAoc3EAfgApAAAANXEAfgBfcQB+AGB0AAphY2Nlc3MkMDAwc3EAfgAic3EAfgAlAXQADmp1bml0LTQuMTEuamFycQB+AChzcQB+ACkAAADldAAgb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVyJDJxAH4AYHEAfgBac3EAfgAic3EAfgAlAXQADmp1bml0LTQuMTEuamFycQB+AChzcQB+ACkAAAAadAAwb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMuc3RhdGVtZW50cy5SdW5CZWZvcmVzdAAPUnVuQmVmb3Jlcy5qYXZhcQB+AFpzcQB+ACJzcQB+ACUBdAAOanVuaXQtNC4xMS5qYXJxAH4AKHNxAH4AKQAAABt0AC9vcmcuanVuaXQuaW50ZXJuYWwucnVubmVycy5zdGF0ZW1lbnRzLlJ1bkFmdGVyc3QADlJ1bkFmdGVycy5qYXZhcQB+AFpzcQB+ACJzcQB+ACUBdAAOanVuaXQtNC4xMS5qYXJxAH4AKHNxAH4AKQAAATVxAH4AX3EAfgBgcQB+AE1zcQB+ACJzcQB+ACUBdAAELmNwL3EAfgAoc3EAfgApAAAAMnQAOm9yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC5qdW5pdDQucnVubmVyLkpVbml0NFRlc3RSZWZlcmVuY2V0ABhKVW5pdDRUZXN0UmVmZXJlbmNlLmphdmFxAH4ATXNxAH4AInNxAH4AJQF0AAQuY3AvcQB+AChzcQB+ACkAAAAmdAAzb3JnLmVjbGlwc2UuamR0LmludGVybmFsLmp1bml0LnJ1bm5lci5UZXN0RXhlY3V0aW9udAASVGVzdEV4ZWN1dGlvbi5qYXZhcQB+AE1zcQB+ACJzcQB+ACUBdAAELmNwL3EAfgAoc3EAfgApAAAB03QANm9yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC5qdW5pdC5ydW5uZXIuUmVtb3RlVGVzdFJ1bm5lcnQAFVJlbW90ZVRlc3RSdW5uZXIuamF2YXQACHJ1blRlc3Rzc3EAfgAic3EAfgAlAXQABC5jcC9xAH4AKHNxAH4AKQAAAqtxAH4Ap3EAfgCocQB+AKlzcQB+ACJzcQB+ACUBdAAELmNwL3EAfgAoc3EAfgApAAABhnEAfgCncQB+AKhxAH4ATXNxAH4AInNxAH4AJQF0AAQuY3AvcQB+AChzcQB+ACkAAADFcQB+AKdxAH4AqHQABG1haW50ABZPTUcgSSd2ZSBiZWVuIGRlbGV0ZWQhcQB+ALd0AEVvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnRUZXN0JERlbGV0ZWRFeGNlcHRpb251cgA0W0xvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h5O/rtAeCFous5AgAAeHAAAAAA";

        final byte[] binaryDecoded = DatatypeConverter.parseBase64Binary(base64);
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
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt = Log4jLogEvent.newBuilder().setLevel(NULL_LEVEL).build();
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testTimestampGeneratedByClock() {
        final LogEvent evt = Log4jLogEvent.newBuilder().build();
        assertEquals(FixedTimeClock.FIXED_TIME, evt.getTimeMillis());
    }
    
    @Test
    public void testInitiallyDummyNanoClock() {
        assertTrue(Log4jLogEvent.getNanoClock() instanceof DummyNanoClock);
        assertEquals("initial dummy nanotime", 0, Log4jLogEvent.getNanoClock().nanoTime());
    }
    
    @Test
    public void testNanoTimeGeneratedByNanoClock() {
        Log4jLogEvent.setNanoClock(new DummyNanoClock(123));
        verifyNanoTimeWithAllConstructors(123);
        Log4jLogEvent.setNanoClock(new DummyNanoClock(87654));
        verifyNanoTimeWithAllConstructors(87654);
    }

    @SuppressWarnings("deprecation")
    private void verifyNanoTimeWithAllConstructors(long expected) {
        assertEquals(expected, Log4jLogEvent.getNanoClock().nanoTime());

        assertEquals("No-arg constructor", expected, new Log4jLogEvent().getNanoTime());
        assertEquals("1-arg constructor", expected, new Log4jLogEvent(98).getNanoTime());
        assertEquals("6-arg constructor", expected, new Log4jLogEvent("l", null, "a", null, null, null).getNanoTime());
        assertEquals("7-arg constructor", expected, new Log4jLogEvent("l", null, "a", null, null, null, null)
                .getNanoTime());
        assertEquals("11-arg constructor", expected, new Log4jLogEvent("l", null, "a", null, null, null, null, null,
                null, null, 0).getNanoTime());
        assertEquals("12-arg factory method", expected, Log4jLogEvent.createEvent("l", null, "a", null, null, null,
                null, null, null, null, null, 0).getNanoTime());
    }
    
    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributes() {
        final Map<String, String> contextMap = new HashMap<String, String>();
        contextMap.put("A", "B");
        final ContextStack contextStack = ThreadContext.getImmutableStack();
        final Exception exception = new Exception("test");
        final Marker marker = MarkerManager.getMarker("EVENTTEST");
        final Message message = new SimpleMessage("foo");
        final StackTraceElement stackTraceElement = new StackTraceElement("A", "B", "file", 123);
        final String fqcn = "qualified";
        final String name = "Ceci n'est pas une pipe";
        final String threadName = "threadName";
        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setContextMap(contextMap) //
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
        
        assertSame(contextMap, event.getContextMap());
        assertSame(contextStack, event.getContextStack());
        assertEquals(true, event.isEndOfBatch());
        assertEquals(true, event.isIncludeLocation());
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
        
        LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals("copy constructor builder", event2, event);
        assertEquals("same hashCode", event2.hashCode(), event.hashCode());
    }
    
    @Test
    public void testEquals() {
        final Map<String, String> contextMap = new HashMap<String, String>();
        contextMap.put("A", "B");
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
                .setContextMap(contextMap) //
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
        
        assertSame(contextMap, event.getContextMap());
        assertSame(contextStack, event.getContextStack());
        assertEquals(true, event.isEndOfBatch());
        assertEquals(true, event.isIncludeLocation());
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
        assertEquals("copy constructor builder", event2, event);
        assertEquals("same hashCode", event2.hashCode(), event.hashCode());
        
        assertSame(contextMap, event2.getContextMap());
        assertSame(contextStack, event2.getContextStack());
        assertEquals(true, event2.isEndOfBatch());
        assertEquals(true, event2.isIncludeLocation());
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
        
        final Map<String, String> differentMap = Collections.emptyMap();
        different("different contextMap", builder(event).setContextMap(differentMap), event);
        different("null contextMap", builder(event).setContextMap(null), event);

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
        try { // TODO null logger name throws NPE in equals. Use Objects.requireNonNull in constructor?
            different("null name", builder(event).setLoggerName(null), event);
            fail("Expected NullPointerException");
        } catch (NullPointerException ok) {
        }

        different("different marker", builder(event).setMarker(MarkerManager.getMarker("different")), event);
        different("null marker", builder(event).setMarker(null), event);

        different("different message", builder(event).setMessage(new ObjectMessage("different")), event);
        try { // TODO null message throws NPE in equals(). Use Objects.requireNonNull in constructor?
            different("null message", builder(event).setMessage(null), event);
            fail("Expected NullPointerException");
        } catch (NullPointerException ok) {
        }

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
    
    private static Log4jLogEvent.Builder builder(LogEvent event) {
        return new Log4jLogEvent.Builder(event);
    }
    
    private void different(String reason, Log4jLogEvent.Builder builder, LogEvent event) {
        final LogEvent other = builder.build();
        assertNotEquals(reason, other, event);
        assertNotEquals(reason + " hashCode", other.hashCode(), event.hashCode());
    }
}
