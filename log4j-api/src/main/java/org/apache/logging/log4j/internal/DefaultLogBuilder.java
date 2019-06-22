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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Supplier;


/**
 * Collects data for a log event and then logs it.
 */
public class DefaultLogBuilder implements LogBuilder {
    private static Message EMPTY_MESSAGE = new SimpleMessage("");
    private static final String FQCN = DefaultLogBuilder.class.getName();

    private final Logger logger;
    private Level level;
    private Marker marker;
    private Throwable throwable;
    private StackTraceElement location;
    private Object object;
    private Message msg;
    private String textMessage;
    private Supplier<Message> supplier;
    private Object[] parameters;

    public DefaultLogBuilder(Logger logger) {
        this.logger = logger;
    }

    public LogBuilder setLevel(Level level) {
        this.level = level;
        this.marker = null;
        this.throwable = null;
        this.location = null;
        this.object = null;
        this.msg = null;
        this.textMessage = null;
        this.supplier = null;
        this.parameters = null;
        return this;
    }

    public LogBuilder withMarker(Marker marker) {
        this.marker = marker;
        return this;
    }

    public LogBuilder withThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    public LogBuilder withLocation() {
        location = StackLocatorUtil.getStackTraceElement(2);
        return this;
    }

    public LogBuilder withLocation(StackTraceElement location) {
        this.location = location;
        return this;
    }

    public LogBuilder withMessage(String msg) {
        this.textMessage = msg;
        return this;
    }

    public LogBuilder withMessage(Object msg) {
        this.object = msg;
        return this;
    }

    public LogBuilder withMessage(Message msg) {
        this.msg = msg;
        return this;
    }

    public LogBuilder withMessage(Supplier<Message> supplier) {
        this.supplier = supplier;
        return this;
    }

    public LogBuilder withParameters(Object... params) {
        if (params != null && params.length > 0) {
            if (parameters == null) {
                parameters = params;
            } else {
                Object[] prev = parameters;
                int count = parameters.length + params.length;
                parameters = new Object[count];
                System.arraycopy(prev, 0, parameters, 0, prev.length);
                System.arraycopy(params, 0, parameters, prev.length, params.length);
            }
        }
        return this;
    }

    public void log() {
        Message message;
        if (msg != null) {
            message = msg;
        } else if (supplier != null) {
            message = supplier.get();
        } else if (object != null) {
            message = logger.getMessageFactory().newMessage(object);
        } else if (textMessage != null) {
            message = logger.getMessageFactory().newMessage(textMessage, parameters);
        } else {
            message = EMPTY_MESSAGE;
        }
        logger.logMessage(level, marker, FQCN, location, message, throwable);
    }
}
