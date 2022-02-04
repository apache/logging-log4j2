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

package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.Module;
import org.apache.logging.log4j.plugins.di.ReflectionCallerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

public class DefaultModule implements Module {
    private static final ReflectionCallerContext CORE_CALLER_CONTEXT = object -> object.setAccessible(true);
    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public void configure(final Injector injector) {
        injector.setCallerContext(CORE_CALLER_CONTEXT);
        injector.bindIfMissing(Key.forClass(ConfigurationScheduler.class), ConfigurationScheduler::new)
                .bindIfMissing(Key.forClass(NanoClock.class), DummyNanoClock::new);

        injector.bindIfMissing(Constants.LOG_EVENT_FACTORY_KEY, () -> {
                    final String logEventFactoryClassName = PropertiesUtil.getProperties().getStringProperty(Constants.LOG4J_LOG_EVENT_FACTORY);
                    LogEventFactory logEventFactory = null;
                    if (logEventFactoryClassName != null) {
                        try {
                            logEventFactory = injector.getInstance(Loader.loadClass(logEventFactoryClassName).asSubclass(LogEventFactory.class));
                        } catch (final ClassNotFoundException e) {
                            LOGGER.error("Unable to create LogEventFactory {}", logEventFactoryClassName, e);
                        }
                    }
                    return logEventFactory != null ? logEventFactory :
                            Constants.ENABLE_THREADLOCALS ? new ReusableLogEventFactory() : new DefaultLogEventFactory();
                });

        injector.bindIfMissing(Constants.DEFAULT_STATUS_LEVEL_KEY, () -> {
                    final String statusLevel = PropertiesUtil.getProperties().getStringProperty(
                            Constants.LOG4J_DEFAULT_STATUS_LEVEL, Level.ERROR.name());
                    try {
                        return Level.toLevel(statusLevel);
                    } catch (final Exception ex) {
                        return Level.ERROR;
                    }
                });
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE;
    }
}
