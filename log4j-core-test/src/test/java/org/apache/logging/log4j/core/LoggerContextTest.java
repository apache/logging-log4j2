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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.impl.internal.InternalLoggerContext;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.jupiter.api.Test;

/**
 * Validate Logging after Shutdown.
 */
public class LoggerContextTest {

    @Test
    public void shutdownTest() {
        LoggerContextFactory contextFactory = LogManager.getFactory();
        assertTrue(contextFactory instanceof Log4jContextFactory);
        Log4jContextFactory factory = (Log4jContextFactory) contextFactory;
        ShutdownCallbackRegistry registry = factory.getShutdownCallbackRegistry();
        assertTrue(registry instanceof DefaultShutdownCallbackRegistry);
        ((DefaultShutdownCallbackRegistry) registry).start();
        ((DefaultShutdownCallbackRegistry) registry).stop();
        LoggerContext loggerContext = factory.getContext(LoggerContextTest.class.getName(), null, null, false);
        assertTrue(loggerContext instanceof InternalLoggerContext);
    }
}
