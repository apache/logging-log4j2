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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 *
 */
public class Log4jLog implements Log, Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final String FQCN = Log4jLog.class.getName();

    private final ExtendedLogger logger;

    public Log4jLog(final ExtendedLogger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isEnabled(Level.DEBUG, null, null);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabled(Level.ERROR, null, null);
    }

    @Override
    public boolean isFatalEnabled() {
        return logger.isEnabled(Level.FATAL, null, null);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isEnabled(Level.INFO, null, null);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isEnabled(Level.TRACE, null, null);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isEnabled(Level.WARN, null, null);
    }

    @Override
    public void trace(final Object message) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.TRACE, null, message, t);
    }

    @Override
    public void debug(final Object message) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, message, t);
    }

    @Override
    public void info(final Object message) {
        logger.logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.INFO, null, message, t);
    }

    @Override
    public void warn(final Object message) {
        logger.logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.WARN, null, message, t);
    }

    @Override
    public void error(final Object message) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, message, t);
    }

    @Override
    public void fatal(final Object message) {
        logger.logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final Object message, final Throwable t) {
        logger.logIfEnabled(FQCN, Level.FATAL, null, message, t);
    }
}
