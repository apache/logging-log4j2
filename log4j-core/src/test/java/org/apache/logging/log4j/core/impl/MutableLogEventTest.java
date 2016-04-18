package org.apache.logging.log4j.core.impl;/*
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the MutableLogEvent class.
 */
public class MutableLogEventTest {
    private static final Map<String, String> CONTEXTMAP = createContextMap();
    private static final ThreadContext.ContextStack STACK = new MutableThreadContextStack(Arrays.asList("abc", "xyz"));

    private static Map<String,String> createContextMap() {
        Map<String,String> result = new HashMap<>();
        result.put("a", "1");
        result.put("b", "2");
        return result;
    }

    @Test
    public void testInitFromCopiesAllFields() {
//        private ThrowableProxy thrownProxy;
        Log4jLogEvent source = Log4jLogEvent.newBuilder() //
                .setContextMap(CONTEXTMAP) //
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
        MutableLogEvent mutable = new MutableLogEvent();
        mutable.initFrom(source);
        assertEquals("contextMap", CONTEXTMAP, mutable.getContextMap());
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
}