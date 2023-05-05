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
package org.apache.logging.log4j.core.time.internal;

import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.time.PreciseClock;

/**
 * Implementation of the {@code PreciseClock} interface that always returns a fixed time value.
 * @since 2.11
 */
public class FixedPreciseClock implements PreciseClock {
    private final long currentTimeMillis;
    private final int nanosOfMillisecond;

    /**
     * Constructs a {@code FixedPreciseClock} that always returns the epoch.
     */
    public FixedPreciseClock() {
        this(0);
    }

    /**
     * Constructs a {@code FixedPreciseClock} that always returns the specified time in milliseconds since the epoch.
     * @param currentTimeMillis milliseconds since the epoch
     */
    public FixedPreciseClock(final long currentTimeMillis) {
        this(currentTimeMillis, 0);
    }

    /**
     * Constructs a {@code FixedPreciseClock} that always returns the specified time in milliseconds since the epoch
     * and nanosecond of the millisecond.
     * @param currentTimeMillis milliseconds since the epoch
     * @param nanosOfMillisecond nanosecond of the specified millisecond
     */
    public FixedPreciseClock(final long currentTimeMillis, final int nanosOfMillisecond) {
        this.currentTimeMillis = currentTimeMillis;
        this.nanosOfMillisecond = nanosOfMillisecond;
    }

    @Override
    public void init(final MutableInstant instant) {
        instant.initFromEpochMilli(currentTimeMillis, nanosOfMillisecond);
    }

    @Override
    public long currentTimeMillis() {
        return currentTimeMillis;
    }
}
