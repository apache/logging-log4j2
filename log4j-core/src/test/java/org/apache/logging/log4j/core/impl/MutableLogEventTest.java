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
import java.util.Arrays;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the MutableLogEvent class.
 */
public class MutableLogEventTest {
    private static final StringMap CONTEXT_DATA = createContextData();
    private static final ThreadContext.ContextStack STACK = new MutableThreadContextStack(Arrays.asList("abc", "xyz"));

    private static StringMap createContextData() {
        final StringMap result = new SortedArrayStringMap();
        result.putValue("a", "1");
        result.putValue("b", "2");
        return result;
    }

    @Test
    public void testToImmutable() {
        final LogEvent logEvent = new MutableLogEvent();
        Assert.assertNotSame(logEvent, logEvent.toImmutable());
    }

    @Test
    public void testInitFromCopiesAllFields() {
//        private ThrowableProxy thrownProxy;
        final Log4jLogEvent source = Log4jLogEvent.newBuilder() //
                .setContextData(CONTEXT_DATA) //
                .setContextStack(STACK) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn("a.b.c.d.e") //
                .setLoggerName("my name is Logger") //
                .setMarker(MarkerManager.getMarker("on your marks")) //
                .setMessage(new SimpleMessage("msg in a bottle")) //
                .setNanoTime(1234567) //
                .setSource(new StackTraceElement("myclass", "mymethod", "myfile", 123)) //
                .setThreadId(100).setThreadName("threadname").setThreadPriority(10) //
                .setThrown(new RuntimeException("run")) //
                .setTimeMillis(987654321)
                .build();
        final MutableLogEvent mutable = new MutableLogEvent();
        mutable.initFrom(source);
        assertEquals("contextMap", CONTEXT_DATA, mutable.getContextData());
        assertEquals("stack", STACK, mutable.getContextStack());
        assertEquals("endOfBatch", true, mutable.isEndOfBatch());
        assertEquals("IncludeLocation()", true, mutable.isIncludeLocation());
        assertEquals("level", Level.FATAL, mutable.getLevel());
        assertEquals("LoggerFqcn()", source.getLoggerFqcn(), mutable.getLoggerFqcn());
        assertEquals("LoggerName", source.getLoggerName(), mutable.getLoggerName());
        assertEquals("marker", source.getMarker(), mutable.getMarker());
        assertEquals("msg", source.getMessage(), mutable.getMessage());
        assertEquals("nano", source.getNanoTime(), mutable.getNanoTime());
        assertEquals("src", source.getSource(), mutable.getSource());
        assertEquals("tid", source.getThreadId(), mutable.getThreadId());
        assertEquals("tname", source.getThreadName(), mutable.getThreadName());
        assertEquals("tpriority", source.getThreadPriority(), mutable.getThreadPriority());
        assertEquals("throwns", source.getThrown(), mutable.getThrown());
        assertEquals("proxy", source.getThrownProxy(), mutable.getThrownProxy());
        assertEquals("millis", source.getTimeMillis(), mutable.getTimeMillis());
    }

    @Test
    public void testClear() {
        final MutableLogEvent mutable = new MutableLogEvent();
        assertEquals("context data", 0, mutable.getContextData().size());
        assertNull("context stack", mutable.getContextStack());
        assertFalse("end of batch", mutable.isEndOfBatch());
        assertFalse("incl loc", mutable.isIncludeLocation());
        assertSame("level", Level.OFF, mutable.getLevel());
        assertNull("fqcn", mutable.getLoggerFqcn());
        assertNull("logger", mutable.getLoggerName());
        assertNull("marker", mutable.getMarker());
        assertEquals("msg", mutable, mutable.getMessage());
        assertEquals("nanoTm", 0, mutable.getNanoTime());
        assertEquals("tid", 0, mutable.getThreadId());
        assertNull("tname", mutable.getThreadName());
        assertEquals("tpriority", 0, mutable.getThreadPriority());
        assertNull("thrwn", mutable.getThrown());
        assertEquals("timeMs", 0, mutable.getTimeMillis());

        assertNull("source", mutable.getSource());
        assertNull("thrownProxy", mutable.getThrownProxy());

        mutable.setContextData(CONTEXT_DATA);
        mutable.setContextStack(STACK);
        mutable.setEndOfBatch(true);
        mutable.setIncludeLocation(true);
        mutable.setLevel(Level.WARN);
        mutable.setLoggerFqcn(getClass().getName());
        mutable.setLoggerName("loggername");
        mutable.setMarker(MarkerManager.getMarker("marked man"));
        mutable.setMessage(new ParameterizedMessage("message in a {}", "bottle"));
        mutable.setNanoTime(1234);
        mutable.setThreadId(987);
        mutable.setThreadName("ito");
        mutable.setThreadPriority(9);
        mutable.setThrown(new Exception());
        mutable.setTimeMillis(56789);

        assertNotNull("context map", mutable.getContextMap());
        assertNotNull("context stack", mutable.getContextStack());
        assertTrue("end of batch", mutable.isEndOfBatch());
        assertTrue("incl loc", mutable.isIncludeLocation());
        assertNotNull("level", mutable.getLevel());
        assertNotNull("fqcn", mutable.getLoggerFqcn());
        assertNotNull("logger", mutable.getLoggerName());
        assertNotNull("marker", mutable.getMarker());
        assertEquals("msg", new ParameterizedMessage("message in a {}", "bottle"), mutable.getMessage());
        assertNotEquals("nanoTm", 0, mutable.getNanoTime());
        assertNotEquals("tid", 0, mutable.getThreadId());
        assertNotNull("tname", mutable.getThreadName());
        assertNotEquals("tpriority", 0, mutable.getThreadPriority());
        assertNotNull("thrwn", mutable.getThrown());
        assertNotEquals("timeMs", 0, mutable.getTimeMillis());

        assertNotNull("source", mutable.getSource());
        assertNotNull("thrownProxy", mutable.getThrownProxy());

        mutable.clear();
        assertEquals("context map", 0, mutable.getContextData().size());
        assertNull("context stack", mutable.getContextStack());
        assertSame("level", Level.OFF, mutable.getLevel());
        assertNull("fqcn", mutable.getLoggerFqcn());
        assertNull("logger", mutable.getLoggerName());
        assertNull("marker", mutable.getMarker());
        assertEquals("msg", mutable, mutable.getMessage());
        assertNull("thrwn", mutable.getThrown());

        assertNull("source", mutable.getSource());
        assertNull("thrownProxy", mutable.getThrownProxy());

        // primitive fields are NOT reset:
        assertTrue("end of batch", mutable.isEndOfBatch());
        assertTrue("incl loc", mutable.isIncludeLocation());
        assertNotEquals("nanoTm", 0, mutable.getNanoTime());
        assertNotEquals("timeMs", 0, mutable.getTimeMillis());

        // thread-local fields are NOT reset:
        assertNotEquals("tid", 0, mutable.getThreadId());
        assertNotNull("tname", mutable.getThreadName());
        assertNotEquals("tpriority", 0, mutable.getThreadPriority());
    }

    @Test
    public void testJavaIoSerializable() throws Exception {
        final MutableLogEvent evt = new MutableLogEvent();
        evt.setContextData(CONTEXT_DATA);
        evt.setContextStack(STACK);
        evt.setEndOfBatch(true);
        evt.setIncludeLocation(true);
        evt.setLevel(Level.WARN);
        evt.setLoggerFqcn(getClass().getName());
        evt.setLoggerName("loggername");
        evt.setMarker(MarkerManager.getMarker("marked man"));
        //evt.setMessage(new ParameterizedMessage("message in a {}", "bottle")); // TODO ParameterizedMessage serialization
        evt.setMessage(new SimpleMessage("peace for all"));
        evt.setNanoTime(1234);
        evt.setThreadId(987);
        evt.setThreadName("ito");
        evt.setThreadPriority(9);
        evt.setTimeMillis(56789);

        final byte[] binary = serialize(evt);
        final Log4jLogEvent evt2 = deserialize(binary);

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFqcn(), evt2.getLoggerFqcn());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextData(), evt2.getContextData());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
        assertEquals(evt.getContextStack(), evt2.getContextStack());
        assertEquals(evt.getMessage(), evt2.getMessage());
        assertNotNull(evt2.getSource());
        assertEquals(evt.getSource(), evt2.getSource());
        assertEquals(evt.getThreadName(), evt2.getThreadName());
        assertNull(evt2.getThrown());
        assertNull(evt2.getThrownProxy());
        assertEquals(evt.isEndOfBatch(), evt2.isEndOfBatch());
        assertEquals(evt.isIncludeLocation(), evt2.isIncludeLocation());

        assertNotEquals(evt.getNanoTime(), evt2.getNanoTime()); // nano time is transient in log4j log event
        assertEquals(0, evt2.getNanoTime());
    }

    @Test
    public void testJavaIoSerializableWithThrown() throws Exception {
        new InternalError("test error");
        final MutableLogEvent evt = new MutableLogEvent();
        evt.setContextData(CONTEXT_DATA);
        evt.setContextStack(STACK);
        evt.setEndOfBatch(true);
        evt.setIncludeLocation(true);
        evt.setLevel(Level.WARN);
        evt.setLoggerFqcn(getClass().getName());
        evt.setLoggerName("loggername");
        evt.setMarker(MarkerManager.getMarker("marked man"));
        //evt.setMessage(new ParameterizedMessage("message in a {}", "bottle")); // TODO ParameterizedMessage serialization
        evt.setMessage(new SimpleMessage("peace for all"));
        evt.setNanoTime(1234);
        evt.setThreadId(987);
        evt.setThreadName("ito");
        evt.setThreadPriority(9);
        evt.setThrown(new Exception());
        evt.setTimeMillis(56789);

        final byte[] binary = serialize(evt);
        final Log4jLogEvent evt2 = deserialize(binary);

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFqcn(), evt2.getLoggerFqcn());
        assertEquals(evt.getLevel(), evt2.getLevel());
        assertEquals(evt.getLoggerName(), evt2.getLoggerName());
        assertEquals(evt.getMarker(), evt2.getMarker());
        assertEquals(evt.getContextData(), evt2.getContextData());
        assertEquals(evt.getContextMap(), evt2.getContextMap());
        assertEquals(evt.getContextStack(), evt2.getContextStack());
        assertEquals(evt.getMessage(), evt2.getMessage());
        assertNotNull(evt2.getSource());
        assertEquals(evt.getSource(), evt2.getSource());
        assertEquals(evt.getThreadName(), evt2.getThreadName());
        assertNull(evt2.getThrown());
        assertNotNull(evt2.getThrownProxy());
        assertEquals(evt.getThrownProxy(), evt2.getThrownProxy());
        assertEquals(evt.isEndOfBatch(), evt2.isEndOfBatch());
        assertEquals(evt.isIncludeLocation(), evt2.isIncludeLocation());

        assertNotEquals(evt.getNanoTime(), evt2.getNanoTime()); // nano time is transient in log4j log event
        assertEquals(0, evt2.getNanoTime());
    }

    private byte[] serialize(final MutableLogEvent event) throws IOException {
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


}