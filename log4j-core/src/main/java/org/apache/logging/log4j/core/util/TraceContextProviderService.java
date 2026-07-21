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

import java.util.Iterator;
import java.util.ServiceLoader;
import org.apache.logging.log4j.spi.TraceContextProvider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Service registry designed to discover, cache, and safely query the active {@link TraceContextProvider}.
 */
public final class TraceContextProviderService {

    private static final String GLOBAL_KEY = "log4j2.activeTraceContextProvider";
    private static volatile TraceContextProvider ACTIVE_PROVIDER;

    static {
        TraceContextProvider found = null;
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = TraceContextProviderService.class.getClassLoader();
            }

            ServiceLoader<TraceContextProvider> loader = ServiceLoader.load(TraceContextProvider.class, cl);
            Iterator<TraceContextProvider> iterator = loader.iterator();

            if (!iterator.hasNext() && cl != TraceContextProviderService.class.getClassLoader()) {
                loader = ServiceLoader.load(
                        TraceContextProvider.class, TraceContextProviderService.class.getClassLoader());
                iterator = loader.iterator();
            }

            if (iterator.hasNext()) {
                found = iterator.next();
                StatusLogger.getLogger()
                        .info("Using TraceContextProvider: {}", found.getClass().getName());
            }
        } catch (final Throwable t) {
            StatusLogger.getLogger().warn("Error loading TraceContextProvider service", t);
        }
        ACTIVE_PROVIDER = found != null ? found : NoOpTraceContextProvider.INSTANCE;
    }

    public static TraceContextProvider getActiveProvider() {
        try {
            final Object globalProvider = System.getProperties().get(GLOBAL_KEY);
            if (globalProvider instanceof TraceContextProvider) {
                return (TraceContextProvider) globalProvider;
            }
        } catch (final SecurityException ignored) {
            // Gracefully ignore SecurityManager restrictions in strict environments
        }
        return ACTIVE_PROVIDER;
    }

    public static void setActiveProvider(final TraceContextProvider provider) {
        try {
            if (provider == null) {
                System.getProperties().remove(GLOBAL_KEY);
                ACTIVE_PROVIDER = NoOpTraceContextProvider.INSTANCE;
            } else {
                System.getProperties().put(GLOBAL_KEY, provider);
                ACTIVE_PROVIDER = provider;
            }
        } catch (final SecurityException ignored) {
            // Fallback for strict environments
            ACTIVE_PROVIDER = provider != null ? provider : NoOpTraceContextProvider.INSTANCE;
        }
    }

    public static String getTraceId() {
        try {
            return getActiveProvider().getTraceId();
        } catch (final Throwable t) {
            return null;
        }
    }

    public static String getSpanId() {
        try {
            return getActiveProvider().getSpanId();
        } catch (final Throwable t) {
            return null;
        }
    }

    public static String getTraceFlags() {
        try {
            return getActiveProvider().getTraceFlags();
        } catch (final Throwable t) {
            return null;
        }
    }

    private static final class NoOpTraceContextProvider implements TraceContextProvider {
        static final NoOpTraceContextProvider INSTANCE = new NoOpTraceContextProvider();

        @Override
        public String getTraceId() {
            return null;
        }

        @Override
        public String getSpanId() {
            return null;
        }

        @Override
        public String getTraceFlags() {
            return null;
        }
    }

    private TraceContextProviderService() {}
}
