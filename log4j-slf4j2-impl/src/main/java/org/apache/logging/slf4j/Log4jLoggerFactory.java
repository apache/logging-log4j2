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
package org.apache.logging.slf4j;

import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Log4j implementation of SLF4J ILoggerFactory interface.
 */
public class Log4jLoggerFactory extends AbstractLoggerAdapter<Logger> implements ILoggerFactory {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final String SLF4J_PACKAGE = "org.slf4j";
    private static final Predicate<Class<?>> CALLER_PREDICATE = clazz ->
            !AbstractLoggerAdapter.class.equals(clazz) && !clazz.getName().startsWith(SLF4J_PACKAGE);
    private static final String TO_SLF4J_CONTEXT = "org.apache.logging.slf4j.SLF4JLoggerContext";

    private final Log4jMarkerFactory markerFactory;

    public Log4jLoggerFactory(final Log4jMarkerFactory markerFactory) {
        this.markerFactory = markerFactory;
    }

    @Override
    protected Logger newLogger(final String name, final LoggerContext context) {
        final String key = Logger.ROOT_LOGGER_NAME.equals(name) ? LogManager.ROOT_LOGGER_NAME : name;
        return new Log4jLogger(markerFactory, validateContext(context).getLogger(key), name);
    }

    @Override
    protected LoggerContext getContext() {
        final Class<?> anchor = LogManager.getFactory().isClassLoaderDependent()
                ? StackLocatorUtil.getCallerClass(Log4jLoggerFactory.class, CALLER_PREDICATE)
                : null;
        LOGGER.trace("Log4jLoggerFactory.getContext() found anchor {}", anchor);
        return anchor == null ? LogManager.getContext(false) : getContext(anchor);
    }

    Log4jMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    private LoggerContext validateContext(final LoggerContext context) {
        if (TO_SLF4J_CONTEXT.equals(context.getClass().getName())) {
            throw new LoggingException("log4j-slf4j-impl cannot be present with log4j-to-slf4j");
        }
        return context;
    }
}
