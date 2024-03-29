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
package org.apache.logging.log4j.core.impl;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import org.apache.logging.log4j.core.impl.CoreProperties.ThreadContextProperties;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.Constants;

/**
 * Binding for the Log4j API.
 */
@ServiceProvider(value = Provider.class, resolution = Resolution.OPTIONAL)
public class Log4jProvider extends Provider {

    final ConfigurableInstanceFactory instanceFactory;

    public Log4jProvider() {
        this(DI.createInitializedFactory());
    }

    @Inject
    public Log4jProvider(final ConfigurableInstanceFactory instanceFactory) {
        super(10, CURRENT_VERSION, Log4jContextFactory.class);
        this.instanceFactory = instanceFactory;
        instanceFactory.registerBinding(Key.forClass(Provider.class), () -> this);
        instanceFactory.registerBinding(Key.forClass(Log4jProvider.class), () -> this);
        instanceFactory.registerBinding(Key.forClass(ThreadContextMap.class), super::getThreadContextMapInstance);
    }

    @Override
    public LoggerContextFactory getLoggerContextFactory() {
        return instanceFactory.getInstance(Key.forClass(LoggerContextFactory.class));
    }

    @Override
    public String getThreadContextMap() {
        final PropertyEnvironment environment = instanceFactory.getInstance(PropertyEnvironment.class);
        final ThreadContextProperties threadContext = environment.getProperty(ThreadContextProperties.class);
        if (threadContext.enable() && threadContext.enableMap()) {
            if (threadContext.mapClass() != null) {
                return threadContext.mapClass();
            }
            if (Constants.ENABLE_THREADLOCALS) {
                return threadContext.garbageFree() ? GARBAGE_FREE_CONTEXT_MAP : COPY_ON_WRITE_CONTEXT_MAP;
            }
            return WEB_APP_CONTEXT_MAP;
        }
        return NO_OP_CONTEXT_MAP;
    }

    @Override
    public ThreadContextMap getThreadContextMapInstance() {
        return instanceFactory.getInstance(Key.forClass(ThreadContextMap.class));
    }
}
