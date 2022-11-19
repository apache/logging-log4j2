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
package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.time.internal.CachedClock;
import org.apache.logging.log4j.core.time.internal.CoarseCachedClock;
import org.apache.logging.log4j.core.time.internal.SystemClock;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.util.Lazy;

/**
 * Factory for {@code Clock} objects.
 *
 * @since 2.11
 */
public final class ClockFactory {

    /**
     * Name of the system property that can be used to specify a {@code Clock}
     * implementation class. The value of this property is {@value}.
     */
    public static final String PROPERTY_NAME = Log4jProperties.CONFIG_CLOCK;
    private static final Lazy<Clock> FALLBACK = Lazy.lazy(() -> {
        // TODO(ms): split out clock bindings for smaller fallback init
        final Injector injector = DI.createInjector();
        injector.init();
        return injector.getInstance(Clock.KEY);
    });

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
    @Deprecated
    public static Clock getClock() {
        return FALLBACK.value();
    }

}
