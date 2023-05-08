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
package org.apache.logging.log4j.core.test.junit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class Log4jEventRecorder implements AutoCloseable {

    private static final String LOGGER_CONTEXT_NAME_PREFIX = Log4jEventRecorder.class.getSimpleName() + "-LoggerContext-";

    private static final AtomicInteger LOGGER_CONTEXT_COUNTER = new AtomicInteger(0);

    private final InternalLog4jAppender appender;

    private final LoggerContext loggerContext;

    private static final class InternalLog4jAppender extends AbstractAppender {

        private static final PatternLayout LAYOUT = PatternLayout.createDefaultLayout();

        private final List<LogEvent> events;

        private InternalLog4jAppender() {
            super("ListAppender", null, LAYOUT, false, null);
            this.events = Collections.synchronizedList(new ArrayList<>());
            start();
        }

        @Override
        public void append(final LogEvent event) {
            final LogEvent copySafeEvent = event instanceof MutableLogEvent
                    ? ((MutableLogEvent) event).createMemento()
                    : event;
            events.add(copySafeEvent);
        }

    }

    Log4jEventRecorder() {
        this.appender = new InternalLog4jAppender();
        this.loggerContext = new LoggerContext(LOGGER_CONTEXT_NAME_PREFIX + LOGGER_CONTEXT_COUNTER.getAndIncrement());
        final LoggerConfig rootConfig = loggerContext.getConfiguration().getRootLogger();
        rootConfig.setLevel(Level.ALL);
        rootConfig.getAppenders().values().forEach(appender -> rootConfig.removeAppender(appender.getName()));
        rootConfig.addAppender(appender, Level.ALL, null);
    }

    public org.apache.logging.log4j.spi.LoggerContext getLoggerContext() {
        return loggerContext;
    }

    public List<LogEvent> getEvents() {
        return appender.events;
    }

    @Override
    public void close() {
        loggerContext.close();
    }

}
