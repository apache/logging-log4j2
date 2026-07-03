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
 * Service registry designed to discover and cache the active {@link TraceContextProvider}
 * natively using the standard Java ServiceLoader.
 */
public final class TraceContextProviderService {

    private static final TraceContextProvider ACTIVE_PROVIDER;

    static {
        TraceContextProvider found = null;
        try {
            // Standard ServiceLoader lookup using the Thread Context ClassLoader (TCCL)
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
        return ACTIVE_PROVIDER;
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
