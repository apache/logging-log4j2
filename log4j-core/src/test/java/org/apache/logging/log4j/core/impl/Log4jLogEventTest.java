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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
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
        /* (non-Javadoc)
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
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", null, Strings.EMPTY,
                Level.INFO, new SimpleMessage("abc"), null);

        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(evt);

        final ByteArrayInputStream inArr = new ByteArrayInputStream(arr.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(inArr);
        final Log4jLogEvent evt2 = (Log4jLogEvent) in.readObject();

        assertEquals(evt.getTimeMillis(), evt2.getTimeMillis());
        assertEquals(evt.getLoggerFQCN(), evt2.getLoggerFQCN());
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
    public void testNullLevelReplacedWithOFF() throws Exception {
        final Marker marker = null;
        final Throwable t = null;
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", marker, Strings.EMPTY,
                NULL_LEVEL, new SimpleMessage("abc"), t);
        assertEquals(Level.OFF, evt.getLevel());
    }

    @Test
    public void testTimestampGeneratedByClock() {
        final Marker marker = null;
        final Throwable t = null;
        final Level NULL_LEVEL = null;
        final Log4jLogEvent evt = new Log4jLogEvent("some.test", marker, Strings.EMPTY,
                NULL_LEVEL, new SimpleMessage("abc"), t);
        assertEquals(FixedTimeClock.FIXED_TIME, evt.getTimeMillis());
        
    }
}
