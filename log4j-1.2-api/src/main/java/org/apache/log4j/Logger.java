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
package org.apache.log4j;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 *
 */
public class Logger extends Category {

    /**
     * The fully qualified name of the Logger class.
     */
    private static final String FQCN = Logger.class.getName();

    protected Logger(final String name) {
        super(name);
    }

    Logger(final LoggerContext context, final String name) {
        super(context, name);
    }

    public static Logger getLogger(final String name) {
        return LogManager.getLogger(name);
    }

    public static Logger getLogger(final Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    public static Logger getRootLogger() {
        return LogManager.getRootLogger();
    }

    public static Logger getLogger(final String name, final LoggerFactory factory) {
        return LogManager.getLogger(name, factory);
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
