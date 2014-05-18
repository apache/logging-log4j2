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

import org.apache.logging.log4j.LogManager;

/**
 * Base class for server classes that listen to {@link LogEvent}s.
 */
public class LogEventListener {

    private final LoggerContext context;

    protected LogEventListener() {
        context = (LoggerContext) LogManager.getContext(false);
    }

    public void log(final LogEvent event) {
        if (event == null) {
            return;
        }
        final Logger logger = context.getLogger(event.getLoggerName());
        if (logger.config.filter(event.getLevel(), event.getMarker(), event.getMessage(), event.getThrown())) {
            logger.config.logEvent(event);
        }
    }
}
