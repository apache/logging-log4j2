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
package org.apache.logging.log4j.core.async;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.ClassFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertyResolver;

/**
 * {@code LoggerContext} that creates {@code AsyncLogger} objects.
 */
public class AsyncLoggerContext extends LoggerContext {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends LoggerContext.Builder {
        private ClassFactory classFactory;
        private Supplier<AsyncQueueFullPolicy> asyncQueueFullPolicySupplier;
        private Supplier<AsyncWaitStrategyFactory> asyncWaitStrategyFactorySupplier;

        @Override
        public Builder setName(final String name) {
            super.setName(name);
            return this;
        }

        @Override
        public Builder setKey(final String key) {
            super.setKey(key);
            return this;
        }

        @Override
        public Builder setExternalContext(final Object externalContext) {
            super.setExternalContext(externalContext);
            return this;
        }

        @Override
        public Builder setConfigLocation(final URI configLocation) {
            super.setConfigLocation(configLocation);
            return this;
        }

        @Override
        public Builder setConfigLocation(final String configLocation) {
            super.setConfigLocation(configLocation);
            return this;
        }

        @Override
        public Builder setInjector(final Injector injector) {
            super.setInjector(injector);
            return this;
        }

        @Override
        public Builder setPropertyResolver(final PropertyResolver propertyResolver) {
            super.setPropertyResolver(propertyResolver);
            return this;
        }

        @Override
        public Builder setMessageFactory(final MessageFactory messageFactory) {
            super.setMessageFactory(messageFactory);
            return this;
        }

        @Override
        public Builder setFlowMessageFactory(final FlowMessageFactory flowMessageFactory) {
            super.setFlowMessageFactory(flowMessageFactory);
            return this;
        }

        public ClassFactory getClassFactory() {
            if (classFactory == null) {
                classFactory = getInjector().getInstance(ClassFactory.class);
            }
            return classFactory;
        }

        public Builder setClassFactory(final ClassFactory classFactory) {
            this.classFactory = classFactory;
            return this;
        }

        public Supplier<AsyncQueueFullPolicy> getAsyncQueueFullPolicySupplier() {
            if (asyncQueueFullPolicySupplier == null) {
                asyncQueueFullPolicySupplier =
                        new AsyncQueueFullPolicyFactory(getPropertyResolver(), getClassFactory(), getInjector());
            }
            return asyncQueueFullPolicySupplier;
        }

        public Builder setAsyncQueueFullPolicySupplier(final Supplier<AsyncQueueFullPolicy> asyncQueueFullPolicySupplier) {
            this.asyncQueueFullPolicySupplier = asyncQueueFullPolicySupplier;
            return this;
        }

        public Supplier<AsyncWaitStrategyFactory> getAsyncWaitStrategyFactorySupplier() {
            return asyncWaitStrategyFactorySupplier;
        }

        public Builder setAsyncWaitStrategyFactorySupplier(final Supplier<AsyncWaitStrategyFactory> asyncWaitStrategyFactorySupplier) {
            this.asyncWaitStrategyFactorySupplier = asyncWaitStrategyFactorySupplier;
            return this;
        }

        @Override
        public AsyncLoggerContext get() {
            final PropertyResolver propertyResolver = getPropertyResolver();
            final ClassFactory classFactory = getClassFactory();
            final Injector injector = getInjector();
            final DisruptorConfiguration disruptorConfiguration =
                    new DisruptorConfiguration(propertyResolver, classFactory, injector);
            final Supplier<AsyncQueueFullPolicy> asyncQueueFullPolicySupplier = getAsyncQueueFullPolicySupplier();
            final AtomicReference<LoggerContext> loggerContextRef = new AtomicReference<>();
            Supplier<AsyncWaitStrategyFactory> asyncWaitStrategyFactorySupplier = getAsyncWaitStrategyFactorySupplier();
            if (asyncWaitStrategyFactorySupplier == null) {
                asyncWaitStrategyFactorySupplier = () -> loggerContextRef.get()
                        .getConfiguration().getAsyncWaitStrategyFactory();
            }
            final AsyncLoggerDisruptor disruptor = new AsyncLoggerDisruptor(getName(), disruptorConfiguration,
                    asyncQueueFullPolicySupplier, asyncWaitStrategyFactorySupplier);
            final AsyncLoggerContext context = new AsyncLoggerContext(getName(), getKey(), getExternalContext(),
                    getConfigLocation(), injector, propertyResolver, getMessageFactory(), getFlowMessageFactory(),
                    disruptor);
            loggerContextRef.set(context);
            return context;
        }
    }

    private final AsyncLoggerDisruptor loggerDisruptor;

    AsyncLoggerContext(final String contextName, final String contextKey, final Object externalContext,
                       final URI configLocation, final Injector injector, final PropertyResolver propertyResolver,
                       final MessageFactory messageFactory, final FlowMessageFactory flowMessageFactory,
                       final AsyncLoggerDisruptor loggerDisruptor) {
        super(contextName, contextKey, externalContext, configLocation, injector, propertyResolver, messageFactory,
                flowMessageFactory);
        this.loggerDisruptor = loggerDisruptor;
    }

    @Override
    protected Logger newInstance(final LoggerContext ctx, final String name, final MessageFactory messageFactory) {
        return new AsyncLogger(ctx, name, messageFactory, loggerDisruptor);
    }

    @Override
    public void setName(final String name) {
        super.setName(name);
        loggerDisruptor.setContextName(name);
    }

    @Override
    public void setKey(final String key) {
        super.setKey("AsyncContext[" + key + ']');
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.LoggerContext#start()
     */
    @Override
    public void start() {
        loggerDisruptor.start();
        super.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.LoggerContext#start(org.apache.logging.log4j.core.config.Configuration)
     */
    @Override
    public void start(final Configuration config) {
        maybeStartHelper(config);
        super.start(config);
    }

    private void maybeStartHelper(final Configuration config) {
        // If no log4j configuration was found, there are no loggers
        // and there is no point in starting the disruptor (which takes up
        // significant memory and starts a thread).
        if (config instanceof DefaultConfiguration) {
            StatusLogger.getLogger().debug("[{}] Not starting Disruptor for DefaultConfiguration.", getName());
        } else {
            loggerDisruptor.start();
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        // first stop Disruptor
        loggerDisruptor.stop(timeout, timeUnit);
        super.stop(timeout, timeUnit);
        return true;
    }

    /**
     * Creates and returns a new {@code RingBufferAdmin} that instruments the ringbuffer of the {@code AsyncLogger}
     * objects in this {@code LoggerContext}.
     *
     * @return a new {@code RingBufferAdmin} that instruments the ringbuffer
     */
    public RingBufferAdmin createRingBufferAdmin() {
        return loggerDisruptor.createRingBufferAdmin(getName());
    }

    /**
     * Signals this context whether it is allowed to use ThreadLocal objects for efficiency.
     * @param useThreadLocals whether this context is allowed to use ThreadLocal objects
     */
    public void setUseThreadLocals(final boolean useThreadLocals) {
        loggerDisruptor.setUseThreadLocals(useThreadLocals);
    }
}
