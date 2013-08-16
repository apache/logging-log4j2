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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * StatusListener that writes to the Console.
 */
public class StatusConsoleListener implements StatusListener {

    private static final String STATUS_LEVEL = "org.apache.logging.log4j.StatusLevel";

    private Level level = Level.FATAL;

    private String[] filters = null;

    private final PrintStream stream;

    /**
     * Creates the StatusConsoleListener using either the level configured by the
     * "org.apache.logging.log4j.StatusLevel" system property if it is set or to a
     * default value of FATAL.
     */
    public StatusConsoleListener() {
        final String str = PropertiesUtil.getProperties().getStringProperty(STATUS_LEVEL);
        if (str != null) {
            level = Level.toLevel(str, Level.FATAL);
        }
        stream = System.out;
    }

    /**
     * Creates the StatusConsoleListener using the supplied Level.
     * @param level The Level of status messages that should appear on the console.
     */
    public StatusConsoleListener(final Level level) {
        this.level = level;
        stream = System.out;
    }

    /**
     * Creates the StatusConsoleListener using the supplied Level.
     * @param level The Level of status messages that should appear on the console.
     * @param stream The PrintStream to write to.
     */
    public StatusConsoleListener(final Level level, final PrintStream stream) {
        this.level = level;
        this.stream = stream;
    }

    /**
     * Sets the level to a new value.
     * @param level The new Level.
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    /**
     * Return the Log Level for which the Listener should receive events.
     * @return the Log Level.
     */
    @Override
    public Level getStatusLevel() {
        return this.level;
    }

    /**
     * Writes status messages to the console.
     * @param data The StatusData.
     */
    @Override
    public void log(final StatusData data) {
        if (!filtered(data)) {
            stream.println(data.getFormattedStatus());
        }
    }

    /**
     * Adds package name filters to exclude.
     * @param filters An array of package names to exclude.
     */
    public void setFilters(final String... filters) {
        this.filters = filters;
    }

    private boolean filtered(final StatusData data) {
        if (filters == null) {
            return false;
        }
        final String caller = data.getStackTraceElement().getClassName();
        for (final String filter : filters) {
            if (caller.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }

}
