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
package org.apache.logging.log4j.status;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import static org.apache.logging.log4j.util.Chars.*;

/**
 * The Status data.
 */
public class StatusData implements Serializable {

    private static final long serialVersionUID = -4341916115118014017L;

    private final long timestamp;
    private final StackTraceElement caller;
    private final Level level;
    private final Message msg;
    private String threadName;
    private final Throwable throwable;

    /**
     * Creates the StatusData object.
     * 
     * @param caller The method that created the event.
     * @param level The logging level.
     * @param msg The message String.
     * @param t The Error or Exception that occurred.
     * @param threadName The thread name
     */
    public StatusData(final StackTraceElement caller, final Level level, final Message msg, final Throwable t,
            final String threadName) {
        this.timestamp = System.currentTimeMillis();
        this.caller = caller;
        this.level = level;
        this.msg = msg;
        this.throwable = t;
        this.threadName = threadName;
    }

    /**
     * Returns the event's timestamp.
     * 
     * @return The event's timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the StackTraceElement for the method that created the event.
     * 
     * @return The StackTraceElement.
     */
    public StackTraceElement getStackTraceElement() {
        return caller;
    }

    /**
     * Returns the logging level for the event.
     * 
     * @return The logging level.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the message associated with the event.
     * 
     * @return The message associated with the event.
     */
    public Message getMessage() {
        return msg;
    }

    public String getThreadName() {
        if (threadName == null) {
            threadName = Thread.currentThread().getName();
        }
        return threadName;
    }

    /**
     * Returns the Throwable associated with the event.
     * 
     * @return The Throwable associated with the event.
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Formats the StatusData for viewing.
     * 
     * @return The formatted status data as a String.
     */
    public String getFormattedStatus() {
        final StringBuilder sb = new StringBuilder();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        sb.append(format.format(new Date(timestamp)));
        sb.append(SPACE);
        sb.append(getThreadName());
        sb.append(SPACE);
        sb.append(level.toString());
        sb.append(SPACE);
        sb.append(msg.getFormattedMessage());
        final Object[] params = msg.getParameters();
        Throwable t;
        if (throwable == null && params != null && params[params.length - 1] instanceof Throwable) {
            t = (Throwable) params[params.length - 1];
        } else {
            t = throwable;
        }
        if (t != null) {
            sb.append(SPACE);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            sb.append(baos.toString());
        }
        return sb.toString();
    }
}
