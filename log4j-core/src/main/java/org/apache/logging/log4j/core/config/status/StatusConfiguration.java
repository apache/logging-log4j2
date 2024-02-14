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
package org.apache.logging.log4j.core.config.status;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Configuration for setting up the {@link StatusLogger} fallback listener.
 */
public class StatusConfiguration {

    private static final Logger LOGGER = StatusLogger.getLogger();
    static final Level DEFAULT_LEVEL = Level.toLevel(
            PropertiesUtil.getProperties().getStringProperty(StatusLogger.DEFAULT_STATUS_LISTENER_LEVEL), Level.ERROR);

    // Guard all accesses to listener and activeConfigurations
    private static final Lock lock = new ReentrantLock();
    // Package-protected for testing
    static StatusConsoleListener listener = new StatusConsoleListener(DEFAULT_LEVEL);
    private static Map<Configuration, Void> activeConfigurations = new WeakHashMap<>();

    private volatile boolean initialized;

    @Nullable
    private PrintStream output;

    @Nullable
    private Level level;

    @Nullable
    private Configuration configuration;

    /**
     * Specifies how verbose the StatusLogger should be.
     * @deprecated This class is not used anymore and only kept for binary backward compatibility.
     */
    @Deprecated
    public enum Verbosity {
        QUIET,
        VERBOSE;

        /**
         * Parses the verbosity property into an enum.
         *
         * @param value property value to parse.
         * @return enum corresponding to value, or QUIET by default.
         * @deprecated This class is not used anymore and only kept for binary backward compatibility.
         */
        @Deprecated
        public static Verbosity toVerbosity(final String value) {
            return Boolean.parseBoolean(value) ? VERBOSE : QUIET;
        }
    }

    /**
     * Logs an error message to the {@link StatusLogger}.
     *
     * @param message error message to log
     * @deprecated Use {@link StatusLogger#getLogger()} and then {@link StatusLogger#error(String)} instead.
     */
    @Deprecated
    public void error(final String message) {
        LOGGER.error(message);
    }

    /**
     * Sets the output of the {@link StatusLogger} fallback listener.
     * <p>
     * Accepted values are as follows:
     * </p>
     * <ul>
     * <li>{@code out} (i.e., {@link System#out})</li>
     * <li>{@code err} (i.e., {@link System#err})</li>
     * <li>a URI (e.g., {@code file:///path/to/log4j-status-logs.txt})</li>
     * </ul>
     * <p>
     * Invalid values will be ignored.
     * </p>
     *
     * @param destination destination where {@link StatusLogger} messages should be output
     * @return {@code this}
     */
    public StatusConfiguration withDestination(@Nullable final String destination) {
        try {
            this.output = parseStreamName(destination);
        } catch (final URISyntaxException error) {
            LOGGER.error("Could not parse provided URI: {}", destination, error);
        } catch (final FileNotFoundException error) {
            LOGGER.error("File could not be found: {}", destination, error);
        }
        return this;
    }

    @Nullable
    private static PrintStream parseStreamName(@Nullable final String name)
            throws URISyntaxException, FileNotFoundException {
        if (name != null) {
            if (name.equalsIgnoreCase("out")) {
                return System.out;
            } else if (name.equalsIgnoreCase("err")) {
                return System.err;
            } else {
                final URI destUri = NetUtils.toURI(name);
                final File output = FileUtils.fileFromUri(destUri);
                if (output != null) {
                    final FileOutputStream fos = new FileOutputStream(output);
                    return new PrintStream(fos, true);
                }
            }
        }
        return null;
    }

    /**
     * Sets the level of the {@link StatusLogger} fallback listener.
     *
     * @param level a level name
     * @return {@code this}
     */
    public StatusConfiguration withStatus(@Nullable final String level) {
        this.level = Level.toLevel(level, null);
        if (this.level == null) {
            LOGGER.error("Invalid status level: {}", level);
        }
        return this;
    }

    /**
     * Sets the level of the {@link StatusLogger} fallback listener.
     *
     * @param level a level
     * @return {@code this}
     */
    public StatusConfiguration withStatus(@Nullable final Level level) {
        this.level = level;
        return this;
    }

    /**
     * Specifies the verbosity level to log at. This only applies to classes configured by
     * {@link #withVerboseClasses(String...) verboseClasses}.
     *
     * @param verbosity basic filter for status logger messages.
     * @return {@code this}
     * @deprecated This method is ineffective and only kept for binary backward compatibility.
     */
    @Deprecated
    public StatusConfiguration withVerbosity(final String verbosity) {
        return this;
    }

    /**
     * Specifies which class names to filter if the configured verbosity level is QUIET.
     *
     * @param verboseClasses names of classes to filter if not using VERBOSE.
     * @return {@code this}
     * @deprecated This method is ineffective and only kept for binary backward compatibility.
     */
    @Deprecated
    public StatusConfiguration withVerboseClasses(final String... verboseClasses) {
        return this;
    }

    /**
     * Sets the logger context associated with this configuration.
     * @param configuration A logger context.
     * @return this
     */
    public StatusConfiguration withConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Configures and initializes the StatusLogger using the configured options in this instance.
     */
    public void initialize() {
        lock.lock();
        try {
            if (!initialized) {
                final boolean registerListener = activeConfigurations.isEmpty();
                if (configuration != null) {
                    activeConfigurations.put(configuration, null);
                } else {
                    LOGGER.error("The required `configuration` parameter is missing.");
                    return;
                }
                if (output != null) {
                    listener.setStream(output);
                }
                if (level != null) {
                    listener.setLevel(level);
                }
                if (registerListener) {
                    StatusLogger.getLogger().registerListener(listener);
                }
                initialized = true;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops the associated status logger.
     */
    public void stop() {
        lock.lock();
        try {
            if (configuration != null) {
                activeConfigurations.remove(configuration, null);
            } else {
                LOGGER.error("The required `configuration` parameter is missing.");
                return;
            }
            if (activeConfigurations.isEmpty()) {
                StatusLogger.getLogger().removeListener(listener);
            }
        } finally {
            lock.unlock();
        }
    }
}
