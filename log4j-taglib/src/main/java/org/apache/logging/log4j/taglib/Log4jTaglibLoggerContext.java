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
package org.apache.logging.log4j.taglib;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This bridge between the tag library and the Log4j API ensures that instances of {@link Log4jTaglibLogger} are
 * appropriately held in memory and not constantly recreated.
 *
 * @since 2.0
 */
@NullMarked
final class Log4jTaglibLoggerContext implements LoggerContext {

    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    private static final Lock READ_LOCK = LOCK.readLock();

    private static final Lock WRITE_LOCK = LOCK.writeLock();

    private static final Map<ServletContext, Log4jTaglibLoggerContext> LOGGER_CONTEXT_BY_SERVLET_CONTEXT =
            new WeakHashMap<>();

    private static final MessageFactory DEFAULT_MESSAGE_FACTORY = ParameterizedMessageFactory.INSTANCE;

    private final LoggerRegistry<Log4jTaglibLogger> loggerRegistry = new LoggerRegistry<>();

    private final ServletContext servletContext;

    private Log4jTaglibLoggerContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public Object getExternalContext() {
        return servletContext;
    }

    @Override
    public Log4jTaglibLogger getLogger(final String name) {
        return getLogger(name, DEFAULT_MESSAGE_FACTORY);
    }

    @Override
    public Log4jTaglibLogger getLogger(final String name, @Nullable final MessageFactory messageFactory) {
        final MessageFactory effectiveMessageFactory =
                messageFactory != null ? messageFactory : DEFAULT_MESSAGE_FACTORY;
        final Log4jTaglibLogger oldLogger = loggerRegistry.getLogger(name, effectiveMessageFactory);
        if (oldLogger != null) {
            return oldLogger;
        }
        final Log4jTaglibLogger newLogger = createLogger(name, effectiveMessageFactory);
        loggerRegistry.putIfAbsent(name, effectiveMessageFactory, newLogger);
        return loggerRegistry.getLogger(name, effectiveMessageFactory);
    }

    private Log4jTaglibLogger createLogger(final String name, final MessageFactory messageFactory) {
        final LoggerContext loggerContext = LogManager.getContext(false);
        final ExtendedLogger delegateLogger = loggerContext.getLogger(name, messageFactory);
        return new Log4jTaglibLogger(delegateLogger, name, delegateLogger.getMessageFactory());
    }

    @Override
    public boolean hasLogger(final String name) {
        return loggerRegistry.hasLogger(name, DEFAULT_MESSAGE_FACTORY);
    }

    @Override
    public boolean hasLogger(final String name, @Nullable final MessageFactory messageFactory) {
        final MessageFactory effectiveMessageFactory =
                messageFactory != null ? messageFactory : DEFAULT_MESSAGE_FACTORY;
        return loggerRegistry.hasLogger(name, effectiveMessageFactory);
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return loggerRegistry.hasLogger(name, messageFactoryClass);
    }

    static Log4jTaglibLoggerContext getInstance(final ServletContext servletContext) {

        // Get the associated logger context, if exists
        READ_LOCK.lock();
        try {
            final Log4jTaglibLoggerContext loggerContext = LOGGER_CONTEXT_BY_SERVLET_CONTEXT.get(servletContext);
            if (loggerContext != null) {
                return loggerContext;
            }
        } finally {
            READ_LOCK.unlock();
        }

        // Create the logger context
        WRITE_LOCK.lock();
        try {
            return LOGGER_CONTEXT_BY_SERVLET_CONTEXT.computeIfAbsent(servletContext, Log4jTaglibLoggerContext::new);
        } finally {
            WRITE_LOCK.unlock();
        }
    }
}
