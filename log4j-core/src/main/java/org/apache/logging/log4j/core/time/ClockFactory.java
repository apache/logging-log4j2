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
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.core.time.internal.SystemMillisClock;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.plugins.condition.ConditionalOnMissingBinding;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertyEnvironment;

/**
 * Factory for {@code Clock} objects.
 *
 * @since 2.11
 */
public final class ClockFactory {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Lazy<Clock> FALLBACK = Lazy.lazy(() -> {
        ConfigurableInstanceFactory factory = DI.createFactory();
        factory.registerBundle(new ClockFactory());
        return factory.getInstance(Clock.KEY);
    });

    /**
     * Returns a {@code Clock} instance depending on the value of system
     * property {@link Log4jPropertyKey#CONFIG_CLOCK}.
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
        return FALLBACK.value();
    }

    @ConditionalOnMissingBinding
    @SingletonFactory
    public Clock clock(
            final PropertyEnvironment environment, final InstanceFactory instanceFactory, final ClassLoader classLoader)
            throws ClassNotFoundException {
        final String customClock = environment.getStringProperty(Log4jPropertyKey.CONFIG_CLOCK);
        if (customClock == null) {
            return logSupportedPrecision(new SystemClock());
        }
        switch (customClock) {
            case "SystemClock":
                return logSupportedPrecision(new SystemClock());

            case "SystemMillisClock":
                return logSupportedPrecision(new SystemMillisClock());

            case "CachedClock":
            case "org.apache.logging.log4j.core.time.internal.CachedClock":
                return logSupportedPrecision(CachedClock.instance());

            case "CoarseCachedClock":
            case "org.apache.logging.log4j.core.time.internal.CoarseCachedClock":
                return logSupportedPrecision(CoarseCachedClock.instance());

            default:
                final Class<? extends Clock> clockClass =
                        classLoader.loadClass(customClock).asSubclass(Clock.class);
                return logSupportedPrecision(instanceFactory.getInstance(clockClass));
        }
    }

    private static Clock logSupportedPrecision(final Clock clock) {
        final String support = clock instanceof PreciseClock ? "supports" : "does not support";
        LOGGER.debug("{} {} precise timestamps.", clock.getClass().getName(), support);
        return clock;
    }
}
