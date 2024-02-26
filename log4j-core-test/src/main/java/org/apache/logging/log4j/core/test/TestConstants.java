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
 * Constants for system properties used by Log4j Core.
 */
public final class TestConstants {

    private TestConstants() {}

    private static final String ASYNC = "log4j.Async.";

    public static final String ASYNC_FORMAT_MESSAGES_IN_BACKGROUND = ASYNC + "formatMessagesInBackground";

    private static final String ASYNC_LOGGER = "log4j.AsyncLogger.";

    public static final String ASYNC_LOGGER_EXCEPTION_HANDLER = ASYNC_LOGGER + "exceptionHandler";

    public static final String ASYNC_LOGGER_RING_BUFFER_SIZE = ASYNC_LOGGER + "ringBufferSize";

    public static final String ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
            ASYNC_LOGGER + "synchronizeEnqueueWhenQueueFull";

    public static final String ASYNC_LOGGER_WAIT_STRATEGY = ASYNC_LOGGER + "waitStrategy";

    private static final String ASYNC_LOGGER_CONFIG = "log4j.AsyncLoggerConfig.";

    public static final String ASYNC_LOGGER_CONFIG_RING_BUFFER_SIZE = ASYNC_LOGGER_CONFIG + "ringBufferSize";

    public static final String ASYNC_LOGGER_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
            ASYNC_LOGGER_CONFIG + "synchronizeEnqueueWhenQueueFull";

    public static final String ASYNC_QUEUE_FULL_POLICY_CLASS_NAME = "log4j.AsyncQueueFullPolicy.className";

    private static final String CONFIGURATION = "log4j.Configuration.";

    public static final String CONFIGURATION_ALLOWED_PROTOCOLS = CONFIGURATION + "allowedProtocols";

    private static final String CONFIGURATION_BASIC_AUTH = CONFIGURATION + "basicAuth.";

    public static final String CONFIGURATION_BASIC_AUTH_USERNAME = CONFIGURATION_BASIC_AUTH + "username";

    public static final String CONFIGURATION_BASIC_AUTH_PASSWORD = CONFIGURATION_BASIC_AUTH + "password";

    public static final String CONFIGURATION_CONFIGURATION_FACTORY = CONFIGURATION + "configurationFactory";

    public static final String CONFIGURATION_FILE = CONFIGURATION + "file";

    public static final String CONFIGURATION_RELIABILITY_STRATEGY = CONFIGURATION + "reliabilityStrategy";

    public static final String CONFIGURATION_USE_PRECISE_CLOCK = CONFIGURATION + "usePreciseClock";

    public static final String CONSOLE_JANSI_ENABLED = "log4j.Console.jansiEnabled";

    private static final String GC = "log4j.GC.";

    public static final String GC_ENABLE_DIRECT_ENCODERS = GC + "enableDirectEncoders";

    private static final String JNDI = "log4j.JNDI.";

    public static final String JNDI_CONTEXT_SELECTOR = JNDI + "contextSelector";

    public static final String JNDI_ENABLE_LOOKUP = JNDI + "enableLookup";

    public static final String JNDI_ENABLE_JDBC = JNDI + "enableJDBC";

    public static final String JNDI_ENABLE_JMS = JNDI + "enableJMS";

    private static final String LOGGER_CONTEXT = "log4j.LoggerContext.";

    public static final String LOGGER_CONTEXT_LOG_EVENT_FACTORY = LOGGER_CONTEXT + "logEventFactory";

    public static final String LOGGER_CONTEXT_SELECTOR = LOGGER_CONTEXT + "selector";

    private static final String MESSAGE = "log4j.Message.";

    public static final String MESSAGE_FACTORY = MESSAGE + "factory";

    private static final String STATUS_LOGGER = "log4j.StatusLogger.";

    public static final String STATUS_LOGGER_DEFAULT_STATUS_LEVEL = STATUS_LOGGER + "defaultStatusLevel";

    public static final String VERSION1_CONFIGURATION = "log4j.configuration";

    public static final String VERSION1_COMPATIBILITY = "log4j1.compatibility";

    private static final String THREAD_CONTEXT = "log4j.ThreadContext.";

    public static final String THREAD_CONTEXT_CONTEXT_DATA = THREAD_CONTEXT + "contextData";

    public static final String THREAD_CONTEXT_GARBAGE_FREE = THREAD_CONTEXT + "garbageFree";

    public static final String THREAD_CONTEXT_MAP_CLASS = THREAD_CONTEXT + "mapClass";

    private static final String WEB = "log4j.WEB.";

    public static final String WEB_IS_WEB_APP = WEB + "isWebApp";

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
