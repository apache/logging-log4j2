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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClockFactoryTest {

    public static void resetClocks() throws IllegalAccessException {
        resetClock(Log4jLogEvent.class);
        resetClock(AsyncLogger.class);
    }

    public static void resetClock(final Class<?> clazz) throws IllegalAccessException {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
        final Field field = FieldUtils.getField(clazz, "CLOCK", true);
        FieldUtils.writeStaticField(field, ClockFactory.getClock(), false);
    }

    @BeforeEach
    void setUp() throws Exception {
        resetClocks();
    }

    @Test
    void testDefaultIsSystemClock() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
        assertSame(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    void testSpecifySystemClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "SystemClock");
        assertSame(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    void testSpecifySystemClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, SystemClock.class.getName());
        assertSame(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    void testSpecifyCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CachedClock");
        assertSame(CachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    void testSpecifyCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CachedClock.class.getName());
        assertSame(CachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    void testSpecifyCoarseCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CoarseCachedClock");
        assertSame(CoarseCachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    void testSpecifyCoarseCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CoarseCachedClock.class.getName());
        assertSame(CoarseCachedClock.class, ClockFactory.getClock().getClass());
    }

    public static class MyClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 42;
        }
    }

    @Test
    void testCustomClock() {
        System.setProperty(ClockFactory.PROPERTY_NAME, MyClock.class.getName());
        assertSame(MyClock.class, ClockFactory.getClock().getClass());
    }
}
