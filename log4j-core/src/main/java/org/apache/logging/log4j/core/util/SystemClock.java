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

import java.time.Instant;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.time.PreciseClock;

/**
 * Implementation of the {@code Clock} interface that returns the system time.
 */
public final class SystemClock implements Clock, PreciseClock {

    /**
     * Returns the system time.
     * @return the result of calling {@code System.currentTimeMillis()}
     */
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public void init(final MutableInstant mutableInstant) {
        final Instant instant = java.time.Clock.systemUTC().instant();
        mutableInstant.initFromEpochSecond(instant.getEpochSecond(), instant.getNano());
    }
}
