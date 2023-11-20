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
 *
 */
public class Logger extends Category {

    /**
     * The fully qualified name of the Logger class.
     */
    private static final String FQCN = Logger.class.getName();

    public static Logger getLogger(final Class<?> clazz) {
        // Depth 2 gets the call site of this method.
        return LogManager.getLogger(clazz.getName(), StackLocatorUtil.getCallerClassLoader(2));
    }

    public static Logger getLogger(final String name) {
        // Depth 2 gets the call site of this method.
        return LogManager.getLogger(name, StackLocatorUtil.getCallerClassLoader(2));
    }

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
