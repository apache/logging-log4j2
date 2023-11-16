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
package org.apache.logging.log4j.osgi.tests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * This Configuration is the same as the DefaultConfiguration but shows how a
 * custom configuration can be built programmatically
 */
public class CustomConfiguration extends AbstractConfiguration {

    /**
     * The name of the default configuration.
     */
    public static final String CONFIG_NAME = "Custom";

    private final ListAppender appender = new ListAppender();

    public CustomConfiguration(final LoggerContext loggerContext) {
        this(loggerContext, ConfigurationSource.NULL_SOURCE);
    }

    /**
     * Constructor to create the default configuration.
     */
    public CustomConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        super(loggerContext, source);
        setName(CONFIG_NAME);
        appender.start();
        addAppender(appender);
        final LoggerConfig root = getRootLogger();
        root.addAppender(appender, null, null);
        root.setLevel(Level.ALL);
    }

    @Override
    protected void doConfigure() {}

    public List<LogEvent> getEvents() {
        return appender.getEvents();
    }

    public void clearEvents() {
        appender.getEvents().clear();
    }

    private static class ListAppender extends AbstractLifeCycle implements Appender {

        private final List<LogEvent> events = Collections.<LogEvent>synchronizedList(new ArrayList<>());

        @Override
        public void append(final LogEvent event) {
            events.add(event.toImmutable());
        }

        @Override
        public String getName() {
            return "LIST";
        }

        @Override
        public Layout<? extends Serializable> getLayout() {
            return null;
        }

        @Override
        public boolean ignoreExceptions() {
            return false;
        }

        @Override
        public ErrorHandler getHandler() {
            return null;
        }

        @Override
        public void setHandler(final ErrorHandler handler) {}

        public List<LogEvent> getEvents() {
            return events;
        }
    }
}
