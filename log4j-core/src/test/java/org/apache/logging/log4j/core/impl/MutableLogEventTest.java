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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the MutableLogEvent class.
 */
public class MutableLogEventTest {
    private static final StringMap CONTEXT_DATA = createContextData();
    private static final ThreadContext.ContextStack STACK = new MutableThreadContextStack(Arrays.asList("abc", "xyz"));

    static boolean useObjectInputStream = false;

    private static StringMap createContextData() {
        final StringMap result = new SortedArrayStringMap();
        result.putValue("a", "1");
        result.putValue("b", "2");
        return result;
    }

    @BeforeAll
    public static void setupClass() {
        try {
            Class.forName("java.io.ObjectInputFilter");
            useObjectInputStream = true;
        } catch (final ClassNotFoundException ex) {
            // Ignore the exception
        }
    }

    @Test
    public void testToImmutable() {
        final LogEvent logEvent = new MutableLogEvent();
        assertNotSame(logEvent, logEvent.toImmutable());
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
        assertEquals(CONTEXT_DATA, mutable.getContextData(), "contextMap");
        assertEquals(STACK, mutable.getContextStack(), "stack");
        assertTrue(mutable.isEndOfBatch(), "endOfBatch");
        assertTrue(mutable.isIncludeLocation(), "IncludeLocation()");
        assertEquals(Level.FATAL, mutable.getLevel(), "level");
        assertEquals(source.getLoggerFqcn(), mutable.getLoggerFqcn(), "LoggerFqcn()");
        assertEquals(source.getLoggerName(), mutable.getLoggerName(), "LoggerName");
        assertEquals(source.getMarker(), mutable.getMarker(), "marker");
        assertEquals(source.getMessage(), mutable.getMessage(), "msg");
        assertEquals(source.getNanoTime(), mutable.getNanoTime(), "nano");
        assertEquals(source.getSource(), mutable.getSource(), "src");
        assertEquals(source.getThreadId(), mutable.getThreadId(), "tid");
        assertEquals(source.getThreadName(), mutable.getThreadName(), "tname");
        assertEquals(source.getThreadPriority(), mutable.getThreadPriority(), "tpriority");
        assertEquals(source.getThrown(), mutable.getThrown(), "throwns");
        assertEquals(source.getThrownProxy(), mutable.getThrownProxy(), "proxy");
        assertEquals(source.getTimeMillis(), mutable.getTimeMillis(), "millis");
    }

    @Test
    public void testInitFromReusableCopiesFormatString() {
        final Message message = ReusableMessageFactory.INSTANCE.newMessage("msg in a {}", "bottle");
        final Log4jLogEvent source = Log4jLogEvent.newBuilder() //
                .setContextData(CONTEXT_DATA) //
                .setContextStack(STACK) //
                .setEndOfBatch(true) //
                .setIncludeLocation(true) //
                .setLevel(Level.FATAL) //
                .setLoggerFqcn("a.b.c.d.e") //
                .setLoggerName("my name is Logger") //
                .setMarker(MarkerManager.getMarker("on your marks")) //
                .setMessage(message) //
                .setNanoTime(1234567) //
                .setSource(new StackTraceElement("myclass", "mymethod", "myfile", 123)) //
                .setThreadId(100).setThreadName("threadname").setThreadPriority(10) //
                .setThrown(new RuntimeException("run")) //
                .setTimeMillis(987654321)
                .build();
        final MutableLogEvent mutable = new MutableLogEvent();
        mutable.initFrom(source);
        assertEquals("msg in a {}", mutable.getFormat(), "format");
        assertEquals("msg in a bottle", mutable.getFormattedMessage(), "formatted");
        assertArrayEquals(new String[] {"bottle"}, mutable.getParameters(), "parameters");
        Message memento = mutable.memento();
        assertEquals("msg in a {}", memento.getFormat(), "format");
        assertEquals("msg in a bottle", memento.getFormattedMessage(), "formatted");
        assertArrayEquals(new String[] {"bottle"}, memento.getParameters(), "parameters");

        Message eventMementoMessage = mutable.createMemento().getMessage();
        assertEquals("msg in a {}", eventMementoMessage.getFormat(), "format");
        assertEquals("msg in a bottle", eventMementoMessage.getFormattedMessage(), "formatted");
        assertArrayEquals(new String[] {"bottle"}, eventMementoMessage.getParameters(), "parameters");

        Message log4JLogEventMessage = new Log4jLogEvent.Builder(mutable).build().getMessage();
        assertEquals("msg in a {}", log4JLogEventMessage.getFormat(), "format");
        assertEquals("msg in a bottle", log4JLogEventMessage.getFormattedMessage(), "formatted");
        assertArrayEquals(new String[] {"bottle"}, log4JLogEventMessage.getParameters(), "parameters");
    }

    @Test
    public void testInitFromReusableObjectCopiesParameter() {
        final Object param = new Object();
        final Message message = ReusableMessageFactory.INSTANCE.newMessage(param);
        final Log4jLogEvent source = Log4jLogEvent.newBuilder()
                .setContextData(CONTEXT_DATA)
                .setContextStack(STACK)
                .setEndOfBatch(true)
                .setIncludeLocation(true)
                .setLevel(Level.FATAL)
                .setLoggerFqcn("a.b.c.d.e")
                .setLoggerName("my name is Logger")
                .setMarker(MarkerManager.getMarker("on your marks"))
                .setMessage(message)
                .setNanoTime(1234567)
                .setSource(new StackTraceElement("myclass", "mymethod", "myfile", 123))
                .setThreadId(100).setThreadName("threadname")
                .setThreadPriority(10)
                .setThrown(new RuntimeException("run"))
                .setTimeMillis(987654321)
                .build();
        final MutableLogEvent mutable = new MutableLogEvent();
        mutable.initFrom(source);
        assertNull(mutable.getFormat(), "format");
        assertEquals(param.toString(), mutable.getFormattedMessage(), "formatted");
        assertArrayEquals(new Object[] {param}, mutable.getParameters(), "parameters");
        Message memento = mutable.memento();
        assertNull(memento.getFormat(), "format");
        assertEquals(param.toString(), memento.getFormattedMessage(), "formatted");
        assertArrayEquals(new Object[] {param}, memento.getParameters(), "parameters");
    }

    @Test
    public void testClear() {
        final MutableLogEvent mutable = new MutableLogEvent();
        // initialize the event with an empty message
        ReusableSimpleMessage simpleMessage = new ReusableSimpleMessage();
        simpleMessage.set("");
        mutable.setMessage(simpleMessage);
        assertEquals(0, mutable.getContextData().size(), "context data");
        assertNull(mutable.getContextStack(), "context stack");
        assertFalse(mutable.isEndOfBatch(), "end of batch");
        assertFalse(mutable.isIncludeLocation(), "incl loc");
        assertSame(Level.OFF, mutable.getLevel(), "level");
        assertNull(mutable.getLoggerFqcn(), "fqcn");
        assertNull(mutable.getLoggerName(), "logger");
        assertNull(mutable.getMarker(), "marker");
        assertEquals(mutable, mutable.getMessage(), "msg");
        assertEquals(0, mutable.getNanoTime(), "nanoTm");
        assertEquals(0, mutable.getThreadId(), "tid");
        assertNull(mutable.getThreadName(), "tname");
        assertEquals(0, mutable.getThreadPriority(), "tpriority");
        assertNull(mutable.getThrown(), "thrwn");
        assertEquals(0, mutable.getTimeMillis(), "timeMs");

        assertNull(mutable.getSource(), "source");
        assertNull(mutable.getThrownProxy(), "thrownProxy");

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

        assertNotNull(mutable.getContextStack(), "context stack");
        assertTrue(mutable.isEndOfBatch(), "end of batch");
        assertTrue(mutable.isIncludeLocation(), "incl loc");
        assertNotNull(mutable.getLevel(), "level");
        assertNotNull(mutable.getLoggerFqcn(), "fqcn");
        assertNotNull(mutable.getLoggerName(), "logger");
        assertNotNull(mutable.getMarker(), "marker");
        assertEquals(new ParameterizedMessage("message in a {}", "bottle"), mutable.getMessage(), "msg");
        assertNotEquals(0, mutable.getNanoTime(), "nanoTm");
        assertNotEquals(0, mutable.getThreadId(), "tid");
        assertNotNull(mutable.getThreadName(), "tname");
        assertNotEquals(0, mutable.getThreadPriority(), "tpriority");
        assertNotNull(mutable.getThrown(), "thrwn");
        assertNotEquals(0, mutable.getTimeMillis(), "timeMs");

        assertNotNull(mutable.getSource(), "source");
        assertNotNull(mutable.getThrownProxy(), "thrownProxy");

        mutable.clear();
        assertEquals(0, mutable.getContextData().size(), "context map");
        assertNull(mutable.getContextStack(), "context stack");
        assertSame(Level.OFF, mutable.getLevel(), "level");
        assertNull(mutable.getLoggerFqcn(), "fqcn");
        assertNull(mutable.getLoggerName(), "logger");
        assertNull(mutable.getMarker(), "marker");
        assertEquals(mutable, mutable.getMessage(), "msg");
        assertNull(mutable.getThrown(), "thrwn");

        assertNull(mutable.getSource(), "source");
        assertNull(mutable.getThrownProxy(), "thrownProxy");

        // primitive fields are NOT reset:
        assertTrue(mutable.isEndOfBatch(), "end of batch");
        assertTrue(mutable.isIncludeLocation(), "incl loc");
        assertNotEquals(0, mutable.getNanoTime(), "nanoTm");
        assertNotEquals(0, mutable.getTimeMillis(), "timeMs");

        // thread-local fields are NOT reset:
        assertNotEquals(0, mutable.getThreadId(), "tid");
        assertNotNull(mutable.getThreadName(), "tname");
        assertNotEquals(0, mutable.getThreadPriority(), "tpriority");
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
        final ObjectInputStream in = useObjectInputStream ? new ObjectInputStream(inArr) :
                new FilteredObjectInputStream(inArr);
        final Log4jLogEvent result = (Log4jLogEvent) in.readObject();
        return result;
    }


}
