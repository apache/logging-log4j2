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
package org.apache.logging.log4j.internal;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class StatusData {

    private final long timestamp;

    private final StackTraceElement caller;

    private final Level level;

    private final Message msg;

    private final Throwable throwable;


    public StatusData(StackTraceElement caller, Level level, Message msg, Throwable t) {
        this.timestamp = System.currentTimeMillis();
        this.caller = caller;
        this.level = level;
        this.msg = msg;
        this.throwable = t;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public StackTraceElement getStackTraceElement() {
        return caller;
    }

    public Level getLevel() {
        return level;
    }

    public Message getMessage() {
        return msg;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getFormattedStatus() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        sb.append(format.format(new Date(timestamp)));       
        sb.append(" ");
        sb.append(level.toString());
        sb.append(" ");
        sb.append(msg.getFormattedMessage());
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
        return sb.toString();
    }
}
