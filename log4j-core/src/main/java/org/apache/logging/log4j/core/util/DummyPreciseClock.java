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
package org.apache.logging.log4j.core.util;

/**
 * Implementation of the {@code PreciseClock} interface that always returns a fixed value.
 * @since 2.11
 */
public class DummyPreciseClock implements PreciseClock {
    private final long currentTimeMillis;
    private final int nanosOfMillisecond;

    public DummyPreciseClock() {
        this(0);
    }

    public DummyPreciseClock(final long currentTimeMillis) {
        this(currentTimeMillis, 0);
    }

    public DummyPreciseClock(final long currentTimeMillis, final int nanosOfMillisecond) {
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
