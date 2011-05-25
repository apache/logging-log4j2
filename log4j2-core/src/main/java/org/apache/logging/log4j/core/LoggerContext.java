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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.internal.StatusLogger;
import org.apache.logging.log4j.spi.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The LoggerContext is the anchor for the logging system. It maintains a list of all the loggers requested by
 * applications and a reference to the Configuration. The Configuration will contain the configured loggers, appenders,
 * filters, etc and will be atomically updated whenever a reconfigure occurs.
 */
public class LoggerContext implements org.apache.logging.log4j.spi.LoggerContext {

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    /**
     * The Configuration is volatile to guarantee that initialization of the Configuration has completed before
     * the reference is updated.
     */
    private volatile Configuration config;

    private static StatusLogger logger = StatusLogger.getLogger();

    private static final LoggerFactory<LoggerContext> FACTORY = new Factory();

    private Object externalContext = null;

    private static final long JVM_START_TIME = System.currentTimeMillis();

    public LoggerContext() {
        reconfigure();
    }

    public static long getStartTime() {
        return JVM_START_TIME;
    }

    public void setExternalContext(Object context) {
        this.externalContext = context;
    }

    public Object getExternalContext() {
        return this.externalContext;
    }

    public Logger getLogger(String name) {
        return getLogger(FACTORY, name);
    }

    public Logger getLogger(LoggerFactory factory, String name) {
        Logger logger = loggers.get(name);
        if (logger != null) {
            return logger;
        }

        logger = (Logger) factory.newInstance(this, name);
        Logger prev = loggers.putIfAbsent(name, logger);
        return prev == null ? logger : prev;
    }

    public boolean hasLogger(String name) {
        return loggers.containsKey(name);
    }

    public Configuration getConfiguration() {
        return config;
    }

    public void addFilter(Filter filter) {
        config.addFilter(filter);
    }

    public void removeFiler(Filter filter) {
        config.removeFilter(filter);
    }

    /**
     * Set the Configuration to be used.
     */
    public synchronized Configuration setConfiguration(Configuration config) {
        if (config == null) {
            throw new NullPointerException("No Configuration was provided");
        }
        Configuration prev = this.config;
        config.start();
        this.config = config;
        updateLoggers();
        if (prev != null) {
            prev.stop();
        }
        return prev;
    }

    /**
     *  Reconfigure the context.
     */
    public synchronized void reconfigure() {
        logger.debug("Reconfiguration started");
        Configuration instance = ConfigurationFactory.getInstance().getConfiguration();
        setConfiguration(instance);
        /*instance.start();
        Configuration old = setConfiguration(instance);
        updateLoggers();
        if (old != null) {
            old.stop();
        } */
        logger.debug("Reconfiguration completed");
    }

    public void updateLoggers() {
        for (Logger logger : loggers.values()) {
            logger.updateConfiguration(config);
        }
    }

    private static class Factory implements LoggerFactory<LoggerContext> {

        public Logger newInstance(LoggerContext ctx, String name) {
            return new Logger(ctx, name);
        }
    }
}
