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
package org.apache.logging.log4j.core.util;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class ShutdownCallbackRegistryTest {

    @Rule
    public final LoggerContextRule ctx = new LoggerContextRule("ShutdownCallbackRegistryTest.xml");

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.setProperty(ShutdownCallbackRegistry.SHUTDOWN_CALLBACK_REGISTRY, Registry.class.getName());
    }

    @Test
    public void testShutdownCallbackRegistry() throws Exception {
        final LoggerContext context = ctx.getLoggerContext();
        assertTrue("LoggerContext should be started", context.isStarted());
        assertThat(Registry.CALLBACKS, hasSize(1));
        Registry.shutdown();
        assertTrue("LoggerContext should be stopped", context.isStopped());
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
