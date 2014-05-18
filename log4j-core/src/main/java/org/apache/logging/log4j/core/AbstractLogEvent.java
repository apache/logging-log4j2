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
package org.apache.logging.log4j.core;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;

/**
 * An abstract log event implementation with default values for all methods. The setters are no-ops.
 */
public abstract class AbstractLogEvent implements LogEvent {

    private static final long serialVersionUID = 1L;

    @Override
    public Map<String, String> getContextMap() {
        return null;
    }

    @Override
    public String getContextMap(String key) {
        return null;
    }

    @Override
    public ContextStack getContextStack() {
        return null;
    }

    @Override
    public Level getLevel() {
        return null;
    }

    @Override
    public String getLoggerFQCN() {
        return null;
    }

    @Override
    public String getLoggerName() {
        return null;
    }

    @Override
    public Marker getMarker() {
        return null;
    }

    @Override
    public Message getMessage() {
        return null;
    }

    @Override
    public StackTraceElement getSource() {
        return null;
    }

    @Override
    public String getThreadName() {
        return null;
    }

    @Override
    public Throwable getThrown() {
        return null;
    }

    @Override
    public ThrowableProxy getThrownProxy() {
        return null;
    }

    @Override
    public long getTimeMillis() {
        return 0;
    }

    @Override
    public boolean isEndOfBatch() {
        return false;
    }

    @Override
    public boolean isIncludeLocation() {
        return false;
    }

    @Override
    public void setEndOfBatch(boolean endOfBatch) {
        // do nothing
    }

    @Override
    public void setIncludeLocation(boolean locationRequired) {
        // do nothing
    }

}
