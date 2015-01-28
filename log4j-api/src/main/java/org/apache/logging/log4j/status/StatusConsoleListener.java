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

import java.io.IOException;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;

/**
 * StatusListener that writes to the Console.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class StatusConsoleListener implements StatusListener {

    private Level level = Level.FATAL;
    private String[] filters;
    private final PrintStream stream;

    /**
     * Creates the StatusConsoleListener using the supplied Level.
     * @param level The Level of status messages that should appear on the console.
     */
    public StatusConsoleListener(final Level level) {
        this(level, System.out);
    }

    /**
     * Creates the StatusConsoleListener using the supplied Level. Make sure not to use a logger stream of some sort
     * to avoid creating an infinite loop of indirection!
     * @param level The Level of status messages that should appear on the console.
     * @param stream The PrintStream to write to.
     * @throws IllegalArgumentException if the PrintStream argument is {@code null}.
     */
    public StatusConsoleListener(final Level level, final PrintStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("You must provide a stream to use for this listener.");
        }
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

    @Override
    public void close() throws IOException {
        // only want to close non-system streams
        if (this.stream != System.out && this.stream != System.err) {
            this.stream.close();
        }
    }
}
