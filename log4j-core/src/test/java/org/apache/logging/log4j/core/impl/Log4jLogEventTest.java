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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Base64;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.core.time.ClockFactoryTest;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableObjectMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Log4jLogEventTest {

    private static final Base64.Decoder decoder = Base64.getDecoder();

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
    public static void beforeClass() {
        System.setProperty(ClockFactory.PROPERTY_NAME, FixedTimeClock.class.getName());
    }

    @AfterAll
    public static void afterClass() throws IllegalAccessException {
        ClockFactoryTest.resetClocks();
    }

    @Test
    public void testToImmutableSame() {
        final LogEvent logEvent = new Log4jLogEvent();
        assertThat(logEvent.toImmutable()).isSameAs(logEvent);
    }

    @Test
    public void testToImmutableNotSame() {
        final LogEvent logEvent = new Log4jLogEvent.Builder().setMessage(new ReusableObjectMessage()).build();
        final LogEvent immutable = logEvent.toImmutable();
        assertThat(immutable).isSameAs(logEvent);
        assertThat(immutable.getMessage() instanceof ReusableMessage).isFalse();
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

        assertThat(evt2.getTimeMillis()).isEqualTo(evt.getTimeMillis());
        assertThat(evt2.getLoggerFqcn()).isEqualTo(evt.getLoggerFqcn());
        assertThat(evt2.getLevel()).isEqualTo(evt.getLevel());
        assertThat(evt2.getLoggerName()).isEqualTo(evt.getLoggerName());
        assertThat(evt2.getMarker()).isEqualTo(evt.getMarker());
        assertThat(evt2.getContextData()).isEqualTo(evt.getContextData());
        assertThat(evt2.getContextStack()).isEqualTo(evt.getContextStack());
        assertThat(evt2.getMessage()).isEqualTo(evt.getMessage());
        assertThat(evt2.getSource()).isEqualTo(evt.getSource());
        assertThat(evt2.getThreadName()).isEqualTo(evt.getThreadName());
        assertThat(evt2.getThrown()).isEqualTo(evt.getThrown());
        assertThat(evt2.isEndOfBatch()).isEqualTo(evt.isEndOfBatch());
        assertThat(evt2.isIncludeLocation()).isEqualTo(evt.isIncludeLocation());
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

        assertThat(evt2.getTimeMillis()).isEqualTo(evt.getTimeMillis());
        assertThat(evt2.getLoggerFqcn()).isEqualTo(evt.getLoggerFqcn());
        assertThat(evt2.getLevel()).isEqualTo(evt.getLevel());
        assertThat(evt2.getLoggerName()).isEqualTo(evt.getLoggerName());
        assertThat(evt2.getMarker()).isEqualTo(evt.getMarker());
        assertThat(evt2.getContextData()).isEqualTo(evt.getContextData());
        assertThat(evt2.getContextStack()).isEqualTo(evt.getContextStack());
        assertThat(evt2.getMessage()).isEqualTo(evt.getMessage());
        assertThat(evt2.getSource()).isEqualTo(evt.getSource());
        assertThat(evt2.getThreadName()).isEqualTo(evt.getThreadName());
        assertThat(evt2.getThrown()).isNull();
        assertThat(evt2.getThrownProxy()).isNotNull();
        assertThat(evt2.getThrownProxy()).isEqualTo(evt.getThrownProxy());
        assertThat(evt2.isEndOfBatch()).isEqualTo(evt.isEndOfBatch());
        assertThat(evt2.isIncludeLocation()).isEqualTo(evt.isIncludeLocation());
    }

    private byte[] serialize(final Log4jLogEvent event) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(event);
        return arr.toByteArray();
    }

    private Log4jLogEvent deserialize(final byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new FilteredObjectInputStream(inArr);
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

        final byte[] binaryDecoded = decoder.decode(base64);
        final Log4jLogEvent evt2 = deserialize(binaryDecoded);

        assertThat(evt2.getLoggerFqcn()).isEqualTo(loggerFQN);
        assertThat(evt2.getLevel()).isEqualTo(level);
        assertThat(evt2.getLoggerName()).isEqualTo(loggerName);
        assertThat(evt2.getMarker()).isEqualTo(marker);
        assertThat(evt2.getMessage()).isEqualTo(msg);
        assertThat(evt2.getThreadName()).isEqualTo(threadName);
        assertThat(evt2.getThrown()).isNull();
        assertThat(evt2.getThrownProxy().getName()).isEqualTo(this.getClass().getName() + "$DeletedException");
        assertThat(evt2.getThrownProxy().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void testNullLevelReplacedWithOFF() throws Exception {
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt = Log4jLogEvent.newBuilder().setLevel(NULL_LEVEL).build();
        assertThat(evt.getLevel()).isEqualTo(Level.OFF);
    }

    @Test
    public void testTimestampGeneratedByClock() {
        final LogEvent evt = Log4jLogEvent.newBuilder().build();
        assertThat(evt.getTimeMillis()).isEqualTo(FixedTimeClock.FIXED_TIME);
    }

    @Test
    public void testInitiallyDummyNanoClock() {
        assertThat(Log4jLogEvent.getNanoClock() instanceof DummyNanoClock).isTrue();
        assertThat(Log4jLogEvent.getNanoClock().nanoTime()).describedAs("initial dummy nanotime").isEqualTo(0);
    }

    @Test
    public void testNanoTimeGeneratedByNanoClock() {
        Log4jLogEvent.setNanoClock(new DummyNanoClock(123));
        verifyNanoTimeWithAllConstructors(123);
        Log4jLogEvent.setNanoClock(new DummyNanoClock(87654));
        verifyNanoTimeWithAllConstructors(87654);
    }

    private void verifyNanoTimeWithAllConstructors(final long expected) {
        assertThat(Log4jLogEvent.getNanoClock().nanoTime()).isEqualTo(expected);

        assertThat(new Log4jLogEvent().getNanoTime()).describedAs("No-arg constructor").isEqualTo(expected);
        assertThat(new Log4jLogEvent("l", null, "a", null, null, null, null)
                .getNanoTime()).describedAs("7-arg constructor").isEqualTo(expected);
    }

    @Test
    public void testBuilderCorrectlyCopiesAllEventAttributes() {
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

        assertThat(event.getContextData()).isEqualTo(contextData);
        assertThat(event.getContextStack()).isSameAs(contextStack);
        assertThat(event.isEndOfBatch()).isTrue();
        assertThat(event.isIncludeLocation()).isTrue();
        assertThat(event.getLevel()).isSameAs(Level.FATAL);
        assertThat(event.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(event.getLoggerName()).isSameAs(name);
        assertThat(event.getMarker()).isSameAs(marker);
        assertThat(event.getMessage()).isSameAs(message);
        assertThat(event.getNanoTime()).isEqualTo(1234567890L);
        assertThat(event.getSource()).isSameAs(stackTraceElement);
        assertThat(event.getThreadName()).isSameAs(threadName);
        assertThat(event.getThrown()).isSameAs(exception);
        assertThat(event.getTimeMillis()).isEqualTo(987654321L);

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertThat(event).describedAs("copy constructor builder").isEqualTo(event2);
        assertThat(event.hashCode()).describedAs("same hashCode").isEqualTo(event2.hashCode());
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

        assertThat(event.getContextData()).isSameAs(contextData);
        assertThat(event.getContextStack()).isSameAs(contextStack);
        assertThat(event.isEndOfBatch()).isTrue();
        assertThat(event.isIncludeLocation()).isTrue();
        assertThat(event.getLevel()).isSameAs(Level.FATAL);
        assertThat(event.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(event.getLoggerName()).isSameAs(name);
        assertThat(event.getMarker()).isSameAs(marker);
        assertThat(event.getMessage()).isSameAs(message);
        assertThat(event.getNanoTime()).isEqualTo(1234567890L);
        assertThat(event.getSource()).isSameAs(stackTraceElement);
        assertThat(event.getThreadName()).isSameAs(threadName);
        assertThat(event.getThrown()).isSameAs(exception);
        assertThat(event.getTimeMillis()).isEqualTo(987654321L);

        final LogEvent event2 = new Log4jLogEvent.Builder(event).build();
        assertThat(event).describedAs("copy constructor builder").isEqualTo(event2);
        assertThat(event.hashCode()).describedAs("same hashCode").isEqualTo(event2.hashCode());
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

        assertThat(event.getContextData()).isSameAs(contextData);
        assertThat(event.getContextStack()).isSameAs(contextStack);
        assertThat(event.isEndOfBatch()).isTrue();
        assertThat(event.isIncludeLocation()).isTrue();
        assertThat(event.getLevel()).isSameAs(Level.FATAL);
        assertThat(event.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(event.getLoggerName()).isSameAs(name);
        assertThat(event.getMarker()).isSameAs(marker);
        assertThat(event.getMessage()).isSameAs(message);
        assertThat(event.getNanoTime()).isEqualTo(1234567890L);
        //assertSame(stackTraceElement, event.getSource()); // don't invoke
        assertThat(event.getThreadName()).isSameAs(threadName);
        assertThat(event.getThrown()).isSameAs(exception);
        assertThat(event.getTimeMillis()).isEqualTo(987654321L);

        final LogEvent e2 = new Log4jLogEvent.Builder(event).build();
        assertThat(e2.getContextData()).isEqualTo(contextData);
        assertThat(e2.getContextStack()).isSameAs(contextStack);
        assertThat(e2.isEndOfBatch()).isTrue();
        assertThat(e2.isIncludeLocation()).isTrue();
        assertThat(e2.getLevel()).isSameAs(Level.FATAL);
        assertThat(e2.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(e2.getLoggerName()).isSameAs(name);
        assertThat(e2.getMarker()).isSameAs(marker);
        assertThat(e2.getMessage()).isSameAs(message);
        assertThat(e2.getNanoTime()).isEqualTo(1234567890L);
        //assertSame(stackTraceElement, e2.getSource()); // don't invoke
        assertThat(e2.getThreadName()).isSameAs(threadName);
        assertThat(e2.getThrown()).isSameAs(exception);
        assertThat(e2.getTimeMillis()).isEqualTo(987654321L);

        // use reflection to get value of source field in log event copy:
        // invoking the getSource() method would initialize the field
        final Field fieldSource = Log4jLogEvent.class.getDeclaredField("source");
        fieldSource.setAccessible(true);
        final Object value = fieldSource.get(e2);
        assertThat(value).describedAs("source in copy").isNull();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testEquals() {
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

        assertThat(event.getContextData()).isEqualTo(contextData);
        assertThat(event.getContextStack()).isSameAs(contextStack);
        assertThat(event.isEndOfBatch()).isTrue();
        assertThat(event.isIncludeLocation()).isTrue();
        assertThat(event.getLevel()).isSameAs(Level.FATAL);
        assertThat(event.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(event.getLoggerName()).isSameAs(name);
        assertThat(event.getMarker()).isSameAs(marker);
        assertThat(event.getMessage()).isSameAs(message);
        assertThat(event.getNanoTime()).isEqualTo(1234567890L);
        assertThat(event.getSource()).isSameAs(stackTraceElement);
        assertThat(event.getThreadName()).isSameAs(threadName);
        assertThat(event.getThrown()).isSameAs(exception);
        assertThat(event.getTimeMillis()).isEqualTo(987654321L);

        final LogEvent event2 = builder(event).build();
        assertThat(event).describedAs("copy constructor builder").isEqualTo(event2);
        assertThat(event.hashCode()).describedAs("same hashCode").isEqualTo(event2.hashCode());

        assertThat(event2.getContextData()).isEqualTo(contextData);
        assertThat(event2.getContextStack()).isSameAs(contextStack);
        assertThat(event2.isEndOfBatch()).isTrue();
        assertThat(event2.isIncludeLocation()).isTrue();
        assertThat(event2.getLevel()).isSameAs(Level.FATAL);
        assertThat(event2.getLoggerFqcn()).isSameAs(fqcn);
        assertThat(event2.getLoggerName()).isSameAs(name);
        assertThat(event2.getMarker()).isSameAs(marker);
        assertThat(event2.getMessage()).isSameAs(message);
        assertThat(event2.getNanoTime()).isEqualTo(1234567890L);
        assertThat(event2.getSource()).isSameAs(stackTraceElement);
        assertThat(event2.getThreadName()).isSameAs(threadName);
        assertThat(event2.getThrown()).isSameAs(exception);
        assertThat(event2.getTimeMillis()).isEqualTo(987654321L);

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
        assertThatThrownBy(() -> different("null name", builder(event).setLoggerName(null), event)).isInstanceOf(NullPointerException.class);

        different("different marker", builder(event).setMarker(MarkerManager.getMarker("different")), event);
        different("null marker", builder(event).setMarker(null), event);

        different("different message", builder(event).setMessage(new ObjectMessage("different")), event);
        assertThatThrownBy(() -> different("null message", builder(event).setMessage(null), event)).isInstanceOf(NullPointerException.class);

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
        assertThat(event).describedAs(reason).isNotEqualTo(other);
        assertThat(event.hashCode()).describedAs(reason + " hashCode").isNotEqualTo(other.hashCode());
    }

    @Test
    public void testToString() {
        // Throws an NPE in 2.6.2
        assertThat(new Log4jLogEvent().toString()).isNotNull();
    }
}
