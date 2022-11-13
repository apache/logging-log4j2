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
package org.apache.logging.log4j.status;

import java.io.PrintStream;
import java.text.DateFormat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

class StatusLoggerFactory {
    private static final String STATUS_LOGGER = "StatusLogger";
    private final StatusLoggerConfiguration configuration;

    StatusLoggerFactory(final StatusLoggerConfiguration configuration) {
        this.configuration = configuration;
    }

    SimpleLogger createSimpleLogger(final String name, final Level loggerLevel, final PrintStream stream) {
        final DateFormat dateFormat = configuration.getDateTimeFormat();
        return new SimpleLogger(name, ParameterizedNoReferenceMessageFactory.INSTANCE, stream,
                loggerLevel, dateFormat, dateFormat != null);
    }

    StatusLogger createStatusLogger() {
        final Level loggerLevel = configuration.isDebugEnabled() ? Level.TRACE : Level.ERROR;
        final SimpleLogger logger = createSimpleLogger(STATUS_LOGGER, loggerLevel, System.err);
        return new StatusLogger(logger, configuration);
    }

    static StatusLoggerFactory getInstance() {
        return new StatusLoggerFactory(new StatusLoggerConfiguration(PropertiesUtil.getProperties(STATUS_LOGGER)));
    }
}
