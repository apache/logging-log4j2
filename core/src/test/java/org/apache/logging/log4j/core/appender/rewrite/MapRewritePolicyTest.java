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
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class MapRewritePolicyTest {
    private static Map<String, String> map = new HashMap<String, String>();
    private static KeyValuePair[] rewrite;
    private static LogEvent logEvent0, logEvent1, logEvent2, logEvent3;

    @BeforeClass
    public static void setupClass() {
        map.put("test1", "one");
        map.put("test2", "two");
        logEvent0 = new Log4jLogEvent("test", null, "MapRewritePolicyTest.setupClass()", Level.ERROR,
        new SimpleMessage("Test"), new RuntimeException("test"), map, null, "none",
        new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 28), 2);
        logEvent1 = new Log4jLogEvent("test", null, "MapRewritePolicyTest.setupClass()", Level.ERROR,
        new MapMessage(map), null, map, null, "none",
        new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 29), 2);
    final ThreadContextStack stack = new MutableThreadContextStack(new ArrayList<String>(map.values()));
        logEvent2 = new Log4jLogEvent("test", MarkerManager.getMarker("test"), "MapRewritePolicyTest.setupClass()",
        Level.TRACE, new StructuredDataMessage("test", "Nothing", "test", map), new RuntimeException("test"), null,
        stack, "none", new StackTraceElement("MapRewritePolicyTest",
        "setupClass", "MapRewritePolicyTest", 30), 20000000);
        logEvent3 = new Log4jLogEvent("test", null, "MapRewritePolicyTest.setupClass()", Level.ALL, new MapMessage(map),
        null, map, stack, null, new StackTraceElement("MapRewritePolicyTest",
        "setupClass", "MapRewritePolicyTest", 31), Long.MAX_VALUE);
        rewrite = new KeyValuePair[] {new KeyValuePair("test2", "2"), new KeyValuePair("test3", "three")};
    }

    @Test
    public void addTest() {
        final MapRewritePolicy addPolicy = MapRewritePolicy.createPolicy("Add", rewrite);
        LogEvent rewritten = addPolicy.rewrite(logEvent0);
        compareLogEvents(logEvent0, rewritten);
        Assert.assertEquals("Simple log message changed", logEvent0.getMessage(), rewritten.getMessage());

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
        Assert.assertEquals("Simple log message changed", logEvent0.getMessage(), rewritten.getMessage());

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
        Assert.assertEquals("Simple log message changed", logEvent0.getMessage(), rewritten.getMessage());

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
        Assert.assertEquals("unwanted entry change", "one", addedMap.get("test1"));
        Assert.assertEquals("existing entry not updated", "2", addedMap.get("test2"));
        Assert.assertEquals("new entry not added", "three", addedMap.get("test3"));
        Assert.assertEquals("wrong size", 3, addedMap.size());
    }

    private void checkUpdated(final Map<String, String> updatedMap) {
        Assert.assertEquals("unwanted entry change", "one", updatedMap.get("test1"));
        Assert.assertEquals("existing entry not updated", "2", updatedMap.get("test2"));
        Assert.assertEquals("wrong size", 2, updatedMap.size());
    }

    private void compareLogEvents(final LogEvent orig, final LogEvent changed) {
        // Ensure that everything but the Mapped Data is still the same
        Assert.assertEquals("LoggerName changed", orig.getLoggerName(), changed.getLoggerName());
        Assert.assertEquals("Marker changed", orig.getMarker(), changed.getMarker());
        Assert.assertEquals("FQCN changed", orig.getFQCN(), changed.getFQCN());
        Assert.assertEquals("Level changed", orig.getLevel(), changed.getLevel());
        Assert.assertEquals("Throwable changed", orig.getThrown() == null //
                ? null //
                : ((Log4jLogEvent) orig).getThrownProxy().getExtendedStackTrace(), //
                changed.getThrown() == null //
                        ? null //
                        : ((Log4jLogEvent) changed).getThrownProxy().getExtendedStackTrace());
        Assert.assertEquals("ContextMap changed", orig.getContextMap(), changed.getContextMap());
        Assert.assertEquals("ContextStack changed", orig.getContextStack(), changed.getContextStack());
        Assert.assertEquals("ThreadName changed", orig.getThreadName(), changed.getThreadName());
        Assert.assertEquals("Source changed", orig.getSource(), changed.getSource());
        Assert.assertEquals("Millis changed", orig.getMillis(), changed.getMillis());
    }
}
