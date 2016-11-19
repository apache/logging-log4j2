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
package org.apache.logging.log4j.core.appender.rewrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.logging.log4j.hamcrest.MapMatchers.hasSize;
import static org.hamcrest.Matchers.hasEntry;

import static org.junit.Assert.*;


public class MapRewritePolicyTest {
    private static Map<String, String> map = new HashMap<>();
    private static KeyValuePair[] rewrite;
    private static LogEvent logEvent0, logEvent1, logEvent2, logEvent3;

    @BeforeClass
    public static void setupClass() {
        map.put("test1", "one");
        map.put("test2", "two");
        logEvent0 = Log4jLogEvent.newBuilder() //
                .setLoggerName("test") //
                .setContextMap(map) //
                .setLoggerFqcn("MapRewritePolicyTest.setupClass()") //
                .setLevel(Level.ERROR) //
                .setMessage(new SimpleMessage("Test")) //
                .setThrown(new RuntimeException("test")) //
                .setThreadName("none")
                .setSource(new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 28))
                .setTimeMillis(2).build();

        logEvent1 = ((Log4jLogEvent) logEvent0).asBuilder() //
                .setMessage(new MapMessage(map)) //
                .setSource(new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 29)) //
                .build();

        final ThreadContextStack stack = new MutableThreadContextStack(new ArrayList<>(map.values()));
        logEvent2 = ((Log4jLogEvent) logEvent0).asBuilder() //
                .setContextStack(stack) //
                .setMarker(MarkerManager.getMarker("test")) //
                .setLevel(Level.TRACE) //
                .setMessage(new StructuredDataMessage("test", "Nothing", "test", map)) //
                .setTimeMillis(20000000) //
                .setSource(new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 30)) //
                .build();
        logEvent3 = ((Log4jLogEvent) logEvent0).asBuilder() //
                .setContextStack(stack) //
                .setLevel(Level.ALL) //
                .setMessage(new MapMessage(map)) //
                .setTimeMillis(Long.MAX_VALUE) //
                .setSource(new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 31)) //
                .build();
        rewrite = new KeyValuePair[]{new KeyValuePair("test2", "2"), new KeyValuePair("test3", "three")};
    }

    @Test
    public void addTest() {
        final MapRewritePolicy addPolicy = MapRewritePolicy.createPolicy("Add", rewrite);
        LogEvent rewritten = addPolicy.rewrite(logEvent0);
        compareLogEvents(logEvent0, rewritten);
        assertEquals("Simple log message changed", logEvent0.getMessage(), rewritten.getMessage());

        rewritten = addPolicy.rewrite(logEvent1);
        compareLogEvents(logEvent1, rewritten);
        checkAdded(((MapMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent2);
        compareLogEvents(logEvent2, rewritten);
        checkAdded(((MapMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent3);
        compareLogEvents(logEvent3, rewritten);
        checkAdded(((MapMessage)rewritten.getMessage()).getData());
    }

    @Test
    public void updateTest() {
        final MapRewritePolicy updatePolicy = MapRewritePolicy.createPolicy("Update", rewrite);
        LogEvent rewritten = updatePolicy.rewrite(logEvent0);
        compareLogEvents(logEvent0, rewritten);
        assertEquals("Simple log message changed", logEvent0.getMessage(), rewritten.getMessage());

        rewritten = updatePolicy.rewrite(logEvent1);
        compareLogEvents(logEvent1, rewritten);
        checkUpdated(((MapMessage)rewritten.getMessage()).getData());

        rewritten = updatePolicy.rewrite(logEvent2);
        compareLogEvents(logEvent2, rewritten);
        checkUpdated(((MapMessage)rewritten.getMessage()).getData());

        rewritten = updatePolicy.rewrite(logEvent3);
        compareLogEvents(logEvent3, rewritten);
        checkUpdated(((MapMessage)rewritten.getMessage()).getData());
    }

    @Test
    public void defaultIsAdd() {
        final MapRewritePolicy addPolicy = MapRewritePolicy.createPolicy(null, rewrite);
        LogEvent rewritten = addPolicy.rewrite(logEvent0);
        compareLogEvents(logEvent0, rewritten);
        assertEquals("Simple log message changed", logEvent0.getMessage(), rewritten.getMessage());

        rewritten = addPolicy.rewrite(logEvent1);
        compareLogEvents(logEvent1, rewritten);
        checkAdded(((MapMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent2);
        compareLogEvents(logEvent2, rewritten);
        checkAdded(((MapMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent3);
        compareLogEvents(logEvent3, rewritten);
        checkAdded(((MapMessage)rewritten.getMessage()).getData());
    }

    private void checkAdded(final Map<String, String> addedMap) {
        assertThat("unwanted entry change", addedMap, hasEntry("test1", "one"));
        assertThat("existing entry not updated", addedMap, hasEntry("test2", "2"));
        assertThat("new entry not added", addedMap, hasEntry("test3", "three"));
        assertThat("wrong size", addedMap, hasSize(3));
    }

    private void checkUpdated(final Map<String, String> updatedMap) {
        assertThat("unwanted entry change", updatedMap, hasEntry("test1", "one"));
        assertThat("existing entry not updated", updatedMap, hasEntry("test2", "2"));
        assertThat("wrong size", updatedMap, hasSize(2));
    }

    @SuppressWarnings("deprecation")
    private void compareLogEvents(final LogEvent orig, final LogEvent changed) {
        // Ensure that everything but the Mapped Data is still the same
        assertEquals("LoggerName changed", orig.getLoggerName(), changed.getLoggerName());
        assertEquals("Marker changed", orig.getMarker(), changed.getMarker());
        assertEquals("FQCN changed", orig.getLoggerFqcn(), changed.getLoggerFqcn());
        assertEquals("Level changed", orig.getLevel(), changed.getLevel());
        assertArrayEquals("Throwable changed",
            orig.getThrown() == null ? null : orig.getThrownProxy().getExtendedStackTrace(),
            changed.getThrown() == null ? null : changed.getThrownProxy().getExtendedStackTrace()
        );
        assertEquals("ContextMap changed", orig.getContextMap(), changed.getContextMap());
        assertEquals("ContextData changed", orig.getContextData(), changed.getContextData());
        assertEquals("ContextStack changed", orig.getContextStack(), changed.getContextStack());
        assertEquals("ThreadName changed", orig.getThreadName(), changed.getThreadName());
        assertEquals("Source changed", orig.getSource(), changed.getSource());
        assertEquals("Millis changed", orig.getTimeMillis(), changed.getTimeMillis());
    }
}
