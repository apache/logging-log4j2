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
package org.apache.logging.log4j.jcl;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 *
 */
public class LogFactoryImpl extends LogFactory {

    private final Map<LoggerContext, ConcurrentMap<String, Log>> contextMap =
        new WeakHashMap<LoggerContext, ConcurrentMap<String, Log>>();

    private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    @Override
    public Log getInstance(final String name) throws LogConfigurationException {
        final ConcurrentMap<String, Log> loggers = getLoggersMap();
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }
        final org.apache.logging.log4j.Logger logger = PrivateManager.getLogger(name);
        if (logger instanceof AbstractLogger) {
            loggers.putIfAbsent(name, new Log4jLog((AbstractLogger) logger, name));
            return loggers.get(name);
        }
        throw new LogConfigurationException(
            "Commons Logging Adapter requires base logging system to extend Log4j AbstractLogger");
    }

    private ConcurrentMap<String, Log> getLoggersMap() {
        final LoggerContext context = PrivateManager.getContext();
        synchronized (contextMap) {
            ConcurrentMap<String, Log> map = contextMap.get(context);
            if (map == null) {
                map = new ConcurrentHashMap<String, Log>();
                contextMap.put(context, map);
            }
            return map;
        }
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Override
    public Log getInstance(@SuppressWarnings("rawtypes") final Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    /**
     * This method is supposed to clear all loggers. In this implementation it will clear all the logger
     * wrappers but the loggers managed by the underlying logger context will not be.
     */
    @Override
    public void release() {
        getLoggersMap().clear();
    }

    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        if (value != null) {
            attributes.put(name, value);
        } else {
            removeAttribute(name);
        }
    }

    /**
     * The real bridge between commons logging and Log4j.
     */
    private static class PrivateManager extends LogManager {
        private static final String FQCN = LogFactory.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static org.apache.logging.log4j.Logger getLogger(final String name) {
            return getLogger(FQCN, name);
        }
    }

}
