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

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.time.PreciseClock;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Supplier;

/**
 * Factory for {@code Clock} objects.
 */
public final class ClockFactory {

    /**
     * Name of the system property that can be used to specify a {@code Clock}
     * implementation class. The value of this property is {@value}.
     */
    public static final String PROPERTY_NAME = "log4j.Clock";

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    // private static final Clock clock = createClock();

    private ClockFactory() {}

    /**
     * Returns a {@code Clock} instance depending on the value of system
     * property {@link #PROPERTY_NAME}.
     * <p>
     * If system property {@code log4j.Clock=CachedClock} is specified,
     * this method returns an instance of {@link CachedClock}. If system
     * property {@code log4j.Clock=CoarseCachedClock} is specified, this
     * method returns an instance of {@link CoarseCachedClock}.
     * </p>
     * <p>
     * If another value is specified, this value is taken as the fully qualified
     * class name of a class that implements the {@code Clock} interface. An
     * object of this class is instantiated and returned.
     * </p>
     * <p>
     * If no value is specified, or if the specified value could not correctly
     * be instantiated or did not implement the {@code Clock} interface, then an
     * instance of {@link SystemClock} is returned.
     * </p>
     *
     * @return a {@code Clock} instance
     */
    public static Clock getClock() {
        return createClock();
    }

    private static Map<String, Supplier<Clock>> aliases() {
        final Map<String, Supplier<Clock>> result = new HashMap<>();
        result.put("SystemClock", SystemClock::new);
        result.put("SystemMillisClock", SystemMillisClock::new);
        result.put("CachedClock", CachedClock::instance);
        result.put("CoarseCachedClock", CoarseCachedClock::instance);
        result.put("org.apache.logging.log4j.core.util.CachedClock", CachedClock::instance);
        result.put("org.apache.logging.log4j.core.util.CoarseCachedClock", CoarseCachedClock::instance);
        return result;
    }

    private static Clock createClock() {
        final String userRequest = PropertiesUtil.getProperties().getStringProperty(PROPERTY_NAME);
        if (userRequest == null) {
            LOGGER.trace("Using default SystemClock for timestamps.");
            return logSupportedPrecision(new SystemClock());
        }
        final Supplier<Clock> specified = aliases().get(userRequest);
        if (specified != null) {
            LOGGER.trace("Using specified {} for timestamps.", userRequest);
            return logSupportedPrecision(specified.get());
        }
        try {
            final Clock result = Loader.newCheckedInstanceOf(userRequest, Clock.class);
            LOGGER.trace("Using {} for timestamps.", result.getClass().getName());
            return logSupportedPrecision(result);
        } catch (final Exception e) {
            final String fmt = "Could not create {}: {}, using default SystemClock for timestamps.";
            LOGGER.error(fmt, userRequest, e);
            return logSupportedPrecision(new SystemClock());
        }
    }

    private static Clock logSupportedPrecision(final Clock clock) {
        final String support = clock instanceof PreciseClock ? "supports" : "does not support";
        LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
        return clock;
    }
}
