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
package org.apache.logging.log4j;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 *
 */
public class TestLogger extends AbstractLogger {

    private static final long serialVersionUID = 1L;

    public TestLogger() {
        super();
    }

    public TestLogger(final String name, final MessageFactory messageFactory) {
        super(name, messageFactory);
    }

    public TestLogger(final String name) {
        super(name);
    }

    private final List<String> list = new ArrayList<>();

    public List<String> getEntries() {
        return list;
    }

    @Override
    public void logMessage(final String fqcn, final Level level, final Marker marker, final Message msg, final Throwable throwable) {
        final StringBuilder sb = new StringBuilder();
        if (marker != null) {
            sb.append(marker);
        }
        sb.append(' ');
        sb.append(level.toString());
        sb.append(' ');
        sb.append(msg.getFormattedMessage());
        final Map<String, String> mdc = ThreadContext.getImmutableContext();
        if (mdc.size() > 0) {
            sb.append(' ');
            sb.append(mdc.toString());
            sb.append(' ');
        }
        final Object[] params = msg.getParameters();
        Throwable t;
        if (throwable == null && params != null && params.length > 0 && params[params.length - 1] instanceof Throwable) {
            t = (Throwable) params[params.length - 1];
        } else {
            t = throwable;
        }
        if (t != null) {
            sb.append(' ');
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            sb.append(baos.toString());
        }
        list.add(sb.toString());
        //System.out.println(sb.toString());
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String msg) {
        return true;
    }


    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String msg, final Throwable t) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String msg, final Object... p1) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0,
            final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final CharSequence msg, final Throwable t) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object msg, final Throwable t) {
        return true;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message msg, final Throwable t) {
        return true;
    }

    @Override
    public Level getLevel() {
        return Level.ALL;
    }
}
