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
package org.apache.logging.log4j.core.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

// as of Java 12, final fields can no longer be overwritten via reflection
@EnabledOnJre({ JRE.JAVA_8, JRE.JAVA_9, JRE.JAVA_10, JRE.JAVA_11 })
public class ClockFactoryTest {

    public static void resetClocks() throws IllegalAccessException {
        resetClock(Log4jLogEvent.class);
        resetClock(AsyncLogger.class);
    }

    public static void resetClock(final Class<?> clazz) throws IllegalAccessException {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
        final Field field = FieldUtils.getField(clazz, "CLOCK", true);
        FieldUtils.removeFinalModifier(field);
        FieldUtils.writeStaticField(field, ClockFactory.getClock(), false);
    }

    @BeforeEach
    public void setUp() throws Exception {
        resetClocks();
    }

    @Test
    public void testDefaultIsSystemClock() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
        assertThat(ClockFactory.getClock().getClass()).isSameAs(SystemClock.class);
    }

    @Test
    public void testSpecifySystemClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "SystemClock");
        assertThat(ClockFactory.getClock().getClass()).isSameAs(SystemClock.class);
    }

    @Test
    public void testSpecifySystemClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, SystemClock.class.getName());
        assertThat(ClockFactory.getClock().getClass()).isSameAs(SystemClock.class);
    }

    @Test
    public void testSpecifyCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CachedClock");
        assertThat(ClockFactory.getClock().getClass()).isSameAs(CachedClock.class);
    }

    @Test
    public void testSpecifyCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CachedClock.class.getName());
        assertThat(ClockFactory.getClock().getClass()).isSameAs(CachedClock.class);
    }

    @Test
    public void testSpecifyCoarseCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CoarseCachedClock");
        assertThat(ClockFactory.getClock().getClass()).isSameAs(CoarseCachedClock.class);
    }

    @Test
    public void testSpecifyCoarseCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CoarseCachedClock.class.getName());
        assertThat(ClockFactory.getClock().getClass()).isSameAs(CoarseCachedClock.class);
    }

    public static class MyClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 42;
        }
    }

    @Test
    public void testCustomClock() {
        System.setProperty(ClockFactory.PROPERTY_NAME, MyClock.class.getName());
        assertThat(ClockFactory.getClock().getClass()).isSameAs(MyClock.class);
    }

}
