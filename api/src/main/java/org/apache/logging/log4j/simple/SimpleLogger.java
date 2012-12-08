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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.PropsUtil;

/**
 *  This is the default logger that is used when no suitable logging implementation is available.
 *
 */
public class SimpleLogger extends AbstractLogger {

    /**
     * Used to format times.
     * <p>
     * Note that DateFormat is not Thread-safe.
     */
    private DateFormat dateFormatter = null;

    private Level level;

    private boolean showDateTime;

    private boolean showContextMap;

    private PrintStream stream;

    private String logName;


    public SimpleLogger(String name, Level defaultLevel, boolean showLogName, boolean showShortLogName,
                        boolean showDateTime, boolean showContextMap, String dateTimeFormat,
                        MessageFactory messageFactory, PropsUtil props, PrintStream stream) {
        super(name, messageFactory);
        String lvl = props.getStringProperty(SimpleLoggerContext.SYSTEM_PREFIX + name + ".level");
        this.level = Level.toLevel(lvl, defaultLevel);
        if (showShortLogName) {
            int index = name.lastIndexOf(".");
            if (index > 0 && index < name.length()) {
                this.logName = name.substring(index + 1);
            } else {
                this.logName = name;
            }
        } else if (showLogName) {
            this.logName = name;
        }
        this.showDateTime = showDateTime;
        this.showContextMap = showContextMap;
        this.stream = stream;

        if (showDateTime) {
            try {
                this.dateFormatter = new SimpleDateFormat(dateTimeFormat);
            } catch(IllegalArgumentException e) {
                // If the format pattern is invalid - use the default format
                this.dateFormatter = new SimpleDateFormat(SimpleLoggerContext.DEFAULT_DATE_TIME_FORMAT);
            }
        }
    }

    public void setStream(PrintStream stream) {
        this.stream = stream;
    }

    public void setLevel(Level level) {
        if (level != null) {
            this.level = level;
        }
    }

    @Override
    public void log(Marker marker, String fqcn, Level level, Message msg, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        // Append date-time if so configured
        if(showDateTime) {
            Date now = new Date();
            String dateText;
            synchronized(dateFormatter) {
                dateText = dateFormatter.format(now);
            }
            sb.append(dateText);
            sb.append(" ");
        }

        sb.append(level.toString());
        sb.append(" ");
        if (logName != null && logName.length() > 0) {
            sb.append(logName);
            sb.append(" ");
        }
        sb.append(msg.getFormattedMessage());
        if (showContextMap) {
            Map<String, String> mdc = ThreadContext.getContext();
            if (mdc.size() > 0) {
                sb.append(" ");
                sb.append(mdc.toString());
                sb.append(" ");
            }
        }
        Object[] params = msg.getParameters();
        Throwable t;
        if (throwable == null && params != null && params[params.length -1] instanceof Throwable ) {
            t = (Throwable) params[params.length - 1];
        } else {
            t = throwable;
        }
        if (t != null) {
            sb.append(" ");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            sb.append(baos.toString());
        }
        stream.println(sb.toString());
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg) {
        return this.level.intLevel() >= level.intLevel();
    }


    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Throwable t) {
        return this.level.intLevel() >= level.intLevel();
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object... p1) {
        return this.level.intLevel() >= level.intLevel();
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Object msg, Throwable t) {
        return this.level.intLevel() >= level.intLevel();
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Message msg, Throwable t) {
        return this.level.intLevel() >= level.intLevel();
    }

}
