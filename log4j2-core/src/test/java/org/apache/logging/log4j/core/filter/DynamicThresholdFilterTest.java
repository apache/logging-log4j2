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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Log4jLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class DynamicThresholdFilterTest {

    @Test
    public void testFilter() {
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "apache");
        ValueLevelPair[] pairs = new ValueLevelPair[] { new ValueLevelPair("testuser", Level.DEBUG),
                                                         new ValueLevelPair("JohnDoe", Level.WARN)};
        DynamicThresholdFilter filter = DynamicThresholdFilter.createFilter("userid", pairs, "ERROR", null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.NEUTRAL);
        assertTrue(filter.filter(null, Level.ERROR, null, null, (Throwable)null) == Filter.Result.NEUTRAL);
        ThreadContext.clear();
        ThreadContext.put("userid", "JohnDoe");
        ThreadContext.put("organization", "apache");
        LogEvent event = new Log4jLogEvent(null, null, null, Level.DEBUG, new SimpleMessage("Test"), null);
        assertTrue(filter.filter(event) == Filter.Result.DENY);
        event = new Log4jLogEvent(null, null, null, Level.ERROR, new SimpleMessage("Test"), null);
        assertTrue(filter.filter(event) == Filter.Result.NEUTRAL);
        ThreadContext.clear();
    }
}
