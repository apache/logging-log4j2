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
package org.apache.logging.log4j.status;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.logging.log4j.Level;

/**
 * A {@link StatusListener} that writes to the console.
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class StatusConsoleListener implements StatusListener {

    private volatile Level level;

    private volatile PrintStream stream;

    /**
     * Constructs a {@link StatusConsoleListener} instance writing to {@link System#out} using the supplied level.
     *
     * @param level the level of status messages that should appear on the console
     * @throws NullPointerException on null {@code level}
     */
    public StatusConsoleListener(final Level level) {
        this(level, System.out);
    }

    /**
     * Constructs a {@link StatusConsoleListener} instance using the supplied level and stream.
     * <p>
     * Make sure not to use a logger stream of some sort to avoid creating an infinite loop of indirection!
     * </p>
     *
     * @param level the level of status messages that should appear on the console
     * @param stream the stream to write to
     * @throws NullPointerException on null {@code level} or {@code stream}
     */
    public StatusConsoleListener(final Level level, final PrintStream stream) {
        this.level = requireNonNull(level, "level");
        this.stream = requireNonNull(stream, "stream");
    }

    /**
     * Sets the level to a new value.
     *
     * @param level the new level
     * @throws NullPointerException on null {@code level}
     */
    public void setLevel(final Level level) {
        this.level = requireNonNull(level, "level");
    }

    /**
     * Sets the output stream to a new value.
     *
     * @param stream the new output stream
     * @throws NullPointerException on null {@code stream}
     * @since 2.23.0
     */
    public void setStream(final PrintStream stream) {
        this.stream = requireNonNull(stream, "stream");
    }

    /**
     * Returns the level for which the listener should receive events.
     *
     * @return the log level
     */
    @Override
    public Level getStatusLevel() {
        return level;
    }

    /**
     * Writes status messages to the console.
     *
     * @param data a status data
     * @throws NullPointerException on null {@code data}
     */
    @Override
    public void log(final StatusData data) {
        requireNonNull(data, "data");
        if (level.isLessSpecificThan(data.getLevel())) {
            final String formattedStatus = data.getFormattedStatus();
            stream.println(formattedStatus);
        }
    }

    /**
     * Adds package name filters to exclude.
     *
     * @param filters An array of package names to exclude.
     * @deprecated This method is ineffective and only kept for binary backward compatibility.
     */
    @Deprecated
    public void setFilters(final String... filters) {}

    @Override
    public void close() throws IOException {
        // Get local copy of the `volatile` member
        final OutputStream localStream = stream;
        // Close only non-system streams
        if (localStream != System.out && localStream != System.err) {
            localStream.close();
        }
    }
}
