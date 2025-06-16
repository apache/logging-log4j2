/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class PatternSelectorTest {

    public static class FauxLogger {
        public String formatEvent(final LogEvent event, final Layout<?> layout) {
            return new String(layout.toByteArray(event));
        }
    }

    LoggerContext ctx = LoggerContext.getContext();

    @Test
    void testMarkerPatternSelector() {
        final PatternMatch[] patterns = new PatternMatch[1];
        patterns[0] = new PatternMatch("FLOW", "%d %-5p [%t]: ====== %C{1}.%M:%L %m ======%n");
        final PatternSelector selector = MarkerPatternSelector.createSelector(
                patterns, "%d %-5p [%t]: %m%n", true, true, ctx.getConfiguration());
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPatternSelector(selector)
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.layout.PatternSelectorTest$FauxLogger")
                .setMarker(MarkerManager.getMarker("FLOW"))
                .setLevel(Level.TRACE) //
                .setIncludeLocation(true)
                .setMessage(new SimpleMessage("entry"))
                .build();
        final String result1 = new FauxLogger().formatEvent(event1, layout);
        final String expectSuffix1 =
                String.format("====== PatternSelectorTest.testMarkerPatternSelector:58 entry ======%n");
        assertTrue(result1.endsWith(expectSuffix1), "Unexpected result: " + result1);
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 1!"))
                .build();
        final String result2 = new String(layout.toByteArray(event2));
        final String expectSuffix2 = String.format("Hello, world 1!%n");
        assertTrue(result2.endsWith(expectSuffix2), "Unexpected result: " + result2);
    }

    @Test
    void testLevelPatternSelector() {
        final PatternMatch[] patterns = new PatternMatch[1];
        patterns[0] = new PatternMatch("TRACE", "%d %-5p [%t]: ====== %C{1}.%M:%L %m ======%n");
        final PatternSelector selector =
                LevelPatternSelector.createSelector(patterns, "%d %-5p [%t]: %m%n", true, true, ctx.getConfiguration());
        final PatternLayout layout = PatternLayout.newBuilder()
                .withPatternSelector(selector)
                .withConfiguration(ctx.getConfiguration())
                .build();
        final LogEvent event1 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.layout.PatternSelectorTest$FauxLogger")
                .setLevel(Level.TRACE) //
                .setIncludeLocation(true)
                .setMessage(new SimpleMessage("entry"))
                .build();
        final String result1 = new FauxLogger().formatEvent(event1, layout);
        final String expectSuffix1 =
                String.format("====== PatternSelectorTest.testLevelPatternSelector:90 entry ======%n");
        assertTrue(result1.endsWith(expectSuffix1), "Unexpected result: " + result1);
        final LogEvent event2 = Log4jLogEvent.newBuilder() //
                .setLoggerName(this.getClass().getName())
                .setLoggerFqcn("org.apache.logging.log4j.core.Logger") //
                .setLevel(Level.INFO) //
                .setMessage(new SimpleMessage("Hello, world 1!"))
                .build();
        final String result2 = new String(layout.toByteArray(event2));
        final String expectSuffix2 = String.format("Hello, world 1!%n");
        assertTrue(result2.endsWith(expectSuffix2), "Unexpected result: " + result2);
    }
}
