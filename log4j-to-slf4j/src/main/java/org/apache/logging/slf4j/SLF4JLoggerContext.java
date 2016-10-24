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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SLF4JLoggerContext implements LoggerContext {
    private final LoggerRegistry<ExtendedLogger> loggerRegistry = new LoggerRegistry<>();

    @Override
    public Object getExternalContext() {
        return null;
    }

    @Override
    public ExtendedLogger getLogger(final String name) {
        if (!loggerRegistry.hasLogger(name)) {
            loggerRegistry.putIfAbsent(name, null, new SLF4JLogger(name, LoggerFactory.getLogger(name)));
        }
        return loggerRegistry.getLogger(name);
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        // FIXME according to LOG4J2-1180, the below line should be:
        // FIXME if (!loggerRegistry.hasLogger(name, messageFactory)) {
        if (!loggerRegistry.hasLogger(name)) {
            // FIXME: should be loggerRegistry.putIfAbsent(name, messageFactory,
            loggerRegistry.putIfAbsent(name, null,
                    new SLF4JLogger(name, messageFactory, LoggerFactory.getLogger(name)));
        }
        // FIXME should be return loggerRegistry.getLogger(name, messageFactory);
        return loggerRegistry.getLogger(name);

        // TODO applying the above fixes causes (log4j-to-slf4j) LoggerTest to fail
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
