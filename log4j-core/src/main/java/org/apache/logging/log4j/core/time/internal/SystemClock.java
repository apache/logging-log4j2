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
package org.apache.logging.log4j.core.time.internal;

import java.time.Instant;

import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.time.PreciseClock;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Implementation of the {@code Clock} interface that returns the system time.
 * @since 2.11.0
 */
// Precise clock is not implemented because the instant() method in the init method is not garbage free.
public final class SystemClock implements Clock, PreciseClock {

    private static final boolean USE_PRECISE_CLOCK = PropertiesUtil.getProperties()
            .getBooleanProperty("log4j2.usePreciseClock", false);
    /**
     * Returns the system time.
     * @return the result of calling {@code System.currentTimeMillis()}
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(MutableInstant mutableInstant) {
        if (USE_PRECISE_CLOCK) {
            Instant instant = java.time.Clock.systemUTC().instant();
            mutableInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
        } else {
            mutableInstant.initFromEpochMilli(System.currentTimeMillis(), 0);
        }
    }
}
