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
package org.apache.logging.log4j.async.logger;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.async.logger.internal.AsyncLoggerDefaultBundle;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Lazy;

/**
 * {@code LoggerContext} that creates {@code AsyncLogger} objects.
 */
public class AsyncLoggerContext extends LoggerContext {

    private final Lazy<AsyncLoggerDisruptor> loggerDisruptor = Lazy.lazy(() ->
            getInstanceFactory().getInstance(AsyncLoggerDisruptor.Factory.class).createAsyncLoggerDisruptor(getName()));

    public AsyncLoggerContext(
            final String name,
            final Object externalContext,
            final URI configLocation,
            final ConfigurableInstanceFactory instanceFactory) {
        super(name, externalContext, configLocation, instanceFactory);
        instanceFactory.registerBundle(AsyncLoggerDefaultBundle.class);
        instanceFactory.registerBinding(Key.forClass(AsyncLoggerDisruptor.class), loggerDisruptor);
    }

    @Override
    protected Class<? extends Logger.Builder> getLoggerBuilderClass() {
        return AsyncLogger.Builder.class;
    }

    @Override
    public void setName(final String name) {
        super.setName("AsyncContext[" + name + "]");
        if (loggerDisruptor.isInitialized()) {
            loggerDisruptor.get().setContextName(name);
        }
    }

    @Override
    public void start() {
        getAsyncLoggerDisruptor().start();
        super.start();
    }

    @Override
    public void start(final Configuration config) {
        // If no log4j configuration was found, there are no loggers
        // and there is no point in starting the disruptor (which takes up
        // significant memory and starts a thread).
        if (config instanceof DefaultConfiguration) {
            StatusLogger.getLogger().debug("[{}] Not starting Disruptor for DefaultConfiguration.", getName());
        } else {
            getAsyncLoggerDisruptor().start();
        }
        super.start(config);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        // first stop Disruptor
        if (loggerDisruptor.isInitialized()) {
            loggerDisruptor.get().stop(timeout, timeUnit);
        }
        super.stop(timeout, timeUnit);
        return true;
    }

    @Override
    public boolean includeLocation() {
        return false;
    }

    // package-protected for tests
    AsyncLoggerDisruptor getAsyncLoggerDisruptor() {
        return loggerDisruptor.get();
    }

    public static final class Builder extends LoggerContext.Builder {

        @Inject
        public Builder(final ConfigurableInstanceFactory parentInstanceFactory) {
            super(parentInstanceFactory);
        }

        @Override
        public LoggerContext build() {
            return new AsyncLoggerContext(getContextName(), null, getConfigLocation(), createInstanceFactory());
        }
    }
}
