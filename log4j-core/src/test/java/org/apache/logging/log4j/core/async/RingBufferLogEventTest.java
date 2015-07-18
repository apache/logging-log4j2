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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the RingBufferLogEvent class.
 */
public class RingBufferLogEventTest {

    @Test
    public void testGetLevelReturnsOffIfNullLevelSet() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final Map<String, String> map = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        final long currentTimeMillis = 0;
        final long nanoTime = 1;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis, nanoTime);
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testGetMessageReturnsNonNullMessage() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final Map<String, String> map = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        final long currentTimeMillis = 0;
        final long nanoTime = 1;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis, nanoTime);
        assertNotNull(evt.getMessage());
    }

    @Test
    public void testGetMillisReturnsConstructorMillisForNormalMessage() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = null;
        final Marker marker = null;
        final String fqcn = null;
        final Level level = null;
        final Message data = null;
        final Throwable t = null;
        final Map<String, String> map = null;
        final ContextStack contextStack = null;
        final String threadName = null;
        final StackTraceElement location = null;
        final long currentTimeMillis = 123;
        final long nanoTime = 1;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis, nanoTime);
        assertEquals(123, evt.getTimeMillis());
    }

    @Test
    public void testSerializationDeserialization() throws IOException, ClassNotFoundException {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = "logger.name";
        final Marker marker = null;
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        final Message data = new SimpleMessage("message");
        final Throwable t = new InternalError("not a real error");
        final Map<String, String> map = null;
        final ContextStack contextStack = null;
        final String threadName = "main";
        final StackTraceElement location = null;
        final long currentTimeMillis = 12345;
        final long nanoTime = 1;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis, nanoTime);
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(evt);
        
        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final RingBufferLogEvent other = (RingBufferLogEvent) in.readObject();
        assertEquals(loggerName, other.getLoggerName());
        assertEquals(marker, other.getMarker());
        assertEquals(fqcn, other.getLoggerFqcn());
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
    
    @Test
    public void testCreateMementoReturnsCopy() {
        final RingBufferLogEvent evt = new RingBufferLogEvent();
        final String loggerName = "logger.name";
        final Marker marker = MarkerManager.getMarker("marked man");
        final String fqcn = "f.q.c.n";
        final Level level = Level.TRACE;
        final Message data = new SimpleMessage("message");
        final Throwable t = new InternalError("not a real error");
        final Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        final ContextStack contextStack = new MutableThreadContextStack(Arrays.asList("a", "b"));
        final String threadName = "main";
        final StackTraceElement location = null;
        final long currentTimeMillis = 12345;
        final long nanoTime = 1;
        evt.setValues(null, loggerName, marker, fqcn, level, data, t, map,
                contextStack, threadName, location, currentTimeMillis, nanoTime);
        
        final LogEvent actual = evt.createMemento();
        assertEquals(evt.getLoggerName(), actual.getLoggerName());
        assertEquals(evt.getMarker(), actual.getMarker());
        assertEquals(evt.getLoggerFqcn(), actual.getLoggerFqcn());
        assertEquals(evt.getLevel(), actual.getLevel());
        assertEquals(evt.getMessage(), actual.getMessage());
        assertEquals(evt.getThrown(), actual.getThrown());
        assertEquals(evt.getContextMap(), actual.getContextMap());
        assertEquals(evt.getContextStack(), actual.getContextStack());
        assertEquals(evt.getThreadName(), actual.getThreadName());
        assertEquals(evt.getTimeMillis(), actual.getTimeMillis());
        assertEquals(evt.getSource(), actual.getSource());
        assertEquals(evt.getThrownProxy(), actual.getThrownProxy());
    }
}
