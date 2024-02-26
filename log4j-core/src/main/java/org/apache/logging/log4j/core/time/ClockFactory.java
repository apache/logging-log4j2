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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.CoreKeys;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.core.time.internal.SystemMillisClock;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;

/**
 * Factory for {@code Clock} objects.
 *
 * @since 2.11
 */
public final class ClockFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Lazy<Clock> FALLBACK =
            Lazy.lazy(() -> DI.builder().build().getInstance(Clock.KEY));

    /**
     * Returns a {@code Clock} instance depending on the value of system
     * property {@link CoreKeys.Configuration#clock()}.
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
     * @deprecated use dependency injection instead to obtain a {@link Clock} instance
     */
    @Deprecated
    public static Clock getClock() {
        return FALLBACK.get();
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    @Deprecated(forRemoval = true)
    public Clock clock(final PropertyEnvironment environment) {
        final CoreKeys.Configuration configuration = environment.getProperty(CoreKeys.Configuration.class);
        if (configuration.clock() == null) {
            return logSupportedPrecision(new SystemClock());
        }
        return switch (configuration.clock()) {
            case "SystemMillisClock" -> logSupportedPrecision(new SystemMillisClock());
            case "CachedClock", "org.apache.logging.log4j.core.time.internal.CachedClock" -> logSupportedPrecision(
                    CachedClock.instance());
            case "CoarseCachedClock",
                    "org.apache.logging.log4j.core.time.internal.CoarseCachedClock" -> logSupportedPrecision(
                    CoarseCachedClock.instance());
            default -> logSupportedPrecision(new SystemClock());
        };
    }

    private static Clock logSupportedPrecision(final Clock clock) {
        final String support = clock instanceof PreciseClock ? "supports" : "does not support";
        LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
        return clock;
    }
}
