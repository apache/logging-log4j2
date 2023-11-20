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
package org.apache.logging.log4j.core.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.util.StackLocatorUtil;

public class ListErrorHandler implements ErrorHandler {

    private final ArrayList<StatusData> statusData = new ArrayList<>();

    private void addStatusData(final String msg, final Throwable t) {
        synchronized (statusData) {
            final StackTraceElement caller = StackLocatorUtil.getStackTraceElement(3);
            final String threadName = Thread.currentThread().getName();
            statusData.add(new StatusData(caller, Level.ERROR, new SimpleMessage(msg), t, threadName));
        }
    }

    @Override
    public void error(String msg) {
        addStatusData(msg, null);
    }

    @Override
    public void error(String msg, Throwable t) {
        addStatusData(msg, t);
    }

    @Override
    public void error(String msg, LogEvent event, Throwable t) {
        addStatusData(msg, t);
    }

    public void clear() {
        synchronized (statusData) {
            statusData.clear();
        }
    }

    public Stream<StatusData> getStatusData() {
        synchronized (statusData) {
            return ((List<StatusData>) statusData.clone()).stream();
        }
    }

    public Stream<StatusData> findStatusData(String regex) {
        return getStatusData()
                .filter(data -> data.getMessage().getFormattedMessage().matches(regex));
    }
}
