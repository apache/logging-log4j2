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
package org.apache.logging.log4j.jdk;

import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Logger;

import org.apache.logging.log4j.spi.LoggerAdapter;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Log4j implementation of {@link java.util.logging.LogManager}. Note that the system property
 * {@code java.util.logging.manager} must be set to {@code org.apache.logging.log4j.jdk.LogManager} in order to use
 * this adaptor. This LogManager requires the {@code log4j-api} library to be available. If {@code log4j-core} is
 * also available, then more features of {@link java.util.logging.Logger} are supported.
 *
 * @since 2.1
 */
public class LogManager extends java.util.logging.LogManager {

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();
    private static final LoggerAdapter<Logger> ADAPTER;

    static {
        // find out if log4j-core is available
        String registryClassName;
        try {
            LoaderUtil.loadClass("org.apache.logging.log4j.core.Logger");
            registryClassName = "org.apache.logging.log4j.jdk.CoreLoggerRegistry";
        } catch (final ClassNotFoundException ignored) {
            registryClassName = "org.apache.logging.log4j.jdk.ApiLoggerRegistry";
        }
        LOGGER.debug("Attempting to use {}", registryClassName);
        try {
            ADAPTER = LoaderUtil.newCheckedInstanceOf(registryClassName, AbstractLoggerAdapter.class);
        } catch (final Exception e) {
            throw LOGGER.throwing(new ExceptionInInitializerError(e));
        }
    }

    public LogManager() {
        super();
        LOGGER.info("Registered Log4j as the java.util.logging.LogManager.");
    }

    @Override
    public boolean addLogger(final Logger logger) {
        // in order to prevent non-bridged loggers from being registered, we always return false to indicate that
        // the named logger should be obtained through getLogger(name)
        return false;
    }

    @Override
    public Logger getLogger(final String name) {
        LOGGER.trace("Call to LogManager.getLogger({})", name);
        return ADAPTER.getLogger(name);
    }

    @Override
    public Enumeration<String> getLoggerNames() {
        return Collections.enumeration(ADAPTER.getLoggersInContext(ADAPTER.getContext()).keySet());
    }

}
