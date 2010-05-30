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
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.XMLConfiguration;
import org.apache.logging.log4j.internal.StatusLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class LoggerContext implements org.apache.logging.log4j.spi.LoggerContext {

    private final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    private volatile Configuration config;

    private static StatusLogger logger = StatusLogger.getLogger();

    private static final LoggerFactory FACTORY = new Factory();

    public LoggerContext() {
        reconfigure();
    }

    public Logger getLogger(String name) {
        return getLogger(FACTORY, name);
    }

    public Logger getLogger(LoggerFactory factory, String name) {
        Logger logger = loggers.get(name);
        if (logger != null) {
            return logger;
        }

        logger = factory.newInstance(this, name);
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
     * @doubt no check for null, could cause NPE if reconfigure is called. (RG) I started to fix
     * this and realized the proper fix was to check for null and if null throw a LoggingException. Is
     * that really better than an NPE?
     */
    public synchronized Configuration setConfiguration(Configuration config) {
        Configuration prev = this.config;
        this.config = config;
        return prev;
    }

    public synchronized void reconfigure() {
        logger.debug("Reconfiguration started");
        Configuration config = ConfigurationFactory.getInstance().getConfiguration();
        config.start();
        Configuration old = setConfiguration(config);
        updateLoggers();
        if (old != null) {
            old.stop();
        }
        logger.debug("Reconfiguration completed");
    }

    public void updateLoggers() {
        for (Logger logger : loggers.values()) {
            logger.updateConfiguration(config);
        }
    }

    private static class Factory implements LoggerFactory {

        public Logger newInstance(LoggerContext ctx, String name) {
            return new Logger(ctx, name);
        }
    }
}
