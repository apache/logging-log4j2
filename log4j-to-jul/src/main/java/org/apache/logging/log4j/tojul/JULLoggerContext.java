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
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;

/**
 * Implementation of Log4j {@link LoggerContext} SPI.
 * This is a factory to produce {@link JULLogger} instances.
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
class JULLoggerContext implements LoggerContext {
    private final LoggerRegistry<ExtendedLogger> loggerRegistry = new LoggerRegistry<>();

    // This implementation is strongly inspired by org.apache.logging.slf4j.SLF4JLoggerContext

    @Override
    public Object getExternalContext() {
        return null;
    }

    @Override
    public ExtendedLogger getLogger(final String name) {
        if (!loggerRegistry.hasLogger(name)) {
            loggerRegistry.putIfAbsent(name, null, new JULLogger(name, Logger.getLogger(name)));
        }
        return loggerRegistry.getLogger(name);
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        if (!loggerRegistry.hasLogger(name, messageFactory)) {
            loggerRegistry.putIfAbsent(
                    name, messageFactory, new JULLogger(name, messageFactory, Logger.getLogger(name)));
        }
        return loggerRegistry.getLogger(name, messageFactory);
    }

    @Override
    public boolean hasLogger(final String name) {
        return loggerRegistry.hasLogger(name);
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return loggerRegistry.hasLogger(name, messageFactory);
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return loggerRegistry.hasLogger(name, messageFactoryClass);
    }
}
