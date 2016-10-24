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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 *
 */
public class SimpleLoggerContext implements LoggerContext {

    private static final String SYSTEM_OUT = "system.out";

    private static final String SYSTEM_ERR = "system.err";

    /** The default format to use when formatting dates */
    protected static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS zzz";

    /** All system properties used by <code>SimpleLog</code> start with this */
    protected static final String SYSTEM_PREFIX = "org.apache.logging.log4j.simplelog.";

    private final PropertiesUtil props;

    /** Include the instance name in the log message? */
    private final boolean showLogName;

    /**
     * Include the short name (last component) of the logger in the log message. Defaults to true - otherwise we'll be
     * lost in a flood of messages without knowing who sends them.
     */
    private final boolean showShortName;
    /** Include the current time in the log message */
    private final boolean showDateTime;
    /** Include the ThreadContextMap in the log message */
    private final boolean showContextMap;
    /** The date and time format to use in the log message */
    private final String dateTimeFormat;

    private final Level defaultLevel;

    private final PrintStream stream;

    private final LoggerRegistry<ExtendedLogger> loggerRegistry = new LoggerRegistry<>();

    public SimpleLoggerContext() {
        props = new PropertiesUtil("log4j2.simplelog.properties");

        showContextMap = props.getBooleanProperty(SYSTEM_PREFIX + "showContextMap", false);
        showLogName = props.getBooleanProperty(SYSTEM_PREFIX + "showlogname", false);
        showShortName = props.getBooleanProperty(SYSTEM_PREFIX + "showShortLogname", true);
        showDateTime = props.getBooleanProperty(SYSTEM_PREFIX + "showdatetime", false);
        final String lvl = props.getStringProperty(SYSTEM_PREFIX + "level");
        defaultLevel = Level.toLevel(lvl, Level.ERROR);

        dateTimeFormat = showDateTime ? props.getStringProperty(SimpleLoggerContext.SYSTEM_PREFIX + "dateTimeFormat",
                DEFAULT_DATE_TIME_FORMAT) : null;

        final String fileName = props.getStringProperty(SYSTEM_PREFIX + "logFile", SYSTEM_ERR);
        PrintStream ps;
        if (SYSTEM_ERR.equalsIgnoreCase(fileName)) {
            ps = System.err;
        } else if (SYSTEM_OUT.equalsIgnoreCase(fileName)) {
            ps = System.out;
        } else {
            try {
                final FileOutputStream os = new FileOutputStream(fileName);
                ps = new PrintStream(os);
            } catch (final FileNotFoundException fnfe) {
                ps = System.err;
            }
        }
        this.stream = ps;
    }

    @Override
    public ExtendedLogger getLogger(final String name) {
        return getLogger(name, null);
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        // Note: This is the only method where we add entries to the 'loggerRegistry' ivar.
        final ExtendedLogger extendedLogger = loggerRegistry.getLogger(name, messageFactory);
        if (extendedLogger != null) {
            AbstractLogger.checkMessageFactory(extendedLogger, messageFactory);
            return extendedLogger;
        }
        final SimpleLogger simpleLogger = new SimpleLogger(name, defaultLevel, showLogName, showShortName, showDateTime,
                showContextMap, dateTimeFormat, messageFactory, props, stream);
        loggerRegistry.putIfAbsent(name, messageFactory, simpleLogger);
        return loggerRegistry.getLogger(name, messageFactory);
    }

    @Override
    public boolean hasLogger(final String name) {
        return false;
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return false;
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return false;
    }

    @Override
    public Object getExternalContext() {
        return null;
    }

}
