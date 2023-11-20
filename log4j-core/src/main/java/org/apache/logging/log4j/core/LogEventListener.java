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
package org.apache.logging.log4j.core;

import java.util.EventListener;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Base class for server classes that listen to {@link LogEvent}s.
 * TODO (MS) How is this class any different from Appender?
 */
public class LogEventListener implements EventListener {

    protected static final StatusLogger LOGGER = StatusLogger.getLogger();
    private final LoggerContext context;

    protected LogEventListener() {
        context = LoggerContext.getContext(false);
    }

    public void log(final LogEvent event) {
        if (event == null) {
            return;
        }
        final Logger logger = context.getLogger(event.getLoggerName());
        if (logger.privateConfig.filter(event.getLevel(), event.getMarker(), event.getMessage(), event.getThrown())) {
            logger.privateConfig.logEvent(event);
        }
    }
}
