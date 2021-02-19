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
import static org.junit.jupiter.api.Assertions.*;

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
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        } catch (ClassNotFoundException ex) {
            // Ignore the exception
        }
    }

    @Test
    public void testToImmutable() {
        final LogEvent logEvent = new MutableLogEvent();
        assertThat(logEvent.toImmutable()).isNotSameAs(logEvent);
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
        assertThat(mutable.getContextData()).describedAs("contextMap").isEqualTo(CONTEXT_DATA);
        assertThat(mutable.getContextStack()).describedAs("stack").isEqualTo(STACK);
        assertTrue(mutable.isEndOfBatch(), "endOfBatch");
        assertTrue(mutable.isIncludeLocation(), "IncludeLocation()");
        assertThat(mutable.getLevel()).describedAs("level").isEqualTo(Level.FATAL);
        assertThat(mutable.getLoggerFqcn()).describedAs("LoggerFqcn()").isEqualTo(source.getLoggerFqcn());
        assertThat(mutable.getLoggerName()).describedAs("LoggerName").isEqualTo(source.getLoggerName());
        assertThat(mutable.getMarker()).describedAs("marker").isEqualTo(source.getMarker());
        assertThat(mutable.getMessage()).describedAs("msg").isEqualTo(source.getMessage());
        assertThat(mutable.getNanoTime()).describedAs("nano").isEqualTo(source.getNanoTime());
        assertThat(mutable.getSource()).describedAs("src").isEqualTo(source.getSource());
        assertThat(mutable.getThreadId()).describedAs("tid").isEqualTo(source.getThreadId());
        assertThat(mutable.getThreadName()).describedAs("tname").isEqualTo(source.getThreadName());
        assertThat(mutable.getThreadPriority()).describedAs("tpriority").isEqualTo(source.getThreadPriority());
        assertThat(mutable.getThrown()).describedAs("throwns").isEqualTo(source.getThrown());
        assertThat(mutable.getThrownProxy()).describedAs("proxy").isEqualTo(source.getThrownProxy());
        assertThat(mutable.getTimeMillis()).describedAs("millis").isEqualTo(source.getTimeMillis());
    }

    @Test
    public void testInitFromReusableCopiesFormatString() {
        Message message = ReusableMessageFactory.INSTANCE.newMessage("msg in a {}", "bottle");
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
        assertThat(mutable.getFormat()).describedAs("format").isEqualTo("msg in a {}");
        assertThat(mutable.getFormattedMessage()).describedAs("formatted").isEqualTo("msg in a bottle");
        assertThat(mutable.getParameters()).describedAs("parameters").isEqualTo(new String[] {"bottle"});
        Message memento = mutable.memento();
        assertThat(memento.getFormat()).describedAs("format").isEqualTo("msg in a {}");
        assertThat(memento.getFormattedMessage()).describedAs("formatted").isEqualTo("msg in a bottle");
        assertThat(memento.getParameters()).describedAs("parameters").isEqualTo(new String[] {"bottle"});

        Message eventMementoMessage = mutable.createMemento().getMessage();
        assertThat(eventMementoMessage.getFormat()).describedAs("format").isEqualTo("msg in a {}");
        assertThat(eventMementoMessage.getFormattedMessage()).describedAs("formatted").isEqualTo("msg in a bottle");
        assertThat(eventMementoMessage.getParameters()).describedAs("parameters").isEqualTo(new String[] {"bottle"});

        Message log4JLogEventMessage = new Log4jLogEvent.Builder(mutable).build().getMessage();
        assertThat(log4JLogEventMessage.getFormat()).describedAs("format").isEqualTo("msg in a {}");
        assertThat(log4JLogEventMessage.getFormattedMessage()).describedAs("formatted").isEqualTo("msg in a bottle");
        assertThat(log4JLogEventMessage.getParameters()).describedAs("parameters").isEqualTo(new String[] {"bottle"});
    }

    @Test
    public void testInitFromReusableObjectCopiesParameter() {
        Object param = new Object();
        Message message = ReusableMessageFactory.INSTANCE.newMessage(param);
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
        assertThat(mutable.getFormat()).describedAs("format").isNull();
        assertThat(mutable.getFormattedMessage()).describedAs("formatted").isEqualTo(param.toString());
        assertThat(mutable.getParameters()).describedAs("parameters").isEqualTo(new Object[] {param});
        Message memento = mutable.memento();
        assertThat(memento.getFormat()).describedAs("format").isNull();
        assertThat(memento.getFormattedMessage()).describedAs("formatted").isEqualTo(param.toString());
        assertThat(memento.getParameters()).describedAs("parameters").isEqualTo(new Object[] {param});
    }

    @Test
    public void testClear() {
        final MutableLogEvent mutable = new MutableLogEvent();
        // initialize the event with an empty message
        ReusableSimpleMessage simpleMessage = new ReusableSimpleMessage();
        simpleMessage.set("");
        mutable.setMessage(simpleMessage);
        assertThat(mutable.getContextData().size()).describedAs("context data").isEqualTo(0);
        assertThat(mutable.getContextStack()).describedAs("context stack").isNull();
        assertFalse(mutable.isEndOfBatch(), "end of batch");
        assertFalse(mutable.isIncludeLocation(), "incl loc");
        assertThat(mutable.getLevel()).describedAs("level").isSameAs(Level.OFF);
        assertThat(mutable.getLoggerFqcn()).describedAs("fqcn").isNull();
        assertThat(mutable.getLoggerName()).describedAs("logger").isNull();
        assertThat(mutable.getMarker()).describedAs("marker").isNull();
        assertThat(mutable.getMessage()).describedAs("msg").isEqualTo(mutable);
        assertThat(mutable.getNanoTime()).describedAs("nanoTm").isEqualTo(0);
        assertThat(mutable.getThreadId()).describedAs("tid").isEqualTo(0);
        assertThat(mutable.getThreadName()).describedAs("tname").isNull();
        assertThat(mutable.getThreadPriority()).describedAs("tpriority").isEqualTo(0);
        assertThat(mutable.getThrown()).describedAs("thrwn").isNull();
        assertThat(mutable.getTimeMillis()).describedAs("timeMs").isEqualTo(0);

        assertThat(mutable.getSource()).describedAs("source").isNull();
        assertThat(mutable.getThrownProxy()).describedAs("thrownProxy").isNull();

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

        assertThat(mutable.getContextStack()).describedAs("context stack").isNotNull();
        assertTrue(mutable.isEndOfBatch(), "end of batch");
        assertTrue(mutable.isIncludeLocation(), "incl loc");
        assertThat(mutable.getLevel()).describedAs("level").isNotNull();
        assertThat(mutable.getLoggerFqcn()).describedAs("fqcn").isNotNull();
        assertThat(mutable.getLoggerName()).describedAs("logger").isNotNull();
        assertThat(mutable.getMarker()).describedAs("marker").isNotNull();
        assertThat(mutable.getMessage()).describedAs("msg").isEqualTo(new ParameterizedMessage("message in a {}", "bottle"));
        assertThat(mutable.getNanoTime()).describedAs("nanoTm").isNotEqualTo(0);
        assertThat(mutable.getThreadId()).describedAs("tid").isNotEqualTo(0);
        assertThat(mutable.getThreadName()).describedAs("tname").isNotNull();
        assertThat(mutable.getThreadPriority()).describedAs("tpriority").isNotEqualTo(0);
        assertThat(mutable.getThrown()).describedAs("thrwn").isNotNull();
        assertThat(mutable.getTimeMillis()).describedAs("timeMs").isNotEqualTo(0);

        assertThat(mutable.getSource()).describedAs("source").isNotNull();
        assertThat(mutable.getThrownProxy()).describedAs("thrownProxy").isNotNull();

        mutable.clear();
        assertThat(mutable.getContextData().size()).describedAs("context map").isEqualTo(0);
        assertThat(mutable.getContextStack()).describedAs("context stack").isNull();
        assertThat(mutable.getLevel()).describedAs("level").isSameAs(Level.OFF);
        assertThat(mutable.getLoggerFqcn()).describedAs("fqcn").isNull();
        assertThat(mutable.getLoggerName()).describedAs("logger").isNull();
        assertThat(mutable.getMarker()).describedAs("marker").isNull();
        assertThat(mutable.getMessage()).describedAs("msg").isEqualTo(mutable);
        assertThat(mutable.getThrown()).describedAs("thrwn").isNull();

        assertThat(mutable.getSource()).describedAs("source").isNull();
        assertThat(mutable.getThrownProxy()).describedAs("thrownProxy").isNull();

        // primitive fields are NOT reset:
        assertTrue(mutable.isEndOfBatch(), "end of batch");
        assertTrue(mutable.isIncludeLocation(), "incl loc");
        assertThat(mutable.getNanoTime()).describedAs("nanoTm").isNotEqualTo(0);
        assertThat(mutable.getTimeMillis()).describedAs("timeMs").isNotEqualTo(0);

        // thread-local fields are NOT reset:
        assertThat(mutable.getThreadId()).describedAs("tid").isNotEqualTo(0);
        assertThat(mutable.getThreadName()).describedAs("tname").isNotNull();
        assertThat(mutable.getThreadPriority()).describedAs("tpriority").isNotEqualTo(0);
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

        assertThat(evt2.getTimeMillis()).isEqualTo(evt.getTimeMillis());
        assertThat(evt2.getLoggerFqcn()).isEqualTo(evt.getLoggerFqcn());
        assertThat(evt2.getLevel()).isEqualTo(evt.getLevel());
        assertThat(evt2.getLoggerName()).isEqualTo(evt.getLoggerName());
        assertThat(evt2.getMarker()).isEqualTo(evt.getMarker());
        assertThat(evt2.getContextData()).isEqualTo(evt.getContextData());
        assertThat(evt2.getContextStack()).isEqualTo(evt.getContextStack());
        assertThat(evt2.getMessage()).isEqualTo(evt.getMessage());
        assertThat(evt2.getSource()).isNotNull();
        assertThat(evt2.getSource()).isEqualTo(evt.getSource());
        assertThat(evt2.getThreadName()).isEqualTo(evt.getThreadName());
        assertThat(evt2.getThrown()).isNull();
        assertThat(evt2.getThrownProxy()).isNull();
        assertThat(evt2.isEndOfBatch()).isEqualTo(evt.isEndOfBatch());
        assertThat(evt2.isIncludeLocation()).isEqualTo(evt.isIncludeLocation());

        assertThat(evt2.getNanoTime()).isNotEqualTo(evt.getNanoTime()); // nano time is transient in log4j log event
        assertThat(evt2.getNanoTime()).isEqualTo(0);
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

        assertThat(evt2.getTimeMillis()).isEqualTo(evt.getTimeMillis());
        assertThat(evt2.getLoggerFqcn()).isEqualTo(evt.getLoggerFqcn());
        assertThat(evt2.getLevel()).isEqualTo(evt.getLevel());
        assertThat(evt2.getLoggerName()).isEqualTo(evt.getLoggerName());
        assertThat(evt2.getMarker()).isEqualTo(evt.getMarker());
        assertThat(evt2.getContextData()).isEqualTo(evt.getContextData());
        assertThat(evt2.getContextStack()).isEqualTo(evt.getContextStack());
        assertThat(evt2.getMessage()).isEqualTo(evt.getMessage());
        assertThat(evt2.getSource()).isNotNull();
        assertThat(evt2.getSource()).isEqualTo(evt.getSource());
        assertThat(evt2.getThreadName()).isEqualTo(evt.getThreadName());
        assertThat(evt2.getThrown()).isNull();
        assertThat(evt2.getThrownProxy()).isNotNull();
        assertThat(evt2.getThrownProxy()).isEqualTo(evt.getThrownProxy());
        assertThat(evt2.isEndOfBatch()).isEqualTo(evt.isEndOfBatch());
        assertThat(evt2.isIncludeLocation()).isEqualTo(evt.isIncludeLocation());

        assertThat(evt2.getNanoTime()).isNotEqualTo(evt.getNanoTime()); // nano time is transient in log4j log event
        assertThat(evt2.getNanoTime()).isEqualTo(0);
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
