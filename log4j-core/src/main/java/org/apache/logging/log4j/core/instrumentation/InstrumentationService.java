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
package org.apache.logging.log4j.core.instrumentation;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.instrumentation.internal.CompositeInstrumentationService;
import org.apache.logging.log4j.core.jmx.RingBufferAdminMBean;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * Service class to influence the way Log4j components are created
 * <p>
 *     Integrators that wish to instrument Log4j Core to provide metrics and other services, should implement this
 *     class and register it with {@link java.util.ServiceLoader}.
 * </p>
 * @since 2.24.0
 */
public interface InstrumentationService {

    /**
     * Indicates that the component is used by an {@link org.apache.logging.log4j.core.appender.AsyncAppender}.
     */
    String ASYNC_APPENDER = "ASYNC_APPENDER";
    /**
     * Indicates that the component is used by an {@link org.apache.logging.log4j.core.async.AsyncLogger}.
     */
    String ASYNC_LOGGER = "ASYNC_LOGGER";
    /**
     * Indicates that the component is used by an {@link org.apache.logging.log4j.core.async.AsyncLoggerConfig}.
     */
    String ASYNC_LOGGER_CONFIG = "ASYNC_LOGGER_CONFIG";

    /**
     * @return The default instance of {@code InstrumentationService}
     */
    static InstrumentationService getInstance() {
        return CompositeInstrumentationService.getInstance();
    }

    /**
     * Allows to instrument the creation of {@link org.apache.logging.log4j.message.Message}s by the logger.
     * <p>
     *     This callback is called each time a new {@link org.apache.logging.log4j.core.Logger} is created by Log4j
     *     Core.
     * </p>
     * @param loggerName The name of the logger to be created.
     * @param messageFactory The message factory provided by the user or the default one.
     * @return The actual message factory to use.
     */
    default MessageFactory instrumentMessageFactory(final String loggerName, final MessageFactory messageFactory) {
        return messageFactory;
    }

    /**
     * Allows to instrument the delivery process of log events during a reconfiguration.
     * <p>
     *     This callback is called each time a new {@link LoggerConfig} is created by Log4j Core.
     * </p>
     * @param loggerName The name of the logger configuration to be created.
     * @param strategy The reliability strategy configured by the user.
     * @return The actual reliability strategy to use.
     */
    default ReliabilityStrategy instrumentReliabilityStrategy(
            final String loggerName, final ReliabilityStrategy strategy) {
        return strategy;
    }

    /**
     * Allows to instrument the creation of {@link org.apache.logging.log4j.core.LogEvent}s.
     * <p>
     *     This callback is called each time a new {@link LoggerConfig} is created by Log4j Core.
     * </p>
     * @param loggerName The name of the logger configuration to be created.
     * @param logEventFactory The log event factory configured by the user.
     * @return The actual log event factory to use.
     */
    default LogEventFactory instrumentLogEventFactory(final String loggerName, final LogEventFactory logEventFactory) {
        return logEventFactory;
    }

    /**
     * Allows to instrument the handling of queue full events.
     * <p>
     *     This event is called, when as new queue full policy is created.
     * </p>
     * @param parentType The type of the component that will use the queue full policy.
     *                   <p>
     *                   Currently the following constants are supported:
     *                   </p>
     *                   <ol>
     *                       <li>{@link #ASYNC_APPENDER},</li>
     *                       <li>{@link #ASYNC_LOGGER},</li>
     *                       <li>{@link #ASYNC_LOGGER_CONFIG}.</li>
     *                   </ol>
     * @param parentName The name of
     * @param queueFullPolicy The reliability strategy configured by the user.
     * @return The actual policy to use.
     */
    default AsyncQueueFullPolicy instrumentQueueFullPolicy(
            final String parentType, final String parentName, final AsyncQueueFullPolicy queueFullPolicy) {
        return queueFullPolicy;
    }

    /**
     * Allows to instrument the ring buffer of a disruptor.
     * <p>
     *     Whenever a new {@link com.lmax.disruptor.RingBuffer} is created, this method is called.
     * </p>
     * @param ringBufferAdmin An object that gives access to the characteristics of a ring buffer,
     */
    default void instrumentRingBuffer(final RingBufferAdminMBean ringBufferAdmin) {}

    /**
     * Allows to instrument a {@link LoggerConfig}.
     * <p>
     *
     * </p>
     * @param loggerConfig The logger configuration to instrument.
     * @return The same logger configuration or a wrapper.
     */
    default LoggerConfig instrumentLoggerConfig(final LoggerConfig loggerConfig) {
        return loggerConfig;
    }

    /**
     * Allows to instrument an {@link Appender}.
     * @param appender The appender to instrument.
     * @return The same appender or a wrapper.
     */
    default Appender instrumentAppender(final Appender appender) {
        return appender;
    }
}
