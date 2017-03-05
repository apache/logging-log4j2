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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
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
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void testToImmutableSame() {
        final LogEvent logEvent = new Log4jLogEvent();
        Assert.assertSame(logEvent, logEvent.toImmutable());
    }

    @Test
    public void testToImmutableNotSame() {
        final LogEvent logEvent = new Log4jLogEvent.Builder().setMessage(new ReusableObjectMessage()).build();
        LogEvent immutable = logEvent.toImmutable();
        Assert.assertSame(logEvent, immutable);
        Assert.assertFalse(immutable.getMessage() instanceof ReusableMessage);
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

        final String base64 = "rO0ABXNyAD5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnQkTG9nRXZlbnRQcm94eYgtmn+yXsP9AwAQWgAMaXNFbmRPZkJhdGNoWgASaXNMb2NhdGlvblJlcXVpcmVkSgAIdGhyZWFkSWRJAA50aHJlYWRQcmlvcml0eUoACnRpbWVNaWxsaXNMAAtjb250ZXh0RGF0YXQAKUxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovdXRpbC9TdHJpbmdNYXA7TAAMY29udGV4dFN0YWNrdAA1TG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9UaHJlYWRDb250ZXh0JENvbnRleHRTdGFjaztMAAVsZXZlbHQAIExvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovTGV2ZWw7TAAKbG9nZ2VyRlFDTnQAEkxqYXZhL2xhbmcvU3RyaW5nO0wACmxvZ2dlck5hbWVxAH4ABEwABm1hcmtlcnQAIUxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovTWFya2VyO0wAEW1hcnNoYWxsZWRNZXNzYWdldAAbTGphdmEvcm1pL01hcnNoYWxsZWRPYmplY3Q7TAANbWVzc2FnZVN0cmluZ3EAfgAETAAGc291cmNldAAdTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDtMAAp0aHJlYWROYW1lcQB+AARMAAt0aHJvd25Qcm94eXQAM0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL1Rocm93YWJsZVByb3h5O3hwAAAAAAAAAAAAAQAAAAUAAAAASZYC0nNyADJvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGoudXRpbC5Tb3J0ZWRBcnJheVN0cmluZ01hcLA3yJFz7CvcAwACWgAJaW1tdXRhYmxlSQAJdGhyZXNob2xkeHABAAAAAXcIAAAAAQAAAAB4c3IAPm9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5UaHJlYWRDb250ZXh0JEVtcHR5VGhyZWFkQ29udGV4dFN0YWNrAAAAAAAAAAECAAB4cHNyAB5vcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouTGV2ZWwAAAAAABggGgIAA0kACGludExldmVsTAAEbmFtZXEAfgAETAANc3RhbmRhcmRMZXZlbHQALExvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovc3BpL1N0YW5kYXJkTGV2ZWw7eHAAAAGQdAAESU5GT35yACpvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouc3BpLlN0YW5kYXJkTGV2ZWwAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AARJTkZPdAAAdAAJc29tZS50ZXN0cHNyABlqYXZhLnJtaS5NYXJzaGFsbGVkT2JqZWN0fL0el+1j/D4CAANJAARoYXNoWwAIbG9jQnl0ZXN0AAJbQlsACG9iakJ5dGVzcQB+ABl4cJNvO+xwdXIAAltCrPMX+AYIVOACAAB4cAAAAGms7QAFc3IALm9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5tZXNzYWdlLlNpbXBsZU1lc3NhZ2WLdE0wYLeiqAMAAUwAB21lc3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZzt4cHQAA2FiY3h0AANhYmNwdAAEbWFpbnNyADFvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h52cww1Zp7rPoCAAdJABJjb21tb25FbGVtZW50Q291bnRMAApjYXVzZVByb3h5cQB+AAhbABJleHRlbmRlZFN0YWNrVHJhY2V0AD9bTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvRXh0ZW5kZWRTdGFja1RyYWNlRWxlbWVudDtMABBsb2NhbGl6ZWRNZXNzYWdlcQB+AARMAAdtZXNzYWdlcQB+AARMAARuYW1lcQB+AARbABFzdXBwcmVzc2VkUHJveGllc3QANFtMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1wbC9UaHJvd2FibGVQcm94eTt4cAAAAABwdXIAP1tMb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW50O8rPiCOlx8+8AgAAeHAAAAAec3IAPG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuRXh0ZW5kZWRTdGFja1RyYWNlRWxlbWVudOHez7rGtpAHAgACTAAOZXh0cmFDbGFzc0luZm90ADZMb3JnL2FwYWNoZS9sb2dnaW5nL2xvZzRqL2NvcmUvaW1wbC9FeHRlbmRlZENsYXNzSW5mbztMABFzdGFja1RyYWNlRWxlbWVudHEAfgAHeHBzcgA0b3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZENsYXNzSW5mbwAAAAAAAAABAgADWgAFZXhhY3RMAAhsb2NhdGlvbnEAfgAETAAHdmVyc2lvbnEAfgAEeHABdAANdGVzdC1jbGFzc2VzL3QAAT9zcgAbamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50YQnFmiY23YUCAARJAApsaW5lTnVtYmVyTAAOZGVjbGFyaW5nQ2xhc3NxAH4ABEwACGZpbGVOYW1lcQB+AARMAAptZXRob2ROYW1lcQB+AAR4cAAAAKx0ADRvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkxvZzRqTG9nRXZlbnRUZXN0dAAWTG9nNGpMb2dFdmVudFRlc3QuamF2YXQAKnRlc3RKYXZhSW9TZXJpYWxpemFibGVXaXRoVW5rbm93blRocm93YWJsZXNxAH4AJXNxAH4AKABxAH4AK3QACDEuNy4wXzU1c3EAfgAs/////nQAJHN1bi5yZWZsZWN0Lk5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbHQAHU5hdGl2ZU1ldGhvZEFjY2Vzc29ySW1wbC5qYXZhdAAHaW52b2tlMHNxAH4AJXNxAH4AKABxAH4AK3EAfgAzc3EAfgAsAAAAOXEAfgA1cQB+ADZ0AAZpbnZva2VzcQB+ACVzcQB+ACgAcQB+ACtxAH4AM3NxAH4ALAAAACt0AChzdW4ucmVmbGVjdC5EZWxlZ2F0aW5nTWV0aG9kQWNjZXNzb3JJbXBsdAAhRGVsZWdhdGluZ01ldGhvZEFjY2Vzc29ySW1wbC5qYXZhcQB+ADtzcQB+ACVzcQB+ACgAcQB+ACtxAH4AM3NxAH4ALAAAAl50ABhqYXZhLmxhbmcucmVmbGVjdC5NZXRob2R0AAtNZXRob2QuamF2YXEAfgA7c3EAfgAlc3EAfgAoAXQADmp1bml0LTQuMTIuamFydAAENC4xMnNxAH4ALAAAADJ0AClvcmcuanVuaXQucnVubmVycy5tb2RlbC5GcmFtZXdvcmtNZXRob2QkMXQAFEZyYW1ld29ya01ldGhvZC5qYXZhdAARcnVuUmVmbGVjdGl2ZUNhbGxzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAAAx0ADNvcmcuanVuaXQuaW50ZXJuYWwucnVubmVycy5tb2RlbC5SZWZsZWN0aXZlQ2FsbGFibGV0ABdSZWZsZWN0aXZlQ2FsbGFibGUuamF2YXQAA3J1bnNxAH4AJXNxAH4AKAF0AA5qdW5pdC00LjEyLmphcnEAfgBJc3EAfgAsAAAAL3QAJ29yZy5qdW5pdC5ydW5uZXJzLm1vZGVsLkZyYW1ld29ya01ldGhvZHEAfgBMdAARaW52b2tlRXhwbG9zaXZlbHlzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAABF0ADJvcmcuanVuaXQuaW50ZXJuYWwucnVubmVycy5zdGF0ZW1lbnRzLkludm9rZU1ldGhvZHQAEUludm9rZU1ldGhvZC5qYXZhdAAIZXZhbHVhdGVzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAAUV0AB5vcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXJ0ABFQYXJlbnRSdW5uZXIuamF2YXQAB3J1bkxlYWZzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAAE50AChvcmcuanVuaXQucnVubmVycy5CbG9ja0pVbml0NENsYXNzUnVubmVydAAbQmxvY2tKVW5pdDRDbGFzc1J1bm5lci5qYXZhdAAIcnVuQ2hpbGRzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAADlxAH4AbXEAfgBucQB+AG9zcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAASJ0ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXIkM3EAfgBncQB+AFRzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAAEd0ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXIkMXEAfgBndAAIc2NoZWR1bGVzcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAASBxAH4AZnEAfgBndAALcnVuQ2hpbGRyZW5zcQB+ACVzcQB+ACgBdAAOanVuaXQtNC4xMi5qYXJxAH4ASXNxAH4ALAAAADpxAH4AZnEAfgBndAAKYWNjZXNzJDAwMHNxAH4AJXNxAH4AKAF0AA5qdW5pdC00LjEyLmphcnEAfgBJc3EAfgAsAAABDHQAIG9yZy5qdW5pdC5ydW5uZXJzLlBhcmVudFJ1bm5lciQycQB+AGdxAH4AYXNxAH4AJXNxAH4AKAF0AA5qdW5pdC00LjEyLmphcnEAfgBJc3EAfgAsAAAAGnQAMG9yZy5qdW5pdC5pbnRlcm5hbC5ydW5uZXJzLnN0YXRlbWVudHMuUnVuQmVmb3Jlc3QAD1J1bkJlZm9yZXMuamF2YXEAfgBhc3EAfgAlc3EAfgAoAXQADmp1bml0LTQuMTIuamFycQB+AElzcQB+ACwAAAAbdAAvb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMuc3RhdGVtZW50cy5SdW5BZnRlcnN0AA5SdW5BZnRlcnMuamF2YXEAfgBhc3EAfgAlc3EAfgAoAXQADmp1bml0LTQuMTIuamFycQB+AElzcQB+ACwAAAFrcQB+AGZxAH4AZ3EAfgBUc3EAfgAlc3EAfgAoAXQADmp1bml0LTQuMTIuamFycQB+AElzcQB+ACwAAACJdAAab3JnLmp1bml0LnJ1bm5lci5KVW5pdENvcmV0AA5KVW5pdENvcmUuamF2YXEAfgBUc3EAfgAlc3EAfgAoAXQADGp1bml0LXJ0LmphcnEAfgArc3EAfgAsAAAAdXQAKGNvbS5pbnRlbGxpai5qdW5pdDQuSlVuaXQ0SWRlYVRlc3RSdW5uZXJ0ABlKVW5pdDRJZGVhVGVzdFJ1bm5lci5qYXZhdAATc3RhcnRSdW5uZXJXaXRoQXJnc3NxAH4AJXNxAH4AKAF0AAxqdW5pdC1ydC5qYXJxAH4AK3NxAH4ALAAAACpxAH4AqHEAfgCpcQB+AKpzcQB+ACVzcQB+ACgBdAAManVuaXQtcnQuamFycQB+ACtzcQB+ACwAAAEGdAAsY29tLmludGVsbGlqLnJ0LmV4ZWN1dGlvbi5qdW5pdC5KVW5pdFN0YXJ0ZXJ0ABFKVW5pdFN0YXJ0ZXIuamF2YXQAFnByZXBhcmVTdHJlYW1zQW5kU3RhcnRzcQB+ACVzcQB+ACgBdAAManVuaXQtcnQuamFycQB+ACtzcQB+ACwAAABUcQB+ALNxAH4AtHQABG1haW5zcQB+ACVzcQB+ACgAcQB+ACtxAH4AM3NxAH4ALP////5xAH4ANXEAfgA2cQB+ADdzcQB+ACVzcQB+ACgAcQB+ACtxAH4AM3NxAH4ALAAAADlxAH4ANXEAfgA2cQB+ADtzcQB+ACVzcQB+ACgAcQB+ACtxAH4AM3NxAH4ALAAAACtxAH4AP3EAfgBAcQB+ADtzcQB+ACVzcQB+ACgAcQB+ACtxAH4AM3NxAH4ALAAAAl5xAH4ARHEAfgBFcQB+ADtzcQB+ACVzcQB+ACgBdAALaWRlYV9ydC5qYXJxAH4AK3NxAH4ALAAAAJN0AC1jb20uaW50ZWxsaWoucnQuZXhlY3V0aW9uLmFwcGxpY2F0aW9uLkFwcE1haW50AAxBcHBNYWluLmphdmFxAH4AunQAFk9NRyBJJ3ZlIGJlZW4gZGVsZXRlZCFxAH4AzXQARW9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuTG9nNGpMb2dFdmVudFRlc3QkRGVsZXRlZEV4Y2VwdGlvbnVyADRbTG9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuVGhyb3dhYmxlUHJveHk7+u0B4IWi6zkCAAB4cAAAAAB4";

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
    private void verifyNanoTimeWithAllConstructors(final long expected) {
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

    @SuppressWarnings("deprecation")
    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributes() {
        final Map<String, String> contextMap = new HashMap<>();
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

        assertEquals(contextMap, event.getContextMap());
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

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals("copy constructor builder", event2, event);
        assertEquals("same hashCode", event2.hashCode(), event.hashCode());
    }

    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributesInclContextData() {
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

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertEquals("copy constructor builder", event2, event);
        assertEquals("same hashCode", event2.hashCode(), event.hashCode());
    }

    @Test
    public void testBuilderCorrectlyCopiesMutableLogEvent() throws Exception {
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
        //event.setSource(stackTraceElement); // cannot be explicitly set
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
        assertEquals(true, event.isEndOfBatch());
        assertEquals(true, event.isIncludeLocation());
        assertSame(Level.FATAL, event.getLevel());
        assertSame(fqcn, event.getLoggerFqcn());
        assertSame(name, event.getLoggerName());
        assertSame(marker, event.getMarker());
        assertSame(message, event.getMessage());
        assertEquals(1234567890L, event.getNanoTime());
        //assertSame(stackTraceElement, event.getSource()); // don't invoke
        assertSame(threadName, event.getThreadName());
        assertSame(exception, event.getThrown());
        assertEquals(987654321L, event.getTimeMillis());

        final LogEvent e2 = new Log4jLogEvent.Builder(event).build();
        assertEquals(contextData, e2.getContextData());
        assertSame(contextStack, e2.getContextStack());
        assertEquals(true, e2.isEndOfBatch());
        assertEquals(true, e2.isIncludeLocation());
        assertSame(Level.FATAL, e2.getLevel());
        assertSame(fqcn, e2.getLoggerFqcn());
        assertSame(name, e2.getLoggerName());
        assertSame(marker, e2.getMarker());
        assertSame(message, e2.getMessage());
        assertEquals(1234567890L, e2.getNanoTime());
        //assertSame(stackTraceElement, e2.getSource()); // don't invoke
        assertSame(threadName, e2.getThreadName());
        assertSame(exception, e2.getThrown());
        assertEquals(987654321L, e2.getTimeMillis());

        // use reflection to get value of source field in log event copy:
        // invoking the getSource() method would initialize the field
        final Field fieldSource = Log4jLogEvent.class.getDeclaredField("source");
        fieldSource.setAccessible(true);
        final Object value = fieldSource.get(e2);
        assertNull("source in copy", value);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testEquals() {
        final Map<String, String> contextMap = new HashMap<>();
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

        assertEquals(contextMap, event.getContextMap());
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

        assertEquals(contextMap, event2.getContextMap());
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
        } catch (final NullPointerException ok) {
        }

        different("different marker", builder(event).setMarker(MarkerManager.getMarker("different")), event);
        different("null marker", builder(event).setMarker(null), event);

        different("different message", builder(event).setMessage(new ObjectMessage("different")), event);
        try { // TODO null message throws NPE in equals(). Use Objects.requireNonNull in constructor?
            different("null message", builder(event).setMessage(null), event);
            fail("Expected NullPointerException");
        } catch (final NullPointerException ok) {
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

    private static Log4jLogEvent.Builder builder(final LogEvent event) {
        return new Log4jLogEvent.Builder(event);
    }

    private void different(final String reason, final Log4jLogEvent.Builder builder, final LogEvent event) {
        final LogEvent other = builder.build();
        assertNotEquals(reason, other, event);
        assertNotEquals(reason + " hashCode", other.hashCode(), event.hashCode());
    }

    @Test
    public void testToString() {
        // Throws an NPE in 2.6.2
        assertNotNull(new Log4jLogEvent().toString());
    }
}
