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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.plugins.di.DI;
import org.junit.jupiter.api.Test;

public class ClockFactoryTest {

    private final DI.FactoryBuilder builder = DI.builder();

    @Test
    public void testDefaultIsSystemClock() {
        final Clock clock = builder.build().getInstance(Clock.KEY);
        assertThat(clock).isInstanceOf(SystemClock.class);
    }

    @Test
    public void testSpecifySystemClockShort() {
        final Clock clock = builder.addInitialBindingFrom(Clock.KEY)
                .toSingleton(SystemClock::new)
                .build()
                .getInstance(Clock.KEY);
        assertThat(clock).isInstanceOf(SystemClock.class);
    }

    @Test
    public void testSpecifyCachedClockShort() {
        final Clock clock = builder.addInitialBindingFrom(Clock.KEY)
                .toSingleton(CachedClock::instance)
                .build()
                .getInstance(Clock.KEY);
        assertThat(clock).isInstanceOf(CachedClock.class);
    }

    @Test
    public void testSpecifyCoarseCachedClockShort() {
        final Clock clock = builder.addInitialBindingFrom(Clock.KEY)
                .toSingleton(CoarseCachedClock::instance)
                .build()
                .getInstance(Clock.KEY);
        assertThat(clock).isInstanceOf(CoarseCachedClock.class);
    }

    public static class MyClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 42;
        }
    }

    @Test
    public void testCustomClock() {
        final Clock clock = builder.addInitialBindingFrom(Clock.KEY)
                .toSingleton(MyClock::new)
                .build()
                .getInstance(Clock.KEY);
        assertThat(clock).isInstanceOf(MyClock.class);
    }
}
