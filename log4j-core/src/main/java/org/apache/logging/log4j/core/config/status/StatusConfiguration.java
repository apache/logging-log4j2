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

package org.apache.logging.log4j.core.config.status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Configuration for setting up {@link StatusConsoleListener} instances.
 */
public class StatusConfiguration {

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static final PrintStream DEFAULT_STREAM = System.out;
    private static final Level DEFAULT_STATUS = Level.ERROR;
    private static final Verbosity DEFAULT_VERBOSITY = Verbosity.QUIET;

    private final Collection<String> errorMessages = Collections.synchronizedCollection(new LinkedList<String>());
    private final StatusLogger logger = StatusLogger.getLogger();

    private volatile boolean initialized = false;

    private PrintStream destination = DEFAULT_STREAM;
    private Level status = DEFAULT_STATUS;
    private Verbosity verbosity = DEFAULT_VERBOSITY;
    private String[] verboseClasses;

    /**
     * Specifies how verbose the StatusLogger should be.
     */
    public static enum Verbosity {
        QUIET, VERBOSE;

        /**
         * Parses the verbosity property into an enum.
         *
         * @param value property value to parse.
         * @return enum corresponding to value, or QUIET by default.
         */
        public static Verbosity toVerbosity(final String value) {
            return Boolean.parseBoolean(value) ? VERBOSE : QUIET;
        }
    }

    /**
     * Logs an error message to the StatusLogger. If the StatusLogger hasn't been set up yet, queues the message to be
     * logged after initialization.
     *
     * @param message error message to log.
     */
    public void error(final String message) {
        if (!this.initialized) {
            this.errorMessages.add(message);
        } else {
            this.logger.error(message);
        }
    }

    /**
     * Specifies the destination for StatusLogger events. This can be {@code out} (default) for using
     * {@link System#out standard out}, {@code err} for using {@link System#err standard error}, or a file URI to
     * which log events will be written. If the provided URI is invalid, then the default destination of standard
     * out will be used.
     *
     * @param destination where status log messages should be output.
     * @return {@code this}
     */
    public StatusConfiguration withDestination(final String destination) {
        try {
            this.destination = parseStreamName(destination);
        } catch (final URISyntaxException e) {
            this.error("Could not parse URI [" + destination + "]. Falling back to default of stdout.");
            this.destination = DEFAULT_STREAM;
        } catch (final FileNotFoundException e) {
            this.error("File could not be found at [" + destination + "]. Falling back to default of stdout.");
            this.destination = DEFAULT_STREAM;
        }
        return this;
    }

    private PrintStream parseStreamName(final String name) throws URISyntaxException, FileNotFoundException {
        if (name == null || name.equalsIgnoreCase("out")) {
            return DEFAULT_STREAM;
        }
        if (name.equalsIgnoreCase("err")) {
            return System.err;
        }
        final URI destination = FileUtils.getCorrectedFilePathUri(name);
        final File output = FileUtils.fileFromUri(destination);
        if (output == null) {
            // don't want any NPEs, no sir
            return DEFAULT_STREAM;
        }
        final FileOutputStream fos = new FileOutputStream(output);
        return new PrintStream(fos, true);
    }

    /**
     * Specifies the logging level by name to use for filtering StatusLogger messages.
     *
     * @param status name of logger level to filter below.
     * @return {@code this}
     * @see Level
     */
    public StatusConfiguration withStatus(final String status) {
        this.status = Level.toLevel(status, null);
        if (this.status == null) {
            this.error("Invalid status level specified: " + status + ". Defaulting to ERROR.");
            this.status = Level.ERROR;
        }
        return this;
    }

    /**
     * Specifies the logging level to use for filtering StatusLogger messages.
     *
     * @param status logger level to filter below.
     * @return {@code this}
     */
    public StatusConfiguration withStatus(final Level status) {
        this.status = status;
        return this;
    }

    /**
     * Specifies the verbosity level to log at. This only applies to classes configured by
     * {@link #withVerboseClasses(String...) verboseClasses}.
     *
     * @param verbosity basic filter for status logger messages.
     * @return {@code this}
     */
    public StatusConfiguration withVerbosity(final String verbosity) {
        this.verbosity = Verbosity.toVerbosity(verbosity);
        return this;
    }

    /**
     * Specifies which class names to filter if the configured verbosity level is QUIET.
     *
     * @param verboseClasses names of classes to filter if not using VERBOSE.
     * @return {@code this}
     */
    public StatusConfiguration withVerboseClasses(final String... verboseClasses) {
        this.verboseClasses = verboseClasses;
        return this;
    }

    /**
     * Configures and initializes the StatusLogger using the configured options in this instance.
     */
    public void initialize() {
        if (!this.initialized) {
            if (this.status == Level.OFF) {
                this.initialized = true;
            } else {
                final boolean configured = configureExistingStatusConsoleListener();
                if (!configured) {
                    registerNewStatusConsoleListener();
                }
                migrateSavedLogMessages();
            }
        }
    }

    private boolean configureExistingStatusConsoleListener() {
        boolean configured = false;
        for (final StatusListener statusListener : this.logger.getListeners()) {
            if (statusListener instanceof StatusConsoleListener) {
                final StatusConsoleListener listener = (StatusConsoleListener) statusListener;
                listener.setLevel(this.status);
                if (this.verbosity == Verbosity.QUIET) {
                    listener.setFilters(this.verboseClasses);
                }
                configured = true;
            }
        }
        return configured;
    }


    private void registerNewStatusConsoleListener() {
        final StatusConsoleListener listener = new StatusConsoleListener(this.status, this.destination);
        if (this.verbosity == Verbosity.QUIET) {
            listener.setFilters(this.verboseClasses);
        }
        this.logger.registerListener(listener);
    }

    private void migrateSavedLogMessages() {
        for (final String message : this.errorMessages) {
            this.logger.error(message);
        }
        this.initialized = true;
        this.errorMessages.clear();
    }
}