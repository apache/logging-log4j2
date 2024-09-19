/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * {@link ErrorHandler} implementation buffering invocations in the form of {@link StatusData}.
 */
final class BufferingErrorHandler implements ErrorHandler {

    private final List<StatusData> statusDataBuffer = Collections.synchronizedList(new ArrayList<>());

    BufferingErrorHandler() {}

    private void addStatusData(final String message, final Throwable throwable) {
        final StackTraceElement caller = StackLocatorUtil.getStackTraceElement(3);
        final String threadName = Thread.currentThread().getName();
        final StatusData statusData =
                new StatusData(caller, Level.ERROR, new SimpleMessage(message), throwable, threadName);
        statusDataBuffer.add(statusData);
    }

    @Override
    public void error(String message) {
        addStatusData(message, null);
    }

    @Override
    public void error(final String message, final Throwable throwable) {
        addStatusData(message, throwable);
    }

    @Override
    public void error(final String message, final LogEvent event, final Throwable throwable) {
        addStatusData(message, throwable);
    }

    public void clear() {
        statusDataBuffer.clear();
    }

    public List<StatusData> getBuffer() {
        return statusDataBuffer;
    }
}
