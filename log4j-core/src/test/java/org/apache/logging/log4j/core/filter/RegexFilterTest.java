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

import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.util.TypeConverters;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class RegexFilterTest {
    @BeforeClass
    public static void before() {
        StatusLogger.getLogger().setLevel(Level.OFF);
    }

    @Test
    public void testThresholds() {
        RegexFilter filter = RegexFilter.createFilter(Pattern.compile(".* test .*"), false, null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.NEUTRAL,
            filter.filter(null, Level.DEBUG, null, "This is a test message", (Throwable) null));
        assertSame(Filter.Result.DENY, filter.filter(null, Level.ERROR, null, "This is not a test", (Throwable) null));
        LogEvent event = new Log4jLogEvent(null, null, null, Level.DEBUG, new SimpleMessage("Another test message"), null);
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));
        event = new Log4jLogEvent(null, null, null, Level.ERROR, new SimpleMessage("test"), null);
        assertSame(Filter.Result.DENY, filter.filter(event));
        filter = RegexFilter.createFilter((Pattern) TypeConverters.convert("* test *", Pattern.class, null), false, null, null);
        assertNull(filter);
    }

    @Test
    public void testNoMsg() {
        final RegexFilter filter = RegexFilter.createFilter(Pattern.compile(".* test .*"), false, null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (String) null, (Throwable) null));
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Message) null, (Throwable) null));
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, null, (Object[]) null));

    }
}
