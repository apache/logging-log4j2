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
package org.apache.logging.log4j.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.ThreadContextMapRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 */
public class DynamicThresholdFilterTest {

    @Rule
    public final ThreadContextMapRule threadContextRule = new ThreadContextMapRule(); 
    
    @After
    public void cleanup() {
        final LoggerContext ctx = LoggerContext.getContext(false);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testFilter() {
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "apache");
        final KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair("testuser", "DEBUG"),
                new KeyValuePair("JohnDoe", "warn") };
        final DynamicThresholdFilter filter = DynamicThresholdFilter.createFilter("userid", pairs, Level.ERROR, null,
                null);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null));
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));
        ThreadContext.clearMap();
        ThreadContext.put("userid", "JohnDoe");
        ThreadContext.put("organization", "apache");
        LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).setMessage(new SimpleMessage("Test")).build();
        assertSame(Filter.Result.DENY, filter.filter(event));
        event = Log4jLogEvent.newBuilder().setLevel(Level.ERROR).setMessage(new SimpleMessage("Test")).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));
        ThreadContext.clearMap();
    }

    @Test
    public void testFilterWorksWhenParamsArePassedAsArguments() {
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "apache");
        final KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair("testuser", "DEBUG"),
                new KeyValuePair("JohnDoe", "warn") };
        final DynamicThresholdFilter filter = DynamicThresholdFilter.createFilter("userid", pairs, Level.ERROR, Filter.Result.ACCEPT, Filter.Result.NEUTRAL);
        filter.start();
        assertTrue(filter.isStarted());
        final Object [] replacements = {"one", "two", "three"};
        assertSame(Filter.Result.ACCEPT, filter.filter(null, Level.DEBUG, null, "some test message", replacements)); 
        assertSame(Filter.Result.ACCEPT, filter.filter(null, Level.DEBUG, null, "some test message", "one", "two", "three")); 
        ThreadContext.clearMap();
    }
    
    @Test
    public void testConfig() {
        try (final LoggerContext ctx = Configurator.initialize("Test1",
                "target/test-classes/log4j2-dynamicfilter.xml")) {
            final Configuration config = ctx.getConfiguration();
            final Filter filter = config.getFilter();
            assertNotNull("No DynamicThresholdFilter", filter);
            assertTrue("Not a DynamicThresholdFilter", filter instanceof DynamicThresholdFilter);
            final DynamicThresholdFilter dynamic = (DynamicThresholdFilter) filter;
            final String key = dynamic.getKey();
            assertNotNull("Key is null", key);
            assertEquals("Incorrect key value", "loginId", key);
            final Map<String, Level> map = dynamic.getLevelMap();
            assertNotNull("Map is null", map);
            assertEquals("Incorrect number of map elements", 1, map.size());
        }
    }
}
