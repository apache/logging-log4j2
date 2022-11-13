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

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.InternalApi;
import org.apache.logging.log4j.util.Strings;

/**
 * This is the default logger that is used when no suitable logging implementation is available.
 */
public class SimpleLogger extends AbstractLogger {

    private static final long serialVersionUID = 1L;

    private static final char SPACE = ' ';

    /**
     * Used to format times.
     * <p>
     * Note that DateFormat is not Thread-safe.
     * </p>
     */
    private final DateFormat dateFormatter;

    private Level level;

    private final boolean showDateTime;

    private final boolean showContextMap;

    private PrintStream stream;

    private final String logName;

    public SimpleLogger(final String name, final MessageFactory messageFactory, final PrintStream stream,
                        final SimpleLoggerConfiguration configuration) {
        super(name, messageFactory);
        level = configuration.getLoggerLevel(name);
        if (configuration.isShortNameShown()) {
            final int index = name.lastIndexOf(".");
            logName = index > 0 ? name.substring(index + 1) : name;
        } else if (configuration.isLogNameShown()) {
            logName = name;
        } else {
            logName = null;
        }
        showDateTime = configuration.isDateTimeShown();
        showContextMap = configuration.isContextMapShown();
        this.stream = stream;
        dateFormatter = showDateTime ? configuration.getDateTimeFormat() : null;
    }

    // exposed for use in StatusLoggerFactory
    @InternalApi
    public SimpleLogger(final String name, final MessageFactory messageFactory, final PrintStream stream,
                        final Level level, final DateFormat dateFormatter, final boolean showDateTime) {
        super(name, messageFactory);
        this.stream = stream;
        this.level = level;
        this.dateFormatter = dateFormatter;
        this.showDateTime = showDateTime;
        this.showContextMap = false;
        final int index = name.lastIndexOf(".");
        logName = index > 0 ? name.substring(index + 1) : name;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final Message msg, final Throwable t) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final CharSequence msg, final Throwable t) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final Object msg, final Throwable t) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String msg) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String msg, final Object... p1) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public boolean isEnabled(final Level testLevel, final Marker marker, final String msg, final Throwable t) {
        return this.level.intLevel() >= testLevel.intLevel();
    }

    @Override
    public void logMessage(final String fqcn, final Level mgsLevel, final Marker marker, final Message msg,
            final Throwable throwable) {
        final StringBuilder sb = new StringBuilder();
        // Append date-time if so configured
        if (showDateTime) {
            final Date now = new Date();
            final String dateText;
            synchronized (dateFormatter) {
                dateText = dateFormatter.format(now);
            }
            sb.append(dateText);
            sb.append(SPACE);
        }

        sb.append(mgsLevel.toString());
        sb.append(SPACE);
        if (Strings.isNotEmpty(logName)) {
            sb.append(logName);
            sb.append(SPACE);
        }
        sb.append(msg.getFormattedMessage());
        if (showContextMap) {
            final Map<String, String> mdc = ThreadContext.getImmutableContext();
            if (mdc.size() > 0) {
                sb.append(SPACE);
                sb.append(mdc);
                sb.append(SPACE);
            }
        }
        final Object[] params = msg.getParameters();
        final Throwable t;
        if (throwable == null && params != null && params.length > 0
                && params[params.length - 1] instanceof Throwable) {
            t = (Throwable) params[params.length - 1];
        } else {
            t = throwable;
        }
        stream.println(sb);
        if (t != null) {
            stream.print(SPACE);
            t.printStackTrace(stream);
        }
    }

    public void setLevel(final Level level) {
        if (level != null) {
            this.level = level;
        }
    }

    public void setStream(final PrintStream stream) {
        this.stream = stream;
    }

}
