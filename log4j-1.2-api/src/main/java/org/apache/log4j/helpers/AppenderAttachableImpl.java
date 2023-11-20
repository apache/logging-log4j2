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
package org.apache.log4j.helpers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Appender;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Allows Classes to attach Appenders.
 */
public class AppenderAttachableImpl implements AppenderAttachable {

    private final ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<>();

    /** Array of appenders. TODO */
    protected Vector appenderList;

    @Override
    public void addAppender(final Appender appender) {
        if (appender != null) {
            // NullAppender name is null.
            appenders.put(Objects.toString(appender.getName()), appender);
        }
    }

    /**
     * Calls the <code>doAppend</code> method on all attached appenders.
     *
     * @param event The event to log.
     * @return The number of appenders.
     */
    public int appendLoopOnAppenders(final LoggingEvent event) {
        for (final Appender appender : appenders.values()) {
            appender.doAppend(event);
        }
        return appenders.size();
    }

    /**
     * Closes all appenders.
     */
    public void close() {
        for (final Appender appender : appenders.values()) {
            appender.close();
        }
    }

    @Override
    public Enumeration<Appender> getAllAppenders() {
        return Collections.enumeration(appenders.values());
    }

    @Override
    public Appender getAppender(final String name) {
        // No null keys allowed in a CHM.
        return name == null ? null : appenders.get(name);
    }

    @Override
    public boolean isAttached(final Appender appender) {
        return appender != null ? appenders.containsValue(appender) : false;
    }

    @Override
    public void removeAllAppenders() {
        appenders.clear();
    }

    @Override
    public void removeAppender(final Appender appender) {
        if (appender != null) {
            final String name = appender.getName();
            if (name != null) {
                appenders.remove(name, appender);
            }
        }
    }

    @Override
    public void removeAppender(final String name) {
        if (name != null) {
            appenders.remove(name);
        }
    }
}
