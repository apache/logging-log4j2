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
package org.apache.log4j;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Log4j 1.x logger type; extends {@link Category} and delegates to Log4j 2.
 *
 * @apiNote Bridges {@code org.apache.log4j.Logger} to {@link org.apache.logging.log4j.Logger}.
 * Extends {@link Category} and delegates logging calls to
 * {@link org.apache.logging.log4j.spi.ExtendedLogger} obtained from the active
 * {@link org.apache.logging.log4j.spi.LoggerContext}.
 * Behavioral differences:
 * <ul>
 *   <li>{@link #getLogger(String, LoggerFactory)} is not supported; use Log4j 2 extension mechanisms instead.</li>
 *   <li>{@link Category#getEffectiveLevel()} maps to {@code Logger.getLevel()} rather than computing an inherited
 *       effective level in all cases.</li>
 * </ul>
 * @see org.apache.logging.log4j.Logger
 */
public class Logger extends Category {

    /**
     * The fully qualified name of the Logger class.
     */
    private static final String FQCN = Logger.class.getName();

    public static Logger getLogger(@SuppressWarnings("rawtypes") final Class clazz) {
        // Depth 2 gets the call site of this method.
        return LogManager.getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2));
    }

    public static Logger getLogger(final String name) {
        // Depth 2 gets the call site of this method.
        return LogManager.getLogger(name, StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * @apiNote Not supported for Log4j 2 extension; retained for Log4j 1 API compatibility only. Prefer
     * {@link org.apache.logging.log4j.LogManager#getLogger(String)} for new code.
     */
    public static Logger getLogger(final String name, final LoggerFactory factory) {
        // Depth 2 gets the call site of this method.
        return LogManager.getLogger(name, factory, StackLocatorUtil.getCallerClassLoader(2));
    }

    public static Logger getRootLogger() {
        return LogManager.getRootLogger();
    }

    Logger(final LoggerContext context, final String name) {
        super(context, name);
    }

    protected Logger(final String name) {
        super(name);
    }

    public boolean isTraceEnabled() {
        return getLogger().isTraceEnabled();
    }

    public void trace(final Object message) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.TRACE, message, null);
    }

    public void trace(final Object message, final Throwable t) {
        maybeLog(FQCN, org.apache.logging.log4j.Level.TRACE, message, t);
    }
}
