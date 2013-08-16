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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.AbstractLoggerWrapper;

/**
 *  Logs "Events" that are represented as StructuredDataMessages.
 */
public final class EventLogger {

    private static final String NAME = "EventLogger";

    /**
     * Define the Event Marker.
     */
    public static final Marker EVENT_MARKER = MarkerManager.getMarker("EVENT");

    private static final String FQCN = EventLogger.class.getName();

    private static AbstractLoggerWrapper loggerWrapper;

    static {
        final Logger eventLogger = LogManager.getLogger(NAME);
        if (!(eventLogger instanceof AbstractLogger)) {
            throw new LoggingException("Logger returned must be based on AbstractLogger");
        }
        loggerWrapper = new AbstractLoggerWrapper((AbstractLogger) eventLogger, NAME, null);
    }


    private EventLogger() {
    }

    /**
     * Log events with a level of ALL.
     * @param msg The event StructuredDataMessage.
     */
    public static void logEvent(final StructuredDataMessage msg) {
        loggerWrapper.log(EVENT_MARKER, FQCN, Level.OFF, msg, null);
    }

    /**
     * Log events and specify the logging level.
     * @param msg The event StructuredDataMessage.
     * @param level The logging Level.
     */
    public static void logEvent(final StructuredDataMessage msg, final Level level) {
        loggerWrapper.log(EVENT_MARKER, FQCN, level, msg, null);
    }
}
