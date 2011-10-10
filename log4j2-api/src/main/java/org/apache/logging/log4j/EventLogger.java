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
public class EventLogger {

    private static final String FQCN = EventLogger.class.getName();

    private static AbstractLoggerWrapper logger;

    static {
        Logger l = LogManager.getLogger("EventLogger");
        if (!(l instanceof AbstractLogger)) {
            throw new LoggingException("Logger returned must be based on AbstractLogger");
        }
        logger = new AbstractLoggerWrapper((AbstractLogger) l, "EventLogger");
    }

    public static final Marker marker = MarkerManager.getMarker("EVENT");

    /**
     * Log events with a level of ALL.
     * @param msg The event StructuredDataMessage.
     */
    public static void logEvent(StructuredDataMessage msg) {
        logger.log(marker, FQCN, Level.ALL, msg, null);
    }

    /**
     * Log events and specify the logging level.
     * @param msg The event StructuredDataMessage.
     * @param level The logging Level.
     */
    public static void logEvent(StructuredDataMessage msg, Level level) {
        logger.log(marker, FQCN, level, msg, null);
    }
}
