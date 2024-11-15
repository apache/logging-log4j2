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
package org.apache.logging.log4j.tojul;

import java.util.logging.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of Log4j {@link LoggerContext} SPI.
 * This is a factory to produce {@link JULLogger} instances.
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
class JULLoggerContext implements LoggerContext {

    private final LoggerRegistry<ExtendedLogger> loggerRegistry = new LoggerRegistry<>();

    private static final MessageFactory DEFAULT_MESSAGE_FACTORY = ParameterizedMessageFactory.INSTANCE;

    // This implementation is strongly inspired by org.apache.logging.slf4j.SLF4JLoggerContext

    @Override
    public Object getExternalContext() {
        return null;
    }

    @Override
    public ExtendedLogger getLogger(final String name) {
        return getLogger(name, DEFAULT_MESSAGE_FACTORY);
    }

    @Override
    public ExtendedLogger getLogger(final String name, @Nullable final MessageFactory messageFactory) {
        final MessageFactory effectiveMessageFactory =
                messageFactory != null ? messageFactory : DEFAULT_MESSAGE_FACTORY;
        final ExtendedLogger oldLogger = loggerRegistry.getLogger(name, effectiveMessageFactory);
        if (oldLogger != null) {
            return oldLogger;
        }
        final ExtendedLogger newLogger = createLogger(name, effectiveMessageFactory);
        loggerRegistry.putIfAbsent(name, effectiveMessageFactory, newLogger);
        return loggerRegistry.getLogger(name, effectiveMessageFactory);
    }

    private static ExtendedLogger createLogger(final String name, @Nullable final MessageFactory messageFactory) {
        final Logger logger = Logger.getLogger(name);
        return new JULLogger(name, messageFactory, logger);
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
}
