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
package org.apache.logging.log4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.test.TestConstants;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The shutdown hook registered by {@link LoggerContext#start(Configuration)} must use that configuration's timeout.
 */
@SetTestProperty(key = TestConstants.LOGGER_CONTEXT_SHUTDOWN_HOOK_ENABLED, value = "true")
class LoggerContextShutdownHookTimeoutTest {

    private static final long CONFIGURED_TIMEOUT_MILLIS = 5_000L;

    private RecordingContext context;

    @AfterEach
    void stopContext() {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    void startWithConfigurationUsesThatShutdownTimeout() {
        final RecordingRegistry registry = new RecordingRegistry();
        final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
        instanceFactory.registerBinding(ShutdownCallbackRegistry.KEY, () -> registry);
        context = new RecordingContext(instanceFactory);
        final TimeoutConfiguration configuration = new TimeoutConfiguration(context, CONFIGURED_TIMEOUT_MILLIS);

        context.start(configuration);

        assertEquals(CONFIGURED_TIMEOUT_MILLIS, configuration.getShutdownTimeoutMillis());
        assertEquals(1, registry.callbacks.size(), "shutdown hook was not registered");
        context.captureStop = true;
        registry.callbacks.get(0).run();
        assertEquals(
                CONFIGURED_TIMEOUT_MILLIS,
                context.observedTimeoutMillis,
                "shutdown hook stopped the context with the default configuration timeout");
    }

    private static final class RecordingContext extends LoggerContext {

        private boolean captureStop;
        private long observedTimeoutMillis = -1L;

        private RecordingContext(final ConfigurableInstanceFactory instanceFactory) {
            super("shutdown-hook-timeout", null, (URI) null, instanceFactory);
        }

        @Override
        public boolean stop(final long timeout, final TimeUnit timeUnit) {
            if (captureStop) {
                observedTimeoutMillis = timeUnit.toMillis(timeout);
                captureStop = false;
                return true;
            }
            return super.stop(timeout, timeUnit);
        }
    }

    private static final class TimeoutConfiguration extends AbstractConfiguration {

        private TimeoutConfiguration(final LoggerContext loggerContext, final long timeoutMillis) {
            super(loggerContext, ConfigurationSource.NULL_SOURCE);
            setName("Timeout");
            shutdownTimeoutMillis = timeoutMillis;
            getRootLogger().setLevel(Level.OFF);
        }
    }

    private static final class RecordingRegistry implements ShutdownCallbackRegistry {

        private final List<Runnable> callbacks = new ArrayList<>();

        @Override
        public Cancellable addShutdownCallback(final Runnable callback) {
            callbacks.add(callback);
            return new Cancellable() {
                @Override
                public void cancel() {
                    callbacks.remove(callback);
                }

                @Override
                public void run() {
                    callback.run();
                }
            };
        }
    }
}
