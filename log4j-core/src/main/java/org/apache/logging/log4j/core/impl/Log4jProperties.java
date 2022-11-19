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
package org.apache.logging.log4j.core.impl;

public class Log4jProperties {
    // TODO: rename properties according to established theme in
    //  https://cwiki.apache.org/confluence/display/LOGGING/Properties+Enhancement

    // LoggerContext.shutdownCallbackRegistry
    public static final String SHUTDOWN_CALLBACK_REGISTRY_CLASS_NAME = "log4j2.shutdownCallbackRegistry";
    // LoggerContext.shutdownHookEnabled
    public static final String SHUTDOWN_HOOK_ENABLED = "log4j2.shutdownHookEnabled";

    // LoggerContext.stacktraceOnStart
    public static final String LOGGER_CONTEXT_STACKTRACE_ON_START = "log4j2.LoggerContext.stacktraceOnStart";

    // Jansi.enabled
    public static final String JANSI_DISABLED = "log4j2.skipJansi";

    // LoggerContext.contextSelector
    public static final String CONTEXT_SELECTOR_CLASS_NAME = "log4j2.contextSelector";

    // LoggerContext.logEventFactory
    public static final String LOG_EVENT_FACTORY_CLASS_NAME = "log4j2.logEventFactory";

    // StatusLogger.defaultStatusLevel
    public static final String STATUS_DEFAULT_LEVEL = "log4j2.defaultStatusLevel";

    // Configuration.level
    public static final String CONFIG_DEFAULT_LEVEL = "log4j2.level";
    // Configuration.clock
    public static final String CONFIG_CLOCK = "log4j2.clock";
    // Configuration.mergeStrategy
    public static final String CONFIG_MERGE_STRATEGY_CLASS_NAME = "log4j2.mergeStrategy";
    // Configuration.reliabilityStrategy
    public static final String CONFIG_RELIABILITY_STRATEGY = "log4j2.reliabilityStrategy";
    // Configuration.reliabilityStrategyAwaitUnconditionallyMillis
    public static final String CONFIG_RELIABILITY_STRATEGY_AWAIT_UNCONDITIONALLY_MILLIS = "log4j2.waitMillisBeforeStopOldConfig";
    // Configuration.factory
    public static final String CONFIG_CONFIGURATION_FACTORY_CLASS_NAME = "log4j2.configurationFactory";
    // Configuration.location
    public static final String CONFIG_LOCATION = "log4j2.configurationFile";
    public static final String CONFIG_V1_FILE_NAME = "log4j.configuration";
    public static final String CONFIG_V1_COMPATIBILITY_ENABLED = "log4j1.compatibility";

    // AsyncLogger.formatMsgAsync
    public static final String ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND = "log4j2.formatMsgAsync";
    // AsyncLogger.queueFullPolicy
    public static final String ASYNC_LOGGER_QUEUE_FULL_POLICY = "log4j2.asyncQueueFullPolicy";
    // AsyncLogger.discardThreshold
    public static final String ASYNC_LOGGER_DISCARD_THRESHOLD = "log4j2.discardThreshold";
    // AsyncLogger.ringBufferSize
    public static final String ASYNC_LOGGER_RING_BUFFER_SIZE = "log4j2.AsyncLogger.ringBufferSize";
    // AsyncLogger.waitStrategy
    public static final String ASYNC_LOGGER_WAIT_STRATEGY = "log4j2.AsyncLogger.waitStrategy";
    // AsyncLogger.synchronizeEnqueueWhenQueueFull
    public static final String ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = "log4j2.AsyncLogger.synchronizeEnqueueWhenQueueFull";
    // AsyncLogger.exceptionHandler
    public static final String ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME = "log4j2.AsyncLogger.exceptionHandler";
    // AsyncLogger.threadNameStrategy
    public static final String ASYNC_LOGGER_THREAD_NAME_STRATEGY = "log4j2.AsyncLogger.threadNameStrategy";
    // AsyncLoggerConfig.ringBufferSize
    public static final String ASYNC_CONFIG_RING_BUFFER_SIZE = "log4j2.AsyncLoggerConfig.ringBufferSize";
    // AsyncLoggerConfig.waitStrategy
    public static final String ASYNC_CONFIG_WAIT_STRATEGY = "log4j2.AsyncLoggerConfig.waitStrategy";
    // AsyncLoggerConfig.synchronizeEnqueueWhenQueueFull
    public static final String ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL = "log4j2.AsyncLoggerConfig.synchronizeEnqueueWhenQueueFull";
    // AsyncLoggerConfig.exceptionHandler
    public static final String ASYNC_CONFIG_EXCEPTION_HANDLER_CLASS_NAME = "log4j2.AsyncLoggerConfig.exceptionHandler";

    // GC.enableDirectEncoders
    public static final String GC_ENABLE_DIRECT_ENCODERS = "log4j2.enableDirectEncoders";
    // GC.initialReusableMsgSize
    public static final String GC_INITIAL_REUSABLE_MESSAGE_SIZE = "log4j2.initialReusableMsgSize";
    // GC.encoderCharBufferSize
    public static final String GC_ENCODER_CHAR_BUFFER_SIZE = "log4j2.encoderCharBufferSize";
    // GC.encoderByteBufferSize
    public static final String GC_ENCODER_BYTE_BUFFER_SIZE = "log4j2.encoderByteBufferSize";
    // GC.layoutStringBuilderMaxSize
    public static final String GC_LAYOUT_STRING_BUILDER_MAX_SIZE = "log4j2.layoutStringBuilderMaxSize";

    // ThreadContext.contextData
    public static final String THREAD_CONTEXT_DATA_CLASS_NAME = "log4j2.contextData";
    // ThreadContext.contextDataInjector
    public static final String THREAD_CONTEXT_DATA_INJECTOR_CLASS_NAME = "log4j2.contextDataInjector";

    // JMX.enabled
    public static final String JMX_DISABLED = "log4j2.disableJmx";
    // JMX.notifyAsync
    public static final String JMX_NOTIFY_ASYNC = "log4j2.jmxNotifyAsync";

    // TransportSecurity.trustStore.location
    public static final String TRANSPORT_SECURITY_TRUST_STORE_LOCATION = "log4j2.trustStoreLocation";
    // TransportSecurity.trustStore.password
    public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD = "log4j2.trustStorePassword";
    // TransportSecurity.trustStore.passwordFile
    public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_FILE = "log4j2.trustStorePasswordFile";
    // TransportSecurity.trustStore.passwordEnvironmentVariable
    public static final String TRANSPORT_SECURITY_TRUST_STORE_PASSWORD_ENV_VAR = "log4j2.trustStorePasswordEnvironmentVariable";
    // TransportSecurity.trustStore.keyStoreType
    public static final String TRANSPORT_SECURITY_TRUST_STORE_KEY_STORE_TYPE = "log4j2.trustStoreKeyStoreType";
    // TransportSecurity.trustStore.keyManagerFactoryAlgorithm
    public static final String TRANSPORT_SECURITY_TRUST_STORE_KEY_MANAGER_FACTORY_ALGORITHM = "log4j2.trustStoreKeyManagerFactoryAlgorithm";
    // TransportSecurity.keyStore.location
    public static final String TRANSPORT_SECURITY_KEY_STORE_LOCATION = "log4j2.keyStoreLocation";
    // TransportSecurity.keyStore.password
    public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD = "log4j2.keyStorePassword";
    // TransportSecurity.keyStore.passwordFile
    public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD_FILE = "log4j2.keyStorePasswordFile";
    // TransportSecurity.keyStore.passwordEnvironmentVariable
    public static final String TRANSPORT_SECURITY_KEY_STORE_PASSWORD_ENV_VAR = "log4j2.keyStorePasswordEnvironmentVariable";
    // TransportSecurity.keyStore.keyStoreType
    public static final String TRANSPORT_SECURITY_KEY_STORE_TYPE = "log4j2.keyStoreType";
    // TransportSecurity.keyStore.keyManagerFactoryAlgorithm
    public static final String TRANSPORT_SECURITY_KEY_STORE_KEY_MANAGER_FACTORY_ALGORITHM = "log4j2.keyStoreKeyManagerFactoryAlgorithm";
    // TransportSecurity.verifyHostName
    public static final String TRANSPORT_SECURITY_VERIFY_HOST_NAME = "log4j2.sslVerifyHostName";

    /**
     * Property that may be used to seed the UUID generation with an integer value.
     *
     * @see org.apache.logging.log4j.core.util.UuidUtil
     */
    // UUID.sequence
    public static final String UUID_SEQUENCE = "log4j2.uuidSequence";
}
