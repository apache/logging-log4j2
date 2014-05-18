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

package org.apache.logging.log4j.core.async;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.junit.Test;

/**
 * Tests the RingBufferLogEvent class.
 */
public class RingBufferLogEventTest {

    @Test
    public void testGetLevelReturnsOffIfNullLevelSet() {
        RingBufferLogEvent evt = new RingBufferLogEvent();
        String loggerName = null;
        Marker marker = null;
        String fqcn = null;
        Level level = null;
        Message data = null;
        Throwable t = null;
        Map<String, String> map = null;
        ContextStack contextStack = null;
        String threadName = null;
        StackTraceElement location = null;
        long currentTimeMillis = 0;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis);
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testGetMessageReturnsNonNullMessage() {
        RingBufferLogEvent evt = new RingBufferLogEvent();
        String loggerName = null;
        Marker marker = null;
        String fqcn = null;
        Level level = null;
        Message data = null;
        Throwable t = null;
        Map<String, String> map = null;
        ContextStack contextStack = null;
        String threadName = null;
        StackTraceElement location = null;
        long currentTimeMillis = 0;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis);
        assertNotNull(evt.getMessage());
    }

    @Test
    public void testGetMillisReturnsConstructorMillisForNormalMessage() {
        RingBufferLogEvent evt = new RingBufferLogEvent();
        String loggerName = null;
        Marker marker = null;
        String fqcn = null;
        Level level = null;
        Message data = null;
        Throwable t = null;
        Map<String, String> map = null;
        ContextStack contextStack = null;
        String threadName = null;
        StackTraceElement location = null;
        long currentTimeMillis = 123;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis);
        assertEquals(123, evt.getTimeMillis());
    }

    static class TimeMsg implements Message, TimestampMessage {
        private static final long serialVersionUID = -2038413535672337079L;
        private final String msg;
        private final long timestamp;

        public TimeMsg(String msg, long timestamp) {
            this.msg = msg;
            this.timestamp = timestamp;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String getFormattedMessage() {
            return msg;
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
    }

    @Test
    public void testGetMillisReturnsMsgTimestampForTimestampMessage() {
        RingBufferLogEvent evt = new RingBufferLogEvent();
        String loggerName = null;
        Marker marker = null;
        String fqcn = null;
        Level level = null;
        Message data = new TimeMsg("", 567);
        Throwable t = null;
        Map<String, String> map = null;
        ContextStack contextStack = null;
        String threadName = null;
        StackTraceElement location = null;
        long currentTimeMillis = 123;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis);
        assertEquals(567, evt.getTimeMillis());
    }

    @Test
    public void testSerializationDeserialization() throws IOException, ClassNotFoundException {
        RingBufferLogEvent evt = new RingBufferLogEvent();
        String loggerName = "logger.name";
        Marker marker = null;
        String fqcn = "f.q.c.n";
        Level level = Level.TRACE;
        Message data = new SimpleMessage("message");
        Throwable t = new InternalError("not a real error");
        Map<String, String> map = null;
        ContextStack contextStack = null;
        String threadName = "main";
        StackTraceElement location = null;
        long currentTimeMillis = 12345;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(evt);
        
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        RingBufferLogEvent other = (RingBufferLogEvent) in.readObject();
        assertEquals(loggerName, other.getLoggerName());
        assertEquals(marker, other.getMarker());
        assertEquals(fqcn, other.getLoggerFQCN());
        assertEquals(level, other.getLevel());
        assertEquals(data, other.getMessage());
        assertNull("null after serialization", other.getThrown());
        assertEquals(new ThrowableProxy(t), other.getThrownProxy());
        assertEquals(map, other.getContextMap());
        assertEquals(contextStack, other.getContextStack());
        assertEquals(threadName, other.getThreadName());
        assertEquals(location, other.getSource());
        assertEquals(currentTimeMillis, other.getTimeMillis());
    }
}
