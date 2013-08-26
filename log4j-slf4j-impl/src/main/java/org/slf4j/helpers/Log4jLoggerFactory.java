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
package org.slf4j.helpers;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.slf4j.SLF4JLoggingException;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SLF4JLogger;

/**
 *
 */
public class Log4jLoggerFactory implements ILoggerFactory {

    private static final String FQCN = Log4jLoggerFactory.class.getName();
    private static final String PACKAGE = "org.slf4j";

    private final Map<LoggerContext, ConcurrentMap<String, Logger>> contextMap =
        new WeakHashMap<LoggerContext, ConcurrentMap<String, Logger>>();

    @Override
    public Logger getLogger(final String name) {
        final LoggerContext context = getContext();
        final ConcurrentMap<String, Logger> loggers = getLoggersMap(context);

        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        final String key = Logger.ROOT_LOGGER_NAME.equals(name) ? LogManager.ROOT_LOGGER_NAME : name;
        final org.apache.logging.log4j.Logger logger = context.getLogger(key);
        if (logger instanceof AbstractLogger) {
            loggers.putIfAbsent(name, new SLF4JLogger((AbstractLogger) logger, name));
            return loggers.get(name);
        }
        throw new SLF4JLoggingException("SLF4J Adapter requires base logging system to extend Log4j AbstractLogger");
    }

    private ConcurrentMap<String, Logger> getLoggersMap(final LoggerContext context) {
        synchronized (contextMap) {
            ConcurrentMap<String, Logger> map = contextMap.get(context);
            if (map == null) {
                map = new ConcurrentHashMap<String, Logger>();
                contextMap.put(context, map);
            }
            return map;
        }
    }
    private LoggerContext getContext() {
        final Throwable t = new Throwable();
        boolean next = false;
        boolean pkg = false;
        String fqcn = LoggerFactory.class.getName();
        for (final StackTraceElement element : t.getStackTrace()) {
            if (FQCN.equals(element.getClassName())) {
                next = true;
                continue;
            }
            if (next && element.getClassName().startsWith(PACKAGE)) {
                fqcn = element.getClassName();
                pkg = true;
                continue;
            }
            if (pkg) {
                break;
            }
        }
        return PrivateManager.getContext(fqcn);
    }

    /**
     * The real bridge between SLF4J and Log4j.
     */
    private static class PrivateManager extends LogManager {
        private static final String FQCN = LoggerFactory.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static LoggerContext getContext(final String fqcn) {
            return getContext(fqcn, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(final String name) {
            return getLogger(FQCN, name);
        }
    }

}
