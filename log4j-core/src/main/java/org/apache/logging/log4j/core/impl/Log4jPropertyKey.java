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

import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.PropertyComponent;
import org.apache.logging.log4j.util.PropertyKey;

public enum Log4jPropertyKey implements PropertyKey {

    ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME(PropertyComponent.ASYNC_LOGGER_CONFIG, Constant.EXCEPTION_HANDLER),
    ASYNC_CONFIG_RETRIES(PropertyComponent.ASYNC_LOGGER_CONFIG, Constant.RETRIES),
    ASYNC_CONFIG_RING_BUFFER_SIZE(PropertyComponent.ASYNC_LOGGER_CONFIG, Constant.RING_BUFFER_SIZE),
    ASYNC_CONFIG_SLEEP_TIME_NS(PropertyComponent.ASYNC_LOGGER_CONFIG, Constant.SLEEP_TIME_NS),
    ASYNC_CONFIG_TIMEOUT(PropertyComponent.ASYNC_LOGGER_CONFIG, Constant.TIMEOUT),
    ASYNC_CONFIG_WAIT_STRATEGY(PropertyComponent.ASYNC_LOGGER_CONFIG, Constant.WAIT_STRATEGY),
    ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL(PropertyComponent.ASYNC_LOGGER_CONFIG,
            Constant.SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL),
    ASYNC_LOGGER_DISCARD_THRESHOLD(PropertyComponent.ASYNC_LOGGER, Constant.DISCARD_THRESHOLD),
    ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME(PropertyComponent.ASYNC_LOGGER, Constant.EXCEPTION_HANDLER),
    ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND(PropertyComponent.ASYNC_LOGGER, Constant.FORMAT_MSG),
    ASYNC_LOGGER_QUEUE_FULL_POLICY(PropertyComponent.ASYNC_LOGGER, Constant.QUEUE_FULL_POLICY),
    ASYNC_LOGGER_RETRIES(PropertyComponent.ASYNC_LOGGER, Constant.RETRIES),
    ASYNC_LOGGER_RING_BUFFER_SIZE(PropertyComponent.ASYNC_LOGGER, Constant.RING_BUFFER_SIZE),
    ASYNC_LOGGER_SLEEP_TIME_NS(PropertyComponent.ASYNC_LOGGER, Constant.SLEEP_TIME_NS),
    ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL(PropertyComponent.ASYNC_LOGGER, Constant.SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL),
    ASYNC_LOGGER_THREAD_NAME_STRATEGY(PropertyComponent.ASYNC_LOGGER, Constant.THREAD_NAME_STRATEGY),
    ASYNC_LOGGER_TIMEOUT(PropertyComponent.ASYNC_LOGGER, Constant.TIMEOUT),
    ASYNC_LOGGER_WAIT_STRATEGY(PropertyComponent.ASYNC_LOGGER, Constant.WAIT_STRATEGY),
    CONFIG_AUTH_PROVIDER(PropertyComponent.CONFIGURATION, Constant.AUTH_PROVIDER),
    CONFIG_CLOCK(PropertyComponent.CONFIGURATION, Constant.CLOCK),
    CONFIG_CONFIGURATION_FACTORY_CLASS_NAME(PropertyComponent.CONFIGURATION, Constant.CONFIGURATION_FACTORY),
    CONFIG_DEFAULT_LEVEL(PropertyComponent.CONFIGURATION, Constant.LEVEL),
    CONFIG_LOCATION(PropertyComponent.CONFIGURATION, Constant.FILE),
    CONSOLE_JANSI_ENABLED(PropertyComponent.CONSOLE, Constant.JANSI_ENABLED),
    CONFIG_MERGE_STRATEGY(PropertyComponent.CONFIGURATION, Constant.MERGE_STRATEGY),
    CONFIG_RELIABILITY_STRATEGY(PropertyComponent.CONFIGURATION, Constant.RELIABILITY_STRATEGY),
    CONFIG_RELIABILITY_STRATEGY_AWAIT_UNCONDITIONALLY_MILLIS(PropertyComponent.CONFIGURATION,
            Constant.WAIT_MILLIS_BEFORE_STOP_OLD_CONFIG),
    CONFIG_V1_COMPATIBILITY_ENABLED(PropertyComponent.LOG4J1, Constant.COMPATIBILITY),
    CONFIG_V1_FILE_NAME(PropertyComponent.LOG4J, Constant.CONFIGURATION),
    CONTEXT_SELECTOR_CLASS_NAME(PropertyComponent.LOG4J, Constant.CONTEXT_SELECTOR),
    GC_ENABLE_DIRECT_ENCODERS(PropertyComponent.GC, Constant.ENABLE_DIRECT_ENCODERS),
    GC_ENCODER_BYTE_BUFFER_SIZE(PropertyComponent.GC, Constant.ENCODER_BYTE_BUFFER_SIZE),
    GC_ENCODER_CHAR_BUFFER_SIZE(PropertyComponent.GC, Constant.ENCODER_CHAR_BUFFER_SIZE),
    GC_INITIAL_REUSABLE_MESSAGE_SIZE(PropertyComponent.GC,Constant.INITIAL_REUSABLE_MSG_SIZE),
    GC_LAYOUT_STRING_BUILDER_MAX_SIZE(PropertyComponent.GC, Constant.LAYOUT_STRINGBUILDER_MAX_SIZE),
    GC_REUSABLE_MESSAGE_MAX_SIZE(PropertyComponent.GC,Constant.MAX_REUSABLE_MSG_SIZE),
    JMX_ENABLED(PropertyComponent.JMX, Constant.ENABLED),
    JMX_NOTIFY_ASYNC(PropertyComponent.JMX, Constant.NOTIFY_ASYNC),
    JNDI_CONTEXT_SELECTOR(PropertyComponent.JNDI, Constant.CONTEXT_SELECTOR),
    JNDI_ENABLE_JDBC(PropertyComponent.JNDI, Constant.ENABLE_JDBC),
    JNDI_ENABLE_JMS(PropertyComponent.JNDI, Constant.ENABLE_JMS),
    JNDI_ENABLE_LOOKUP(PropertyComponent.JNDI, Constant.ENABLE_LOOKUP),
    LOG_EVENT_FACTORY_CLASS_NAME(PropertyComponent.LOGGER, Constant.LOG_EVENT_FACTORY),
    SCRIPT_ENABLE_LANGUAGES(PropertyComponent.SCRIPT, Constant.ENABLE_LANGUAGES),
    SHUTDOWN_CALLBACK_REGISTRY(PropertyComponent.LOGGER_CONTEXT, Constant.SHUT_DOWN_CALLBACK_REGISTRY),
    SHUTDOWN_HOOK_ENABLED(PropertyComponent.LOGGER_CONTEXT, Constant.SHUT_DOWN_HOOK_ENABLED),
    STACKTRACE_ON_START(PropertyComponent.LOGGER_CONTEXT, Constant.STACK_TRACE_ON_START),
    STATUS_DEFAULT_LEVEL(PropertyComponent.STATUS_LOGGER, Constant.DEFAULT_STATUS_LEVEL),
    THREAD_CONTEXT_DATA_CLASS_NAME(PropertyComponent.THREAD_CONTEXT,  Constant.CONTEXT_DATA),
    THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME(PropertyComponent.THREAD_CONTEXT, Constant.CONTEXT_DATA_INJECTOR),
    TRANSPORT_SECURITY_TRUST_STORE_LOCATION(PropertyComponent.TRANSPORT_SECURITY, Constant.TRUST_STORE_LOCATION),
    TRANSPORT_SECURITY_TRUST_STORE_PASSWORD(PropertyComponent.TRANSPORT_SECURITY, Constant.TRUST_STORE_PASSWORD),
    TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE(PropertyComponent.TRANSPORT_SECURITY,
            Constant.TRUST_STORE_PASSWORD_FILE),
    TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR(PropertyComponent.TRANSPORT_SECURITY,
            Constant.TRUST_STORE_PASSWORD_ENVIRONMENT_VARIABLE),
    TRANSPORT_SECURITY_TRUST_STORE_TYPE(PropertyComponent.TRANSPORT_SECURITY,
            Constant.TRUST_STORE_TYPE),
    TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM(PropertyComponent.TRANSPORT_SECURITY,
            Constant.TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM),
    TRANSPORT_SECURITY_KEY_STORE_LOCATION(PropertyComponent.TRANSPORT_SECURITY, Constant.KEYSTORE_LOCATION),
    TRANSPORT_SECURITY_KEY_STORE_PASSWORD(PropertyComponent.TRANSPORT_SECURITY, Constant.KEYSTORE_PASSWORD),
    TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE(PropertyComponent.TRANSPORT_SECURITY, Constant.KEYSTORE_PASSWORD_FILE),
    TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR(PropertyComponent.TRANSPORT_SECURITY,
            Constant.KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE),
    TRANSPORT_SECURITY_KEY_STORE_TYPE(PropertyComponent.TRANSPORT_SECURITY, Constant.KEYSTORE_TYPE),
    TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM(PropertyComponent.TRANSPORT_SECURITY,
            Constant.KEYSTORE_KEY_MANAGER_FACTORY_ALGORITHM),
    TRANSPORT_SECURITY_VERIFY_HOST_NAME(PropertyComponent.TRANSPORT_SECURITY,Constant.SSL_VERIFY_HOST_NAME);

    private PropertyComponent component;
    private String name;

    Log4jPropertyKey(final PropertyComponent component, String name) {
        this.component = component;
        this.name = name;
    }

    public static PropertyKey findKey(String component, String name) {
        for (PropertyKey key : values()) {
            if (key.getComponent().equals(component) && key.getName().equals(name)) {
                return key;
            }
        }
        return null;
    }

    public String getComponent() {
        return component.getName();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getKey();
    }

    public static class Constant {
        private static final String DELIM = ".";

        static final String EXCEPTION_HANDLER = "exceptionHandler";
        public static final String ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM + EXCEPTION_HANDLER;
        static final String RETRIES = "retries";
        public static final String ASYNC_CONFIG_RETRIES = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM + RETRIES;
        static final String RING_BUFFER_SIZE = "ringBufferSize";
        public static final String ASYNC_CONFIG_RING_BUFFER_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM + RING_BUFFER_SIZE;
        static final String SLEEP_TIME_NS = "sleepTimeNS";
        public static final String ASYNC_CONFIG_SLEEP_TIME_NS = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM + SLEEP_TIME_NS;
        static final String TIMEOUT = "timeout";
        public static final String ASYNC_CONFIG_TIMEOUT = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM + TIMEOUT;
        static final String WAIT_STRATEGY = "waitStrategy";
        public static final String ASYNC_CONFIG_WAIT_STRATEGY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM + WAIT_STRATEGY;
        static final String SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = "synchronizeEnqueueWhenQueueFull";
        public static final String ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.ASYNC_LOGGER_CONFIG + DELIM
                + SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL;
        static final String DISCARD_THRESHOLD = "discardThreshold";
        public static final String ASYNC_LOGGER_DISCARD_THRESHOLD = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + DISCARD_THRESHOLD;
        public static final String ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.ASYNC_LOGGER + DELIM
                + EXCEPTION_HANDLER;
        static final String FORMAT_MSG = "formatMsg";
        public static final String ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + FORMAT_MSG;
        static final String QUEUE_FULL_POLICY = "queueFullPolicy";
        public static final String ASYNC_LOGGER_QUEUE_FULL_POLICY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + QUEUE_FULL_POLICY;
        public static final String ASYNC_LOGGER_RETRIES = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + RETRIES;
        public static final String ASYNC_LOGGER_RING_BUFFER_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + RING_BUFFER_SIZE;
        public static final String ASYNC_LOGGER_SLEEP_TIME_NS = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + SLEEP_TIME_NS;
        public static final String ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.ASYNC_LOGGER + DELIM
                + SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL;
        static final String THREAD_NAME_STRATEGY = "threadNameStrategy";
        public static final String ASYNC_LOGGER_THREAD_NAME_STRATEGY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + THREAD_NAME_STRATEGY;
        public static final String ASYNC_LOGGER_TIMEOUT = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + TIMEOUT;
        public static final String ASYNC_LOGGER_WAIT_STRATEGY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.ASYNC_LOGGER + DELIM + WAIT_STRATEGY;
        static final String AUTH_PROVIDER = "authorizationProvider";
        public static final String CONFIG_AUTH_PROVIDER = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.CONFIGURATION + DELIM + AUTH_PROVIDER;
        static final String CLOCK = "clock";
        public static final String CONFIG_CLOCK = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.CONFIGURATION + DELIM + CLOCK;
        static final String CONFIGURATION_FACTORY = "configurationFactory";
        public static final String CONFIG_CONFIGURATION_FACTORY_CLASS_NAME =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.CONFIGURATION + DELIM +
                        CONFIGURATION_FACTORY;
        static final String LEVEL = "level";
        public static final String CONFIG_DEFAULT_LEVEL = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.CONFIGURATION + DELIM + LEVEL;
        static final String FILE = "file";
        public static final String CONFIG_LOCATION = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.CONFIGURATION + DELIM + FILE;
        static final String JANSI_ENABLED = "jansiEnabled";
        public static final String CONFIG_JANSI_ENABLED = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.CONFIGURATION + DELIM + JANSI_ENABLED;
        static final String MERGE_STRATEGY = "mergeStrategy";
        public static final String CONFIG_MERGE_STRATEGY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.CONFIGURATION + DELIM + MERGE_STRATEGY;
        static final String RELIABILITY_STRATEGY = "reliabilityStrategy";
        public static final String CONFIG_RELIABILITY_STRATEGY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.CONFIGURATION + DELIM + RELIABILITY_STRATEGY;
        static final String WAIT_MILLIS_BEFORE_STOP_OLD_CONFIG = "waitMillisBeforeStopOldConfig";
        public static final String CONFIG_RELIABILITY_STRATEGY_AWAIT_UNCONDITIONALLY_MILLIS =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.CONFIGURATION + DELIM
                + WAIT_MILLIS_BEFORE_STOP_OLD_CONFIG;
        static final String COMPATIBILITY = "compatibility";
        public static final String CONFIG_V1_COMPATIBILITY_ENABLED = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOG4J1 + DELIM + COMPATIBILITY;
        static final String CONFIGURATION = "configuration";
        public static final String CONFIG_V1_FILE_NAME = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOG4J1 + DELIM + CONFIGURATION;
        static final String CONTEXT_SELECTOR = "contextSelector";
        public static final String CONTEXT_SELECTOR_CLASS_NAME = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOG4J + DELIM + CONTEXT_SELECTOR;
        static final String ENABLE_DIRECT_ENCODERS = "enableDirectEncoders";
        public static final String GC_ENABLE_DIRECT_ENCODERS = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.GC + DELIM + ENABLE_DIRECT_ENCODERS;
        static final String ENCODER_BYTE_BUFFER_SIZE = "encoderByteBufferSize";
        public static final String GC_ENCODER_BYTE_BUFFER_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.GC + DELIM + ENCODER_BYTE_BUFFER_SIZE;
        static final String ENCODER_CHAR_BUFFER_SIZE = "encoderCharBufferSize";
        public static final String GC_ENCODER_CHAR_BUFFER_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.GC + DELIM + ENCODER_CHAR_BUFFER_SIZE;
        static final String INITIAL_REUSABLE_MSG_SIZE = "initialReusableMsgSize";
        public static final String GC_INITIAL_REUSABLE_MESSAGE_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.GC + DELIM + INITIAL_REUSABLE_MSG_SIZE;
        static final String LAYOUT_STRINGBUILDER_MAX_SIZE = "layoutStringBuilderMaxSize";
        public static final String GC_LAYOUT_STRING_BUILDER_MAX_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.GC + DELIM + LAYOUT_STRINGBUILDER_MAX_SIZE;
        static final String MAX_REUSABLE_MSG_SIZE = "maxReusableMsgSize";
        public static final String GC_REUSABLE_MESSAGE_MAX_SIZE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.GC + DELIM + MAX_REUSABLE_MSG_SIZE;
        static final String ENABLED = "enabled";
        public static final String JMX_ENABLED = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.JMX + DELIM + ENABLED;
        static final String NOTIFY_ASYNC = "notifyAsync";
        public static final String JMX_NOTIFY_ASYNC = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.JMX + DELIM + NOTIFY_ASYNC;
        public static final String JNDI_CONTEXT_SELECTOR = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.JNDI + DELIM + CONTEXT_SELECTOR;
        static final String ENABLE_JDBC = "enableJDBC";
        public static final String JNDI_ENABLE_JDBC = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.JNDI + DELIM + ENABLE_JDBC;
        static final String ENABLE_JMS = "enableJMS";
        public static final String JNDI_ENABLE_JMS = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.JNDI + DELIM + ENABLE_JMS;
        static final String ENABLE_LOOKUP = "enableLookup";
        public static final String JNDI_ENABLE_LOOKUP = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.JNDI + DELIM + ENABLE_LOOKUP;
        static final String LOG_EVENT_FACTORY = "logEventFactory";
        public static final String LOG_EVENT_FACTORY_CLASS_NAME = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOGGER_CONTEXT + DELIM + LOG_EVENT_FACTORY;
        static final String ENABLE_LANGUAGES = "enableLanguages";
        public static final String SCRIPT_ENABLE_LANGUAGES = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.SCRIPT + DELIM + ENABLE_LANGUAGES;
        static final String SHUT_DOWN_CALLBACK_REGISTRY = "shutdownCallbackRegistry";
        public static final String SHUTDOWN_CALLBACK_REGISTRY = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOGGER_CONTEXT + DELIM + SHUT_DOWN_CALLBACK_REGISTRY;
        static final String SHUT_DOWN_HOOK_ENABLED = "shutdownHookEnabled";
        public static final String SHUTDOWN_HOOK_ENABLED = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOGGER_CONTEXT + DELIM + SHUT_DOWN_HOOK_ENABLED;
        static final String STACK_TRACE_ON_START = "stacktraceOnStart";
        public static final String STACKTRACE_ON_START = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.LOGGER_CONTEXT + DELIM + STACK_TRACE_ON_START;
        static final String DEFAULT_STATUS_LEVEL = "defaultStatusLevel";
        public static final String STATUS_DEFAULT_LEVEL = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.STATUS_LOGGER + DELIM + DEFAULT_STATUS_LEVEL;
        static final String CONTEXT_DATA = "contextData";
        public static final String THREAD_CONTEXT_DATA_CLASS_NAME = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.THREAD_CONTEXT + DELIM + CONTEXT_DATA;
        static final String CONTEXT_DATA_INJECTOR = "contextDataInjector";
        public static final String THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.THREAD_CONTEXT + DELIM
                        + CONTEXT_DATA_INJECTOR;
        static final String TRUST_STORE_LOCATION = "trustStoreLocation";
        public static final String TRANSPORT_SECURITY_TRUST_STORE_LOCATION =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + TRUST_STORE_LOCATION;
        static final String TRUST_STORE_PASSWORD = "trustStorePassword";
        public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + TRUST_STORE_PASSWORD;
        static final String TRUST_STORE_PASSWORD_FILE = "trustStorePasswordFile";
        public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + TRUST_STORE_PASSWORD_FILE;
        static final String TRUST_STORE_PASSWORD_ENVIRONMENT_VARIABLE = "trustStorePasswordEnvironmentVariable";
        public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                + TRUST_STORE_PASSWORD_ENVIRONMENT_VARIABLE;
        static final String TRUST_STORE_TYPE = "trustStoreType";
        public static final String TRANSPORT_SECURITY_TRUST_STORE_TYPE =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + TRUST_STORE_TYPE;
        static final String TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM = "trustStoreKeyManagerFactoryAlgorithm";
        public static final String TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                + TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM;
        static final String KEYSTORE_LOCATION = "keyStoreLocation";
        public static final String TRANSPORT_SECURITY_KEY_STORE_LOCATION = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM + KEYSTORE_LOCATION;
        static final String KEYSTORE_PASSWORD = "keystorePassword";
        public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM + KEYSTORE_PASSWORD;
        static final String KEYSTORE_PASSWORD_FILE = "keyStorePasswordFile";
        public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + KEYSTORE_PASSWORD_FILE;
        static final String KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE = "keyStorePasswordEnvironmentVariable";
        public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + KEYSTORE_PASSWORD_ENVIRONMENT_VARIABLE;
        static final String KEYSTORE_TYPE = "keyStoreType";
        public static final String TRANSPORT_SECURITY_KEY_STORE_TYPE = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM + KEYSTORE_TYPE;
        static final String KEYSTORE_KEY_MANAGER_FACTORY_ALGORITHM = "keyStoreKeyManagerFactoryAlgorithm";
        public static final String TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM =
                LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM
                        + KEYSTORE_KEY_MANAGER_FACTORY_ALGORITHM;
        static final String SSL_VERIFY_HOST_NAME = "sslVerifyHostName";
        public static final String TRANSPORT_SECURITY_VERIFY_HOST_NAME = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.TRANSPORT_SECURITY + DELIM + SSL_VERIFY_HOST_NAME;
    }
}
