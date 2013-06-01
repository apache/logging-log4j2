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
package org.apache.logging.log4j.core.helpers;

import static org.junit.Assert.*;

import org.apache.logging.log4j.core.helpers.CachedClock;
import org.apache.logging.log4j.core.helpers.Clock;
import org.apache.logging.log4j.core.helpers.ClockFactory;
import org.apache.logging.log4j.core.helpers.CoarseCachedClock;
import org.apache.logging.log4j.core.helpers.SystemClock;
import org.junit.Test;

public class ClockFactoryTest {

    @Test
    public void testDefaultIsSystemClock() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
        assertEquals(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifySystemClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "SystemClock");
        assertEquals(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifySystemClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, SystemClock.class.getName());
        assertEquals(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CachedClock");
        assertEquals(CachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CachedClock.class.getName());
        assertEquals(CachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCoarseCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CoarseCachedClock");
        assertEquals(CoarseCachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCoarseCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CoarseCachedClock.class.getName());
        assertEquals(CoarseCachedClock.class, ClockFactory.getClock().getClass());
    }

    static class MyClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 42;
        }
    }

    @Test
    public void testCustomClock() {
        System.setProperty(ClockFactory.PROPERTY_NAME, MyClock.class.getName());
        assertEquals(MyClock.class, ClockFactory.getClock().getClass());
    }

}
