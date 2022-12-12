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
package org.apache.logging.log4j.simple;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.LoggingSystemProperties;
import org.apache.logging.log4j.util.PropertyResolver;

import static org.apache.logging.log4j.simple.SimpleLoggerContext.DEFAULT_DATE_TIME_FORMAT;

public class SimpleLoggerConfiguration {

    protected final PropertyResolver propertyResolver;

    public SimpleLoggerConfiguration(final PropertyResolver resolver) {
        propertyResolver = resolver;
    }

    /** Include the ThreadContextMap in the log message */
    public boolean isContextMapShown() {
        return propertyResolver.getBoolean(LoggingSystemProperties.SIMPLE_SHOW_CONTEXT_MAP, false);
    }

    /** Include the instance name in the log message? */
    public boolean isLogNameShown() {
        return propertyResolver.getBoolean(LoggingSystemProperties.SIMPLE_SHOW_LOG_NAME, false);
    }

    /**
     * Include the short name (last component) of the logger in the log message. Defaults to true - otherwise we'll be
     * lost in a flood of messages without knowing who sends them.
     */
    public boolean isShortNameShown() {
        return propertyResolver.getBoolean(LoggingSystemProperties.SIMPLE_SHOW_SHORT_LOG_NAME, true);
    }

    /** Include the current time in the log message */
    public boolean isDateTimeShown() {
        return propertyResolver.getBoolean(LoggingSystemProperties.SIMPLE_SHOW_DATE_TIME, false);
    }

    public Level getDefaultLevel() {
        return propertyResolver.getString(LoggingSystemProperties.SIMPLE_LOG_LEVEL)
                .map(Level::getLevel)
                .orElse(Level.ERROR);
    }

    public Level getLoggerLevel(final String loggerName) {
        return propertyResolver.getString(String.format(LoggingSystemProperties.SIMPLE_LOGGER_LOG_LEVEL, loggerName))
                .map(Level::getLevel)
                .orElseGet(this::getDefaultLevel);
    }

    public DateFormat getDateTimeFormat() {
        return propertyResolver.getString(LoggingSystemProperties.SIMPLE_DATE_TIME_FORMAT)
                .map(format -> {
                    try {
                        return new SimpleDateFormat(format);
                    } catch (final IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .orElseGet(() -> new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT));
    }

    public String getLogFileName() {
        return propertyResolver.getString(LoggingSystemProperties.SIMPLE_LOG_FILE).orElse("system.err");
    }
}
