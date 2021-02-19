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

import static org.apache.logging.log4j.hamcrest.MapMatchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.spi.MutableThreadContextStack;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.StringMap;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class MapRewritePolicyTest {
    private static final StringMap stringMap = ContextDataFactory.createContextData();
    private static Map<String, String> map = new HashMap<>();
    private static KeyValuePair[] rewrite;
    private static LogEvent logEvent0, logEvent1, logEvent2, logEvent3;

    @BeforeAll
    public static void setupClass() {
        stringMap.putValue("test1", "one");
        stringMap.putValue("test2", "two");
        map = stringMap.toMap(); 
        logEvent0 = Log4jLogEvent.newBuilder() //
                .setLoggerName("test") //
                .setContextData(stringMap) //
                .setLoggerFqcn("MapRewritePolicyTest.setupClass()") //
                .setLevel(Level.ERROR) //
                .setMessage(new SimpleMessage("Test")) //
                .setThrown(new RuntimeException("test")) //
                .setThreadName("none")
                .setSource(new StackTraceElement("MapRewritePolicyTest", "setupClass", "MapRewritePolicyTest", 28))
                .setTimeMillis(2).build();

        logEvent1 = ((Log4jLogEvent) logEvent0).asBuilder() //
                .setMessage(new StringMapMessage(map)) //
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
                .setMessage(new StringMapMessage(map)) //
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
        assertThat(rewritten.getMessage()).describedAs("Simple log message changed").isEqualTo(logEvent0.getMessage());

        rewritten = addPolicy.rewrite(logEvent1);
        compareLogEvents(logEvent1, rewritten);
        checkAdded(((StringMapMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent2);
        compareLogEvents(logEvent2, rewritten);
        checkAdded(((StructuredDataMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent3);
        compareLogEvents(logEvent3, rewritten);
        checkAdded(((StringMapMessage)rewritten.getMessage()).getData());
    }

    @Test
    public void updateTest() {
        final MapRewritePolicy updatePolicy = MapRewritePolicy.createPolicy("Update", rewrite);
        LogEvent rewritten = updatePolicy.rewrite(logEvent0);
        compareLogEvents(logEvent0, rewritten);
        assertThat(rewritten.getMessage()).describedAs("Simple log message changed").isEqualTo(logEvent0.getMessage());

        rewritten = updatePolicy.rewrite(logEvent1);
        compareLogEvents(logEvent1, rewritten);
        checkUpdated(((StringMapMessage)rewritten.getMessage()).getData());

        rewritten = updatePolicy.rewrite(logEvent2);
        compareLogEvents(logEvent2, rewritten);
        checkUpdated(((StructuredDataMessage)rewritten.getMessage()).getData());

        rewritten = updatePolicy.rewrite(logEvent3);
        compareLogEvents(logEvent3, rewritten);
        checkUpdated(((StringMapMessage)rewritten.getMessage()).getData());
    }

    @Test
    public void defaultIsAdd() {
        final MapRewritePolicy addPolicy = MapRewritePolicy.createPolicy(null, rewrite);
        LogEvent rewritten = addPolicy.rewrite(logEvent0);
        compareLogEvents(logEvent0, rewritten);
        assertThat(rewritten.getMessage()).describedAs("Simple log message changed").isEqualTo(logEvent0.getMessage());

        rewritten = addPolicy.rewrite(logEvent1);
        compareLogEvents(logEvent1, rewritten);
        checkAdded(((StringMapMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent2);
        compareLogEvents(logEvent2, rewritten);
        checkAdded(((StructuredDataMessage)rewritten.getMessage()).getData());

        rewritten = addPolicy.rewrite(logEvent3);
        compareLogEvents(logEvent3, rewritten);
        checkAdded(((StringMapMessage)rewritten.getMessage()).getData());
    }

    private void checkAdded(final Map<String, String> addedMap) {
        assertThat(addedMap).describedAs("unwanted entry change").is(new HamcrestCondition<>(hasEntry("test1", "one")));
        assertThat(addedMap).describedAs("existing entry not updated").is(new HamcrestCondition<>(hasEntry("test2", "2")));
        assertThat(addedMap).describedAs("new entry not added").is(new HamcrestCondition<>(hasEntry("test3", "three")));
        assertThat(addedMap).describedAs("wrong size").is(new HamcrestCondition<>(hasSize(3)));
    }

    private void checkUpdated(final Map<String, String> updatedMap) {
        assertThat(updatedMap).describedAs("unwanted entry change").is(new HamcrestCondition<>(hasEntry("test1", "one")));
        assertThat(updatedMap).describedAs("existing entry not updated").is(new HamcrestCondition<>(hasEntry("test2", "2")));
        assertThat(updatedMap).describedAs("wrong size").is(new HamcrestCondition<>(hasSize(2)));
    }

    @SuppressWarnings("deprecation")
    private void compareLogEvents(final LogEvent orig, final LogEvent changed) {
        // Ensure that everything but the Mapped Data is still the same
        assertThat(changed.getLoggerName()).describedAs("LoggerName changed").isEqualTo(orig.getLoggerName());
        assertThat(changed.getMarker()).describedAs("Marker changed").isEqualTo(orig.getMarker());
        assertThat(changed.getLoggerFqcn()).describedAs("FQCN changed").isEqualTo(orig.getLoggerFqcn());
        assertThat(changed.getLevel()).describedAs("Level changed").isEqualTo(orig.getLevel());
        assertThat(changed.getThrown() == null ? null : changed.getThrownProxy().getExtendedStackTrace()).describedAs("Throwable changed").isEqualTo(orig.getThrown() == null ? null : orig.getThrownProxy().getExtendedStackTrace());
        assertThat(changed.getContextData()).describedAs("ContextData changed").isEqualTo(orig.getContextData());
        assertThat(changed.getContextStack()).describedAs("ContextStack changed").isEqualTo(orig.getContextStack());
        assertThat(changed.getThreadName()).describedAs("ThreadName changed").isEqualTo(orig.getThreadName());
        assertThat(changed.getSource()).describedAs("Source changed").isEqualTo(orig.getSource());
        assertThat(changed.getTimeMillis()).describedAs("Millis changed").isEqualTo(orig.getTimeMillis());
    }
}
