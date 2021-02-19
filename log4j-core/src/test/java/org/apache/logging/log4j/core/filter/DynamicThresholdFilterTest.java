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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.UsingThreadContextMap;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

@UsingThreadContextMap
public class DynamicThresholdFilterTest {

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
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.NEUTRAL);
        assertThat(filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.NEUTRAL);
        ThreadContext.clearMap();
        ThreadContext.put("userid", "JohnDoe");
        ThreadContext.put("organization", "apache");
        LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.DEBUG).setMessage(new SimpleMessage("Test")).build();
        assertThat(filter.filter(event)).isSameAs(Filter.Result.DENY);
        event = Log4jLogEvent.newBuilder().setLevel(Level.ERROR).setMessage(new SimpleMessage("Test")).build();
        assertThat(filter.filter(event)).isSameAs(Filter.Result.NEUTRAL);
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
        assertThat(filter.isStarted()).isTrue();
        final Object [] replacements = {"one", "two", "three"};
        assertThat(filter.filter(null, Level.DEBUG, null, "some test message", replacements)).isSameAs(Filter.Result.ACCEPT);
        assertThat(filter.filter(null, Level.DEBUG, null, "some test message", "one", "two", "three")).isSameAs(Filter.Result.ACCEPT);
        ThreadContext.clearMap();
    }
    
    @Test
    @LoggerContextSource("log4j2-dynamicfilter.xml")
    public void testConfig(final Configuration config) {
        final Filter filter = config.getFilter();
        assertThat(filter).describedAs("No DynamicThresholdFilter").isNotNull();
        assertTrue(filter instanceof DynamicThresholdFilter, "Not a DynamicThresholdFilter");
        final DynamicThresholdFilter dynamic = (DynamicThresholdFilter) filter;
        final String key = dynamic.getKey();
        assertThat(key).describedAs("Key is null").isNotNull();
        assertThat(key).describedAs("Incorrect key value").isEqualTo("loginId");
        final Map<String, Level> map = dynamic.getLevelMap();
        assertThat(map).describedAs("Map is null").isNotNull();
        assertThat(map).describedAs("Incorrect number of map elements").hasSize(1);
    }
}
