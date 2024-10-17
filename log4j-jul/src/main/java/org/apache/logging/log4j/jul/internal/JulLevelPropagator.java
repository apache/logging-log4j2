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
package org.apache.logging.log4j.jul.internal;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.logging.jul.tolog4j.LevelTranslator;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.jul.spi.LevelChangePropagator;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Propagates Log4j Core level to JUL.
 */
public class JulLevelPropagator implements LevelChangePropagator, Consumer<Configuration> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    public static final LevelChangePropagator INSTANCE = new JulLevelPropagator();

    private final AtomicInteger installCount = new AtomicInteger();
    private LoggerContext context;
    /** Save "hard" references to configured JUL loggers. */
    private final Set<java.util.logging.Logger> julLoggerRefs = new HashSet<>();

    @Override
    public void start() {
        if (installCount.getAndIncrement() == 0) {
            context = LoggerContext.getContext(false);
            LOGGER.info("Installing Log4j Core to JUL level propagator for context `{}`", context);
            context.addConfigurationStartedListener(this);
            propagateLogLevels(context.getConfiguration());
        }
    }

    @Override
    public void stop() {
        if (installCount.decrementAndGet() == 0) {
            LOGGER.info("Uninstalling Log4j Core to JUL level propagator for context `{}`", context);
            context.removeConfigurationStartedListener(this);
            context = null;
            julLoggerRefs.clear();
        }
    }

    @Override
    public void accept(Configuration configuration) {
        propagateLogLevels(configuration);
    }

    private void propagateLogLevels(final Configuration configuration) {
        LOGGER.info("Starting Log4j Core to JUL level propagation for configuration `{}`", configuration);
        // clear or init. saved JUL logger references
        // JUL loggers have to be explicitly referenced because JUL internally uses
        // weak references so not instantiated loggers may be garbage collected
        // and their level config gets lost then.
        julLoggerRefs.clear();

        final Map<String, LoggerConfig> log4jLoggers = configuration.getLoggers();
        for (LoggerConfig loggerConfig : log4jLoggers.values()) {
            final java.util.logging.Logger julLog =
                    java.util.logging.Logger.getLogger(loggerConfig.getName()); // this also fits for root = ""
            final java.util.logging.Level julLevel =
                    LevelTranslator.toJavaLevel(loggerConfig.getLevel()); // loggerConfig.getLevel() never returns null
            julLog.setLevel(julLevel);
            julLoggerRefs.add(julLog);
        }
        final java.util.logging.LogManager julMgr = java.util.logging.LogManager.getLogManager();
        for (Enumeration<String> en = julMgr.getLoggerNames(); en.hasMoreElements(); ) {
            final java.util.logging.Logger julLog = julMgr.getLogger(en.nextElement());
            if (julLog != null
                    && julLog.getLevel() != null
                    && !"".equals(julLog.getName())
                    && !log4jLoggers.containsKey(julLog.getName())) {
                julLog.setLevel(null);
            }
        }
    }
}
