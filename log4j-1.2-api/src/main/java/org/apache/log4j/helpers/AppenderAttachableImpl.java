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
package org.apache.log4j.helpers;

import org.apache.log4j.Appender;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Allows Classes to attach Appenders.
 */
public class AppenderAttachableImpl implements AppenderAttachable {

    private final ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<>();

    @Override
    public void addAppender(Appender newAppender) {
        if (newAppender != null) {
            appenders.put(newAppender.getName(), newAppender);
        }
    }

    @Override
    public Enumeration getAllAppenders() {
        return Collections.enumeration(appenders.values());
    }

    @Override
    public Appender getAppender(String name) {
        return appenders.get(name);
    }

    @Override
    public boolean isAttached(Appender appender) {
        return appenders.containsValue(appender);
    }

    @Override
    public void removeAllAppenders() {
        appenders.clear();
    }

    @Override
    public void removeAppender(Appender appender) {
        appenders.remove(appender.getName(), appender);
    }

    @Override
    public void removeAppender(String name) {
        appenders.remove(name);
    }

    /**
     * Call the <code>doAppend</code> method on all attached appenders.
     * @param event The event to log.
     * @return The number of appenders.
     */
    public int appendLoopOnAppenders(LoggingEvent event) {
        for (Appender appender : appenders.values()) {
            appender.doAppend(event);
        }
        return appenders.size();
    }

    public void close() {
        for (Appender appender : appenders.values()) {
            appender.close();
        }
    }
}
