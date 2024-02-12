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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.util.Constants;

/**
 * Records events that occur in the logging system.
 * {@link StatusLogger} is expected to be a standalone, self-sufficient component that the logging system can rely on for low-level logging purposes.
 * <h2>Listeners</h3>
 * <p>
 * Each recorded event will first get buffered and then used to notify the registered {@link StatusListener}s.
 * If none are available, the <em>fallback listener</em> of type {@link StatusConsoleListener} will be used.
 * </p>
 * <p>
 * You can programmatically register listeners using {@link #registerListener(StatusListener)} method.
 * </p>
 * <h3>Configuration</h3>
 * <p>
 * The {@code StatusLogger} can be configured in following ways:
 * </p>
 * <ol>
 * <li>Passing system properties to the Java process (e.g., {@code -Dlog4j2.StatusLogger.level=INFO})</li>
 * <li>Providing properties in a {@value StatusLogger#PROPERTIES_FILE_NAME} file in the classpath</li>
 * <li>Using Log4j configuration (i.e., {@code <Configuration status="WARN" dest="out">} in a {@code log4j2.xml} in the classpath)</li>
 * </ol>
 * <p>
 * It is crucial to understand that there is a time between the first {@code StatusLogger} access and a configuration file (e.g., {@code log4j2.xml}) read.
 * Consider the following example:
 * </p>
 * <ol>
 * <li>The default level (of fallback listener) is {@code ERROR}</li>
 * <li>You have {@code <Configuration status="WARN">} in your {@code log4j2.xml}</li>
 * <li>Until your {@code log4j2.xml} configuration is read, the effective level will be {@code ERROR}</li>
 * <li>Once your {@code log4j2.xml} configuration is read, the effective level will be {@code WARN} as you configured</li>
 * </ol>
 * <p>
 * Hence, unless you use either system properties or {@value StatusLogger#PROPERTIES_FILE_NAME} file in the classpath, there is a time window that only the defaults will be effective.
 * </p>
 * <p>
 * {@code StatusLogger} is designed as a singleton class accessed statically.
 * If you are running an application containing multiple Log4j configurations (e.g., in a servlet environment with multiple containers) and you happen to have differing {@code StatusLogger} configurations (e.g, one {@code log4j2.xml} containing {@code <Configuration status="ERROR">} while the other {@code <Configuration status="INFO">}), the last loaded configuration will be effective one.
 * </p>
 * <h2>Configuration properties</h3>
 * <p>
 * The list of available properties for configuring the {@code StatusLogger} is shared below.
 * </p>
 * <table>
 * <caption>available properties for configuring the <code>StatusLogger</code></caption>
 * <tr>
 *     <th>Name</th>
 *     <th>Default</th>
 *     <th>Description</th>
 * </tr>
 * <tr>
 *     <td><code>{@value MAX_STATUS_ENTRIES}</code></td>
 *     <td>0</td>
 *     <td>
 *         The maximum number of events buffered.
 *         Once the limit is reached, older entries will be removed as new entries are added.
 *     </td>
 * </tr>
 * <tr>
 *     <td><code>{@value DEFAULT_STATUS_LISTENER_LEVEL}</code></td>
 *     <td>{@code ERROR}</td>
 *     <td>
 *         The {@link Level} name to use as the fallback listener level.<br/>
 *         The fallback listener is used when the listener registry is empty.
 *         The fallback listener will accept entries filtered by the level provided in this configuration.
 *     </td>
 * </tr>
 * <tr>
 *     <td><code>{@value STATUS_DATE_FORMAT}</code></td>
 *     <td>{@code null}</td>
 *     <td>A {@link java.time.format.DateTimeFormatter} pattern to format the created {@link StatusData}.</td>
 * </tr>
 * <tr>
 *     <td><code>{@value #DEBUG_PROPERTY_NAME}</code></td>
 *     <td>false</td>
 *     <td>The debug mode toggle.</td>
 * </tr>
 * </table>
 * <h2>Debug mode</h3>
 * <p>
 * When the {@value Constants#LOG4J2_DEBUG} system property is present, any level-related filtering will be skipped and all events will be notified to listeners.
 * If no listeners are available, the <em>fallback listener</em> of type {@link StatusConsoleListener} will be used.
 * </p>
 */
public class StatusLogger extends AbstractLogger {

    private static final long serialVersionUID = 2L;

    /**
     * The name of the system property that enables debug mode in its presence.
     * <p>
     * This is a local clone of {@link Constants#LOG4J2_DEBUG}.
     * The cloning is necessary to avoid cyclic initialization.
     * </p>
     */
    private static final String DEBUG_PROPERTY_NAME = "log4j2.debug";

    /**
     * The name of the system property that can be configured with the maximum number of events buffered.
     * <p>
     * Once the limit is reached, older entries will be removed as new entries are added.
     * </p>
     */
    public static final String MAX_STATUS_ENTRIES = "log4j2.status.entries";

    /**
     * The name of the system property that can be configured with the {@link Level} name to use as the fallback listener level.
     * <p>
     * The fallback listener is used when the listener registry is empty.
     * The fallback listener will accept entries filtered by the level provided in this configuration.
     * </p>
     *
     * @since 2.8
     */
    public static final String DEFAULT_STATUS_LISTENER_LEVEL = "log4j2.StatusLogger.level";

    /**
     * The name of the system property that can be configured with a {@link java.time.format.DateTimeFormatter} pattern that will be used while formatting the created {@link StatusData}.
     *
     * @since 2.11.0
     */
    public static final String STATUS_DATE_FORMAT = "log4j2.StatusLogger.DateFormat";

    /**
     * The name of the file to be searched in the classpath to read properties from.
     *
     * @since 2.23.0
     */
    public static final String PROPERTIES_FILE_NAME = "log4j2.StatusLogger.properties";

    /**
     * Holder for user-provided {@link StatusLogger} configurations.
     *
     * @since 2.23.0
     */
    public static final class Config {

        private static final Config INSTANCE = new Config();

        private final boolean debugEnabled;

        private final int bufferCapacity;

        @Nullable
        private final Level fallbackListenerLevel;

        @Nullable
        private final DateTimeFormatter instantFormatter;

        /**
         * Constructs an instance using the given properties.
         * <b>Users should not create new instances, but use {@link #getInstance()} instead</b>!
         *
         * @param debugEnabled the value of the {@value DEBUG_PROPERTY_NAME} property
         * @param bufferCapacity the value of the {@value MAX_STATUS_ENTRIES} property
         * @param instantFormatter the value of the {@value STATUS_DATE_FORMAT} property
         */
        public Config(boolean debugEnabled, int bufferCapacity, @Nullable DateTimeFormatter instantFormatter) {
            this.debugEnabled = debugEnabled;
            if (bufferCapacity < 0) {
                throw new IllegalArgumentException(
                        "was expecting a positive `bufferCapacity`, found: " + bufferCapacity);
            }
            this.bufferCapacity = bufferCapacity;
            // Public ctor intentionally doesn't set `fallbackListenerLevel`.
            // Because, if public ctor is used, it means user is programmatically creating a `Config` instance.
            // Hence, they will use the public `StatusLogger` ctor too.
            // There they need to provide the fallback listener explicitly anyway.
            // Therefore, there is no need to ask for a `fallbackListenerLevel` here.
            // Since this `fallbackListenerLevel` is only used by the private `StatusLogger` ctor.
            this.fallbackListenerLevel = null;
            this.instantFormatter = instantFormatter;
        }

        /**
         * Constructs an instance using either system properties or a property file (i.e., {@value Config#PROPERTIES_FILE_NAME}) in the classpath, if available.
         */
        private Config() {
            final Properties fileProvidedProperties = readPropertiesFile();
            this.debugEnabled = readDebugEnabled(fileProvidedProperties);
            this.bufferCapacity = readBufferCapacity(fileProvidedProperties);
            this.fallbackListenerLevel = readFallbackListenerLevel(fileProvidedProperties);
            this.instantFormatter = readInstantFormatter(fileProvidedProperties);
        }

        /**
         * Gets the static instance.
         *
         * @return a singleton instance
         */
        public static Config getInstance() {
            return INSTANCE;
        }

        private static boolean readDebugEnabled(final Properties fileProvidedProperties) {
            final String debug = readProperty(fileProvidedProperties, DEBUG_PROPERTY_NAME);
            return debug != null;
        }

        private static int readBufferCapacity(final Properties fileProvidedProperties) {
            final String capacityString = readProperty(fileProvidedProperties, MAX_STATUS_ENTRIES);
            return capacityString != null ? Integer.parseInt(capacityString) : 0;
        }

        private static Level readFallbackListenerLevel(final Properties fileProvidedProperties) {
            final String level = readProperty(fileProvidedProperties, DEFAULT_STATUS_LISTENER_LEVEL);
            return level != null ? Level.valueOf(level) : Level.ERROR;
        }

        private static DateTimeFormatter readInstantFormatter(final Properties fileProvidedProperties) {
            final String format = readProperty(fileProvidedProperties, STATUS_DATE_FORMAT);
            return format != null ? DateTimeFormatter.ofPattern(format) : null;
        }

        private static String readProperty(final Properties fileProvidedProperties, final String propertyName) {
            final String systemProvidedValue = System.getProperty(propertyName);
            return systemProvidedValue != null
                    ? systemProvidedValue
                    : (String) fileProvidedProperties.get(propertyName);
        }

        // We need to roll out our own `.properties` reader.
        // We could have used `PropertiesUtil`, `PropertyFilePropertySource`, etc.
        // Consequently, they would delegate to `LoaderUtil`, etc.
        // All these mechanisms expect a working `StatusLogger`.
        // Hence, in order to be self-sufficient, we cannot rely on them.
        private static Properties readPropertiesFile() {
            final Properties properties = new Properties();
            final URL url = StatusLogger.class.getResource(PROPERTIES_FILE_NAME);
            if (url == null) {
                return properties;
            }
            try (final InputStream stream = url.openStream()) {
                properties.load(stream);
            } catch (final IOException error) {
                // There is no logging system at this stage.
                // There is nothing we can do but simply dumping the failure.
                error.printStackTrace(System.err);
            }
            return properties;
        }
    }

    /**
     * Wrapper for the default instance for lazy initialization.
     * <p>
     * The initialization will be performed when the JVM initializes the class.
     * Since {@code InstanceHolder} has no other fields or methods, class initialization occurs when the {@code INSTANCE} field is first referenced.
     * </p>
     *
     * @see <a href="https://www.infoworld.com/article/2074979/double-checked-locking--clever--but-broken.html?page=2">Double-checked locking: Clever, but broken</a>
     */
    private static final class InstanceHolder {

        private static volatile StatusLogger INSTANCE = new StatusLogger();
    }

    private final Config config;

    private final StatusConsoleListener fallbackListener;

    private final List<StatusListener> listeners;

    private final transient ReadWriteLock listenerLock = new ReentrantReadWriteLock();

    private final transient Lock listenerReadLock = listenerLock.readLock();

    private final transient Lock listenerWriteLock = listenerLock.writeLock();

    private final Queue<StatusData> buffer = new ConcurrentLinkedQueue<>();

    /**
     * Constructs the default instance.
     */
    private StatusLogger() {
        this(
                StatusLogger.class.getSimpleName(),
                ParameterizedNoReferenceMessageFactory.INSTANCE,
                Config.getInstance(),
                new StatusConsoleListener(Config.getInstance().fallbackListenerLevel));
    }

    /**
     * Constructs an instance using given properties.
     * <b>Users should not create new instances, but use {@link #getLogger()} instead!</b>
     *
     * @param name the logger name
     * @param messageFactory the message factory
     * @param config the configuration
     * @param fallbackListener the fallback listener
     * @throws NullPointerException on null {@code name}, {@code messageFactory}, {@code config}, or {@code fallbackListener}
     * @since 2.23.0
     */
    public StatusLogger(
            final String name,
            final MessageFactory messageFactory,
            final Config config,
            final StatusConsoleListener fallbackListener) {
        super(requireNonNull(name, "name"), requireNonNull(messageFactory, "messageFactory"));
        this.config = requireNonNull(config, "config");
        this.fallbackListener = requireNonNull(fallbackListener, "fallbackListener");
        this.listeners = new ArrayList<>();
    }

    /**
     * Gets the static instance.
     *
     * @return the singleton instance
     */
    public static StatusLogger getLogger() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Sets the static (i.e., singleton) instance returned by {@link #getLogger()}.
     * This method is intended for testing purposes and can have unforeseen consequences if used in production code.
     *
     * @param logger a logger instance
     * @throws NullPointerException on null {@code logger}
     * @since 2.23.0
     */
    public static void setLogger(final StatusLogger logger) {
        InstanceHolder.INSTANCE = requireNonNull(logger, "logger");
    }

    /**
     * Returns the fallback listener.
     *
     * @return the fallback listener
     */
    public StatusConsoleListener getFallbackListener() {
        return fallbackListener;
    }

    /**
     * Sets the level of the fallback listener.
     *
     * @param level a level
     * @deprecated Instead use the {@link StatusConsoleListener#setLevel(Level) setLevel(Level)} method on the fallback listener returned by {@link #getFallbackListener()}.
     */
    @Deprecated
    public void setLevel(final Level level) {
        requireNonNull(level, "level");
        fallbackListener.setLevel(level);
    }

    /**
     * Registers a new listener.
     *
     * @param listener a listener to register
     */
    public void registerListener(final StatusListener listener) {
        requireNonNull(listener, "listener");
        listenerWriteLock.lock();
        try {
            listeners.add(listener);
        } finally {
            listenerWriteLock.unlock();
        }
    }

    /**
     * Removes the given listener.
     *
     * @param listener a listener to remove
     */
    public void removeListener(final StatusListener listener) {
        requireNonNull(listener, "listener");
        listenerWriteLock.lock();
        try {
            listeners.remove(listener);
            closeListenerSafely(listener);
        } finally {
            listenerWriteLock.unlock();
        }
    }

    /**
     * Sets the level of the fallback listener.
     *
     * @param level a level
     * @deprecated Instead use the {@link StatusConsoleListener#setLevel(Level) setLevel(Level)} method on the fallback listener returned by {@link #getFallbackListener()}.
     */
    @Deprecated
    public void updateListenerLevel(final Level level) {
        requireNonNull(level, "level");
        fallbackListener.setLevel(level);
    }

    /**
     * Returns the listener collection.
     *
     * @return a thread-safe read-only collection of listeners
     */
    public Iterable<StatusListener> getListeners() {
        listenerReadLock.lock();
        try {
            return Collections.unmodifiableCollection(listeners);
        } finally {
            listenerReadLock.unlock();
        }
    }

    /**
     * Clears the event buffer, removes the <em>registered</em> (not the fallback one!) listeners, and resets the fallback listener.
     */
    public void reset() {
        listenerWriteLock.lock();
        try {
            final Iterator<StatusListener> listenerIterator = listeners.iterator();
            while (listenerIterator.hasNext()) {
                final StatusListener listener = listenerIterator.next();
                closeListenerSafely(listener);
                listenerIterator.remove();
            }
        } finally {
            listenerWriteLock.unlock();
        }
        fallbackListener.close();
        buffer.clear();
    }

    private static void closeListenerSafely(final StatusListener listener) {
        try {
            listener.close();
        } catch (final IOException error) {
            final String message = String.format("failed closing listener: %s", listener);
            new RuntimeException(message, error).printStackTrace(System.err);
        }
    }

    /**
     * Returns buffered events.
     *
     * @deprecated Instead of relying on the buffering provided by {@code StatusLogger}, users should register their own listeners to access to logged events.
     * @return a thread-safe read-only collection of buffered events
     */
    @Deprecated
    public List<StatusData> getStatusData() {
        // Wrapping the buffer clone with an unmodifiable list.
        // By disallowing modifications, we make it clear to the user that mutations will not get propagated.
        // `Collections.unmodifiableList(new ArrayList<>(...))` should be replaced with `List.of()` in Java 9+.
        return Collections.unmodifiableList(new ArrayList<>(buffer));
    }

    /**
     * Clears the event buffer.
     *
     * @deprecated Instead of relying on the buffering provided by {@code StatusLogger}, users should register their own listeners to access to logged events.
     */
    @Deprecated
    public void clear() {
        buffer.clear();
    }

    /**
     * Returns the least specific level among listeners, if registered any; otherwise, the fallback listener level.
     *
     * @return the least specific listener level, if registered any; otherwise, the fallback listener level
     */
    @Override
    public Level getLevel() {
        Level leastSpecificLevel = fallbackListener.getStatusLevel();
        // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
        for (int listenerIndex = 0; listenerIndex < listeners.size(); listenerIndex++) {
            final StatusListener listener = listeners.get(listenerIndex);
            final Level listenerLevel = listener.getStatusLevel();
            if (listenerLevel.isLessSpecificThan(leastSpecificLevel)) {
                leastSpecificLevel = listenerLevel;
            }
        }
        return leastSpecificLevel;
    }

    @Override
    public void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable throwable) {
        final StatusData statusData = createStatusData(fqcn, level, message, throwable);
        buffer(statusData);
        notifyListeners(statusData);
    }

    private void buffer(final StatusData statusData) {
        if (config.bufferCapacity == 0) {
            return;
        }
        buffer.add(statusData);
        while (buffer.size() >= config.bufferCapacity) {
            buffer.remove();
        }
    }

    private void notifyListeners(final StatusData statusData) {
        final boolean foundListeners;
        listenerReadLock.lock();
        try {
            foundListeners = !listeners.isEmpty();
            listeners.forEach(listener -> notifyListener(listener, statusData));
        } finally {
            listenerReadLock.unlock();
        }
        if (!foundListeners) {
            notifyListener(fallbackListener, statusData);
        }
    }

    private void notifyListener(final StatusListener listener, final StatusData statusData) {
        if (config.debugEnabled || listener.getStatusLevel().isLessSpecificThan(statusData.getLevel())) {
            listener.log(statusData);
        }
    }

    private StatusData createStatusData(
            @Nullable final String fqcn,
            final Level level,
            final Message message,
            @Nullable final Throwable throwable) {
        final StackTraceElement caller = getStackTraceElement(fqcn);
        return new StatusData(caller, level, message, throwable, null, config.instantFormatter);
    }

    @Nullable
    private static StackTraceElement getStackTraceElement(@Nullable final String fqcn) {
        if (fqcn == null) {
            return null;
        }
        boolean next = false;
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (final StackTraceElement element : stackTrace) {
            final String className = element.getClassName();
            if (next && !fqcn.equals(className)) {
                return element;
            }
            if (fqcn.equals(className)) {
                next = true;
            } else if ("?".equals(className)) {
                break;
            }
        }
        return null;
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object... params) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final String message, final Object p0) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level, final Marker marker, final CharSequence message, final Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Object message, final Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker, final Message message, final Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        requireNonNull(level, "level");
        return getLevel().isLessSpecificThan(level);
    }
}
