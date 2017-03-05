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

import java.util.Calendar;
import java.util.TimeZone;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.ClockFactoryTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class TimeFilterTest {
    private static long CLOCKTIME = System.currentTimeMillis();

    /** Helper class */
    public static class FixedTimeClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return CLOCKTIME;
        }
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ClockFactory.PROPERTY_NAME, FixedTimeClock.class.getName());
    }

    @AfterClass
    public static void afterClass() throws IllegalAccessException {
        ClockFactoryTest.resetClocks();
    }

    @Test
    public void testTime() {
        final TimeFilter filter = TimeFilter.createFilter("02:00:00", "03:00:00", "America/LosAngeles", null, null);
        filter.start();
        assertTrue(filter.isStarted());
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/LosAngeles"));
        cal.set(Calendar.HOUR_OF_DAY, 2);
        CLOCKTIME = cal.getTimeInMillis();
        LogEvent event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));

        cal.roll(Calendar.DAY_OF_MONTH, true);
        CLOCKTIME = cal.getTimeInMillis();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.NEUTRAL, filter.filter(event));
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));

        cal.set(Calendar.HOUR_OF_DAY, 4);
        CLOCKTIME = cal.getTimeInMillis();
        event = Log4jLogEvent.newBuilder().setTimeMillis(CLOCKTIME).build();
        assertSame(Filter.Result.DENY, filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null));
        assertSame(Filter.Result.DENY, filter.filter(event));
    }
}
