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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.PropsUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class SimpleLoggerContext implements LoggerContext {

    /** The default format to use when formating dates */
    protected static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS zzz";

    /** All system properties used by <code>SimpleLog</code> start with this */
    protected static final String SYSTEM_PREFIX = "org.apache.logging.log4j.simplelog.";

    /** Properties loaded from simplelog.properties */
    private Properties simpleLogProps = new Properties();

    private PropsUtil props;

    /** Include the instance name in the log message? */
    private final boolean showLogName;
    /** Include the short name ( last component ) of the logger in the log
     *  message. Defaults to true - otherwise we'll be lost in a flood of
     *  messages without knowing who sends them.
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

    public SimpleLoggerContext() {
        props = new PropsUtil("log4j2.simplelog.properties");

        showContextMap = props.getBooleanProperty(SYSTEM_PREFIX + "showContextMap", false);
        showLogName = props.getBooleanProperty(SYSTEM_PREFIX + "showlogname", false);
        showShortName = props.getBooleanProperty(SYSTEM_PREFIX + "showShortLogname", true);
        showDateTime = props.getBooleanProperty(SYSTEM_PREFIX + "showdatetime", false);
        String lvl = props.getStringProperty(SYSTEM_PREFIX + "level");
        defaultLevel = Level.toLevel(lvl, Level.ERROR);

        dateTimeFormat = showDateTime ? props.getStringProperty(SimpleLoggerContext.SYSTEM_PREFIX + "dateTimeFormat",
            DEFAULT_DATE_TIME_FORMAT) : null;

        String fileName = props.getStringProperty(SYSTEM_PREFIX + "logFile", "system.err");
        PrintStream ps;
        if ("system.err".equalsIgnoreCase(fileName)) {
            ps = System.err;
        } else if ("system.out".equalsIgnoreCase(fileName)) {
            ps = System.out;
        } else {
            try {
                FileOutputStream os = new FileOutputStream(fileName);
                ps = new PrintStream(os);
            } catch (FileNotFoundException fnfe) {
                ps = System.err;
            }
        }
        this.stream = ps;
    }

    private ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<String, Logger>();

    public Logger getLogger(String name) {
        if (loggers.containsKey(name)) {
            return loggers.get(name);
        }

        loggers.putIfAbsent(name, new SimpleLogger(name, defaultLevel, showLogName, showShortName, showDateTime,
            showContextMap, dateTimeFormat, props, stream));
        return loggers.get(name);
    }

    public boolean hasLogger(String name) {
        return false;
    }

    public Object getExternalContext() {
        return null;
    }
}
