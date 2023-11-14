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
package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

public class ClockFactoryTest {

    private final ConfigurableInstanceFactory instanceFactory = DI.createFactory();

    @Test
    public void testDefaultIsSystemClock() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(SystemClock.class);
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "SystemClock")
    public void testSpecifySystemClockShort() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(SystemClock.class);
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.SystemClock")
    public void testSpecifySystemClockLong() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(SystemClock.class);
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "CachedClock")
    public void testSpecifyCachedClockShort() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(CachedClock.class);
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.CachedClock")
    public void testSpecifyCachedClockLong() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(CachedClock.class);
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "CoarseCachedClock")
    public void testSpecifyCoarseCachedClockShort() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(CoarseCachedClock.class);
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.internal.CoarseCachedClock")
    public void testSpecifyCoarseCachedClockLong() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(CoarseCachedClock.class);
    }

    public static class MyClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 42;
        }
    }

    @Test
    @SetSystemProperty(key = Log4jPropertyKey.Constant.CONFIG_CLOCK, value = "org.apache.logging.log4j.core.time.ClockFactoryTest$MyClock")
    public void testCustomClock() {
        DI.initializeFactory(instanceFactory);
        assertThat(instanceFactory.getInstance(Clock.class)).isInstanceOf(MyClock.class);
    }

}
