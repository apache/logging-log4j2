/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.camel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

/**
 * Logger wrapper for use with Camel.
 */
public class LoggerWrapper {
    private Logger logger = LogManager.getLogger();
    private Marker marker;
    private Level level = Level.INFO;

    public boolean shouldLog() {
        return logger.isEnabled(level, marker);
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public void setLoggerName(final String name) {
        this.logger = LogManager.getLogger(name);
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(final Marker marker) {
        this.marker = marker;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(final Level level) {
        this.level = level;
    }

    public void log(final String message) {
        logger.log(level, marker, message);
    }

    public void log(final String message, final Throwable exception) {
        logger.log(level, marker, message, exception);
    }

}
