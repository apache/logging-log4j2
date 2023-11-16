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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ShutdownCallbackRegistryTest {

    @BeforeAll
    public static void setUpClass() {
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty(ShutdownCallbackRegistry.SHUTDOWN_CALLBACK_REGISTRY, Registry.class.getName());
    }

    @AfterAll
    public static void afterClass() {
        System.clearProperty(ShutdownCallbackRegistry.SHUTDOWN_CALLBACK_REGISTRY);
        System.clearProperty("log4j2.is.webapp");
    }

    @Test
    @LoggerContextSource("ShutdownCallbackRegistryTest.xml")
    public void testShutdownCallbackRegistry(final LoggerContext context) {
        assertTrue(context.isStarted(), "LoggerContext should be started");
        assertThat(Registry.CALLBACKS, hasSize(1));
        Registry.shutdown();
        assertTrue(context.isStopped(), "LoggerContext should be stopped");
        assertThat(Registry.CALLBACKS, hasSize(0));
        final ContextSelector selector = ((Log4jContextFactory) LogManager.getFactory()).getSelector();
        assertThat(selector.getLoggerContexts(), not(hasItem(context)));
    }

    public static class Registry implements ShutdownCallbackRegistry {
        private static final Logger LOGGER = StatusLogger.getLogger();
        private static final Collection<Cancellable> CALLBACKS = new ConcurrentLinkedQueue<>();

        @Override
        public Cancellable addShutdownCallback(final Runnable callback) {
            final Cancellable cancellable = new Cancellable() {
                @Override
                public void cancel() {
                    LOGGER.debug("Cancelled shutdown callback: {}", callback);
                    CALLBACKS.remove(this);
                }

                @Override
                public void run() {
                    LOGGER.debug("Called shutdown callback: {}", callback);
                    callback.run();
                }
            };
            CALLBACKS.add(cancellable);
            return cancellable;
        }

        private static void shutdown() {
            for (final Runnable callback : CALLBACKS) {
                LOGGER.debug("Calling shutdown callback: {}", callback);
                callback.run();
            }
            CALLBACKS.clear();
        }
    }
}
