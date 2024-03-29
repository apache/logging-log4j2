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
package org.apache.logging.log4j.core.test;

import org.jspecify.annotations.Nullable;

/**
 * Constants for system properties used by Log4j Core and other artifacts.
 */
public final class TestConstants {

    private TestConstants() {}

    private static final String ASYNC = "log4j.async.";

    public static final String ASYNC_FORMAT_MESSAGES_IN_BACKGROUND = ASYNC + "formatMessagesInBackground";

    private static final String ASYNC_LOGGER = ASYNC + "logger.";

    public static final String ASYNC_LOGGER_EXCEPTION_HANDLER = ASYNC_LOGGER + "exceptionHandler";

    public static final String ASYNC_LOGGER_RING_BUFFER_SIZE = ASYNC_LOGGER + "ringBufferSize";

    public static final String ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
            ASYNC_LOGGER + "synchronizeEnqueueWhenQueueFull";

    public static final String ASYNC_LOGGER_WAIT_STRATEGY = ASYNC_LOGGER + "waitStrategy.type";

    public static final String ASYNC_LOGGER_CONFIG_RING_BUFFER_SIZE = ASYNC_LOGGER + "ringBufferSize";

    public static final String ASYNC_LOGGER_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
            ASYNC_LOGGER + "synchronizeEnqueueWhenQueueFull";

    public static final String ASYNC_QUEUE_FULL_POLICY_CLASS_NAME = "log4j.async.queueFullPolicy.type";

    private static final String AUTH = "log4j.auth.";

    private static final String AUTH_BASIC = AUTH + "basic.";

    public static final String AUTH_BASIC_USERNAME = AUTH_BASIC + "username";

    public static final String AUTH_BASIC_PASSWORD = AUTH_BASIC + "password";

    private static final String CONFIGURATION = "log4j.configuration.";

    public static final String CONFIGURATION_ALLOWED_PROTOCOLS = CONFIGURATION + "allowedProtocols";

    public static final String CONFIGURATION_CONFIGURATION_FACTORY = CONFIGURATION + "configurationFactory";

    public static final String CONFIGURATION_FILE = CONFIGURATION + "location";

    public static final String CONFIGURATION_RELIABILITY_STRATEGY = CONFIGURATION + "reliabilityStrategy";

    public static final String CONFIGURATION_USE_PRECISE_CLOCK = CONFIGURATION + "usePreciseClock";

    public static final String CONSOLE_JANSI_ENABLED = "log4j.console.jansiEnabled";

    private static final String GC = "log4j.gc.";

    public static final String GC_ENABLE_DIRECT_ENCODERS = GC + "enableDirectEncoders";

    private static final String JNDI = "log4j.jndi.";

    public static final String JNDI_ENABLE_LOOKUP = JNDI + "enableLookup";

    public static final String JNDI_ENABLE_JDBC = JNDI + "enableJdbc";

    private static final String LOGGER_CONTEXT = "log4j.loggerContext.";

    public static final String LOGGER_CONTEXT_FACTORY = LOGGER_CONTEXT + "factory";

    public static final String LOGGER_CONTEXT_SELECTOR = LOGGER_CONTEXT + "selector";

    public static final String LOGGER_CONTEXT_SHUTDOWN_HOOK_ENABLED = LOGGER_CONTEXT + "shutdownHookEnabled";

    private static final String LOG_EVENT = "log4j.logEvent.";

    public static final String LOG_EVENT_FACTORY = LOG_EVENT + "logEventFactory";

    private static final String MESSAGE = "log4j.message.";

    public static final String MESSAGE_FACTORY = MESSAGE + "factory";

    private static final String STATUS_LOGGER = "log4j.statusLogger.";

    public static final String STATUS_LOGGER_DEFAULT_STATUS_LEVEL = STATUS_LOGGER + "defaultStatusLevel";

    public static final String VERSION1_CONFIGURATION = "log4j.configuration";

    public static final String VERSION1_COMPATIBILITY = "log4j1.compatibility";

    private static final String THREAD_CONTEXT = "log4j.threadContext.";

    public static final String THREAD_CONTEXT_CONTEXT_DATA = THREAD_CONTEXT + "contextData";

    public static final String THREAD_CONTEXT_GARBAGE_FREE = THREAD_CONTEXT + "garbageFree";

    public static final String THREAD_CONTEXT_MAP_CLASS = THREAD_CONTEXT + "mapClass";

    public static final String WEB_IS_WEB_APP = "log4j2.isWebApp";

    public static @Nullable String setSystemProperty(final String key, final @Nullable String value) {
        final String oldValue = System.getProperty(key);
        if (value != null) {
            System.setProperty(key, value);
        } else {
            System.clearProperty(key);
        }
        return oldValue;
    }
}
