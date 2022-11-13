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
import org.apache.logging.log4j.util.PropertyEnvironment;

import static org.apache.logging.log4j.simple.SimpleLoggerContext.DEFAULT_DATE_TIME_FORMAT;
import static org.apache.logging.log4j.simple.SimpleLoggerContext.SYSTEM_PREFIX;

public class SimpleLoggerConfiguration {

    protected final PropertyEnvironment environment;

    public SimpleLoggerConfiguration(final PropertyEnvironment environment) {
        this.environment = environment;
    }

    /** Include the ThreadContextMap in the log message */
    public boolean isContextMapShown() {
        return environment.getBooleanProperty(SYSTEM_PREFIX + "showContextMap", false);
    }

    /** Include the instance name in the log message? */
    public boolean isLogNameShown() {
        return environment.getBooleanProperty(SYSTEM_PREFIX + "showlogname", false);
    }

    /**
     * Include the short name (last component) of the logger in the log message. Defaults to true - otherwise we'll be
     * lost in a flood of messages without knowing who sends them.
     */
    public boolean isShortNameShown() {
        return environment.getBooleanProperty(SYSTEM_PREFIX + "showShortLogname", true);
    }

    /** Include the current time in the log message */
    public boolean isDateTimeShown() {
        return environment.getBooleanProperty(SYSTEM_PREFIX + "showdatetime", false);
    }

    public Level getDefaultLevel() {
        final String level = environment.getStringProperty(SYSTEM_PREFIX + "level");
        return Level.toLevel(level, Level.ERROR);
    }

    public Level getLoggerLevel(final String loggerName) {
        final String level = environment.getStringProperty(SYSTEM_PREFIX + loggerName + ".level");
        return Level.toLevel(level, getDefaultLevel());
    }

    public DateFormat getDateTimeFormat() {
        try {
            return new SimpleDateFormat(environment.getStringProperty(SYSTEM_PREFIX + "dateTimeFormat", DEFAULT_DATE_TIME_FORMAT));
        } catch (final IllegalArgumentException e) {
            return new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
        }
    }

    public String getLogFileName() {
        return environment.getStringProperty(SYSTEM_PREFIX + "logFile", "system.err");
    }
}
