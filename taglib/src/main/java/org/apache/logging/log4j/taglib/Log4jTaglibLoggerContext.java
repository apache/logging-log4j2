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
package org.apache.logging.log4j.taglib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContext;

import javax.servlet.ServletContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This bridge between the tag library and the Log4j API ensures that instances of {@link Log4jTaglibLogger} are
 * appropriately held in memory and not constantly recreated.
 *
 * @since 2.0
 */
final class Log4jTaglibLoggerContext implements LoggerContext {
    private static final ConcurrentMap<ServletContext, Log4jTaglibLoggerContext> CONTEXTS =
            new ConcurrentHashMap<ServletContext, Log4jTaglibLoggerContext>();

    private final ConcurrentMap<String, Log4jTaglibLogger> loggers =
            new ConcurrentHashMap<String, Log4jTaglibLogger>();

    private final ServletContext servletContext;

    private Log4jTaglibLoggerContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Object getExternalContext() {
        return this.servletContext;
    }

    public Log4jTaglibLogger getLogger(String name) {
        return this.getLogger(name, null);
    }

    public Log4jTaglibLogger getLogger(String name, MessageFactory factory) {
        Log4jTaglibLogger logger = this.loggers.get(name);
        if (logger != null) {
            AbstractLogger.checkMessageFactory(logger, factory);
            return logger;
        }

        // wrap a logger from an underlying implementation
        Logger original = factory == null ? LogManager.getLogger(name) : LogManager.getLogger(name, factory);
        if (original instanceof AbstractLogger) {
            logger = new Log4jTaglibLogger((AbstractLogger) original, name, original.getMessageFactory());
            Log4jTaglibLogger existing = this.loggers.putIfAbsent(name, logger);
            return existing == null ? logger : existing;
        }

        throw new LoggingException("Log4j Tag Library requires base logging system to extend Log4j AbstractLogger.");
    }

    public boolean hasLogger(String name) {
        return this.loggers.containsKey(name);
    }

    static synchronized Log4jTaglibLoggerContext getInstance(ServletContext servletContext) {
        if (CONTEXTS.containsKey(servletContext)) {
            return CONTEXTS.get(servletContext);
        }

        Log4jTaglibLoggerContext context = new Log4jTaglibLoggerContext(servletContext);
        Log4jTaglibLoggerContext existing = CONTEXTS.putIfAbsent(servletContext, context);
        return existing == null ? context : existing;
    }
}
