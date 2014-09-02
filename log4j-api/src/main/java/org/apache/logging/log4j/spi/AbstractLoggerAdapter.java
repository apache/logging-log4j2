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
package org.apache.logging.log4j.spi;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides an abstract base class to use for implementing ExternalLoggerContextRegistry.
 * @since 2.1
 */
public abstract class AbstractLoggerAdapter<L> implements LoggerAdapter<L> {

    /**
     * A map to store loggers for their given LoggerContexts.
     */
    protected final Map<LoggerContext, ConcurrentMap<String, L>> registry =
        new WeakHashMap<LoggerContext, ConcurrentMap<String, L>>();

    @Override
    public L getLogger(String name) {
        final LoggerContext context = getContext();
        final ConcurrentMap<String, L> loggers = getLoggersInContext(context);
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        loggers.putIfAbsent(name, newLogger(name, context));
        return loggers.get(name);
    }

    @Override
    public ConcurrentMap<String, L> getLoggersInContext(final LoggerContext context) {
        synchronized (registry) {
            ConcurrentMap<String, L> loggers = registry.get(context);
            if (loggers == null) {
                loggers = new ConcurrentHashMap<String, L>();
                registry.put(context, loggers);
            }
            return loggers;
        }
    }

    @Override
    public void stop() {
        registry.clear();
    }
}
