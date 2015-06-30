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
package liquibase.ext.logging.log4j2;

import liquibase.logging.core.AbstractLogger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs Liquibase messages to Log4j 2.x.
 * <p>
 * This class must be in the {@code liquibase} package in order for the Liquibase plugin discovery mechanism to work.
 * </p>
 */
public class Log4j2Logger extends AbstractLogger {

    private static final String FQCN = Log4j2Logger.class.getName();

    private ExtendedLogger logger;

    @Override
    public void debug(final String message) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, buildMessage(message));
    }

    @Override
    public void debug(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.DEBUG, null, buildMessage(message), e);
    }

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public void info(final String message) {
        logger.logIfEnabled(FQCN, Level.INFO, null, buildMessage(message));
    }

    @Override
    public void info(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.INFO, null, buildMessage(message), e);
    }

    @Override
    public void setLogLevel(final String logLevel, final String logFile) {
        setLogLevel(logLevel);
        // ignore logFile
    }

    @Override
    public void setName(final String name) {
        logger = LogManager.getContext(false).getLogger(name);
    }

    @Override
    public void severe(final String message) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, buildMessage(message));
    }

    @Override
    public void severe(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.ERROR, null, buildMessage(message), e);
    }

    @Override
    public void warning(final String message) {
        logger.logIfEnabled(FQCN, Level.WARN, null, buildMessage(message));
    }

    @Override
    public void warning(final String message, final Throwable e) {
        logger.logIfEnabled(FQCN, Level.WARN, null, buildMessage(message), e);
    }

}
