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
package org.apache.logging.log4j;

import java.net.URI;
import org.apache.logging.log4j.internal.LogManagerStatus;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Terminable;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ProviderUtil;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * The anchor point for the Log4j logging system. The most common usage of this class is to obtain a named
 * {@link Logger}. The method {@link #getLogger()} is provided as the most convenient way to obtain a named Logger based
 * on the calling class name. This class also provides method for obtaining named Loggers that use
 * {@link String#format(String, Object...)} style messages instead of the default type of parameterized messages. These
 * are obtained through the {@link #getFormatterLogger(Class)} family of methods. Other service provider methods are
 * given through the {@link #getContext()} and {@link #getFactory()} family of methods; these methods are not normally
 * useful for typical usage of Log4j.
 */
public class LogManager {

    /**
     * Log4j's property to set to the fully qualified class name of a custom implementation of
     * {@link LoggerContextFactory}.
     * @deprecated Replaced since 2.24.0 with {@value org.apache.logging.log4j.spi.Provider#PROVIDER_PROPERTY_NAME}.
     */
    @Deprecated
    public static final String FACTORY_PROPERTY_NAME = "log4j2.loggerContextFactory";

    /**
     * The name of the root Logger.
     */
    public static final String ROOT_LOGGER_NAME = Strings.EMPTY;

    private static final Logger LOGGER = StatusLogger.getLogger();

    // for convenience
    private static final String FQCN = LogManager.class.getName();

    private static volatile LoggerContextFactory factory =
            ProviderUtil.getProvider().getLoggerContextFactory();

    /*
     * Scans the classpath to find all logging implementation. Currently, only one will be used but this could be
     * extended to allow multiple implementations to be used.
     */
    static {
        LogManagerStatus.setInitialized(true);
    }

    /**
     * Prevents instantiation
     */
    protected LogManager() {}

    /**
     * Detects if a Logger with the specified name exists. This is a convenience method for porting from version 1.
     *
     * @param name The Logger name to search for.
     * @return true if the Logger exists, false otherwise.
     * @see LoggerContext#hasLogger(String)
     */
    public static boolean exists(final String name) {
        return getContext().hasLogger(name);
    }

    /**
     * Returns the current LoggerContext.
     * <p>
     * WARNING - The LoggerContext returned by this method may not be the LoggerContext used to create a Logger for the
     * calling class.
     * </p>
     *
     * @return The current LoggerContext.
     */
    public static LoggerContext getContext() {
        try {
            return factory.getContext(FQCN, null, null, true);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(FQCN, null, null, true);
        }
    }

    /**
     * Returns a LoggerContext.
     *
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final boolean currentContext) {
        // TODO: would it be a terrible idea to try and find the caller ClassLoader here?
        try {
            return factory.getContext(FQCN, null, null, currentContext, null, null);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(FQCN, null, null, currentContext, null, null);
        }
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext) {
        try {
            return factory.getContext(FQCN, loader, null, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(FQCN, loader, null, currentContext);
        }
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(
            final ClassLoader loader, final boolean currentContext, final Object externalContext) {
        try {
            return factory.getContext(FQCN, loader, externalContext, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(FQCN, loader, externalContext, currentContext);
        }
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @param configLocation The URI for the configuration to use.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(
            final ClassLoader loader, final boolean currentContext, final URI configLocation) {
        try {
            return factory.getContext(FQCN, loader, null, currentContext, configLocation, null);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(
                    FQCN, loader, null, currentContext, configLocation, null);
        }
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param configLocation The URI for the configuration to use.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(
            final ClassLoader loader,
            final boolean currentContext,
            final Object externalContext,
            final URI configLocation) {
        try {
            return factory.getContext(FQCN, loader, externalContext, currentContext, configLocation, null);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(
                    FQCN, loader, externalContext, currentContext, configLocation, null);
        }
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param configLocation The URI for the configuration to use.
     * @param name The LoggerContext name.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(
            final ClassLoader loader,
            final boolean currentContext,
            final Object externalContext,
            final URI configLocation,
            final String name) {
        try {
            return factory.getContext(FQCN, loader, externalContext, currentContext, configLocation, name);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(
                    FQCN, loader, externalContext, currentContext, configLocation, name);
        }
    }

    /**
     * Returns a LoggerContext
     *
     * @param fqcn The fully qualified class name of the Class that this method is a member of.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    protected static LoggerContext getContext(final String fqcn, final boolean currentContext) {
        try {
            return factory.getContext(fqcn, null, null, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(fqcn, null, null, currentContext);
        }
    }

    /**
     * Returns a LoggerContext
     *
     * @param fqcn The fully qualified class name of the Class that this method is a member of.
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    protected static LoggerContext getContext(
            final String fqcn, final ClassLoader loader, final boolean currentContext) {
        try {
            return factory.getContext(fqcn, loader, null, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(fqcn, loader, null, currentContext);
        }
    }

    /**
     * Returns a LoggerContext
     *
     * @param fqcn The fully qualified class name of the Class that this method is a member of.
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     *            ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            returned and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be returned. If true then only a single LoggerContext will be returned.
     * @param configLocation The URI for the configuration to use.
     * @param name The LoggerContext name.
     * @return a LoggerContext.
     */
    protected static LoggerContext getContext(
            final String fqcn,
            final ClassLoader loader,
            final boolean currentContext,
            final URI configLocation,
            final String name) {
        try {
            return factory.getContext(fqcn, loader, null, currentContext, configLocation, name);
        } catch (final IllegalStateException ex) {
            LOGGER.warn("{} Using SimpleLogger", ex.getMessage());
            return SimpleLoggerContextFactory.INSTANCE.getContext(fqcn, loader, null, currentContext);
        }
    }

    /**
     * Shutdown using the LoggerContext appropriate for the caller of this method.
     * This is equivalent to calling {@code LogManager.shutdown(false)}.
     * <p>
     *     This call is synchronous and will block until shut down is complete. This may include flushing pending log
     *     events over network connections.
     * </p>
     * @since 2.6
     */
    public static void shutdown() {
        shutdown(false);
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     * This is equivalent to calling {@code LogManager.shutdown(LogManager.getContext(currentContext))}.
     * <p>
     *     This call is synchronous and will block until shut down is complete. This may include flushing pending log
     *     events over network connections.
     * </p>
     *
     * @param currentContext if true a default LoggerContext (may not be the LoggerContext used to create a Logger
     *            for the calling class) will be used.
     *            If false the LoggerContext appropriate for the caller of this method is used. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            used and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be used.
     * @since 2.6
     */
    public static void shutdown(final boolean currentContext) {
        factory.shutdown(FQCN, null, currentContext, false);
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     * This is equivalent to calling {@code LogManager.shutdown(LogManager.getContext(currentContext))}.
     * <p>
     *     This call is synchronous and will block until shut down is complete. This may include flushing pending log
     *     events over network connections.
     * </p>
     *
     * @param currentContext if true a default LoggerContext (may not be the LoggerContext used to create a Logger
     *            for the calling class) will be used.
     *            If false the LoggerContext appropriate for the caller of this method is used. For
     *            example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     *            used and if the caller is a class in the container's classpath then a different LoggerContext may
     *            be used.
     * @param allContexts if true all LoggerContexts that can be located will be shutdown.
     * @since 2.13.0
     */
    public static void shutdown(final boolean currentContext, final boolean allContexts) {
        factory.shutdown(FQCN, null, currentContext, allContexts);
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     * <p>
     *     This call is synchronous and will block until shut down is complete. This may include flushing pending log
     *     events over network connections.
     * </p>
     *
     * @param context the LoggerContext.
     * @since 2.6
     */
    public static void shutdown(final LoggerContext context) {
        if (context instanceof Terminable) {
            ((Terminable) context).terminate();
        }
    }

    /**
     * Returns the current LoggerContextFactory.
     *
     * @return The LoggerContextFactory.
     */
    public static LoggerContextFactory getFactory() {
        return factory;
    }

    /**
     * Sets the current LoggerContextFactory to use. Normally, the appropriate LoggerContextFactory is created at
     * startup, but in certain environments, a LoggerContextFactory implementation may not be available at this point.
     * Thus, an alternative LoggerContextFactory can be set at runtime.
     *
     * <p>
     * Note that any Logger or LoggerContext objects already created will still be valid, but they will no longer be
     * accessible through LogManager. Thus, <strong>it is a bad idea to use this method without a good reason</strong>!
     * Generally, this method should be used only during startup before any code starts caching Logger objects.
     * </p>
     *
     * @param factory the LoggerContextFactory to use.
     */
    // FIXME: should we allow only one update of the factory?
    public static void setFactory(final LoggerContextFactory factory) {
        LogManager.factory = factory;
    }

    /**
     * Returns a formatter Logger using the fully qualified name of the calling Class as the Logger name.
     * <p>
     * This logger lets you use a {@link java.util.Formatter} string in the message to format parameters.
     * </p>
     *
     * @return The Logger for the calling class.
     * @throws UnsupportedOperationException if the calling class cannot be determined.
     * @since 2.4
     */
    public static Logger getFormatterLogger() {
        return getFormatterLogger(StackLocatorUtil.getCallerClass(2));
    }

    /**
     * Returns a formatter Logger using the fully qualified name of the Class as the Logger name.
     * <p>
     * This logger let you use a {@link java.util.Formatter} string in the message to format parameters.
     * </p>
     * <p>
     * Short-hand for {@code getLogger(clazz, StringFormatterMessageFactory.INSTANCE)}
     * </p>
     *
     * @param clazz The Class whose name should be used as the Logger name.
     * @return The Logger, created with a {@link StringFormatterMessageFactory}
     * @throws UnsupportedOperationException if {@code clazz} is {@code null} and the calling class cannot be
     *             determined.
     * @see Logger#fatal(Marker, String, Object...)
     * @see Logger#fatal(String, Object...)
     * @see Logger#error(Marker, String, Object...)
     * @see Logger#error(String, Object...)
     * @see Logger#warn(Marker, String, Object...)
     * @see Logger#warn(String, Object...)
     * @see Logger#info(Marker, String, Object...)
     * @see Logger#info(String, Object...)
     * @see Logger#debug(Marker, String, Object...)
     * @see Logger#debug(String, Object...)
     * @see Logger#trace(Marker, String, Object...)
     * @see Logger#trace(String, Object...)
     * @see StringFormatterMessageFactory
     */
    public static Logger getFormatterLogger(final Class<?> clazz) {
        return getLogger(
                clazz != null ? clazz : StackLocatorUtil.getCallerClass(2), StringFormatterMessageFactory.INSTANCE);
    }

    /**
     * Returns a formatter Logger using the fully qualified name of the value's Class as the Logger name.
     * <p>
     * This logger let you use a {@link java.util.Formatter} string in the message to format parameters.
     * </p>
     * <p>
     * Short-hand for {@code getLogger(value, StringFormatterMessageFactory.INSTANCE)}
     * </p>
     *
     * @param value The value whose class name should be used as the Logger name.
     * @return The Logger, created with a {@link StringFormatterMessageFactory}
     * @throws UnsupportedOperationException if {@code value} is {@code null} and the calling class cannot be
     *             determined.
     * @see Logger#fatal(Marker, String, Object...)
     * @see Logger#fatal(String, Object...)
     * @see Logger#error(Marker, String, Object...)
     * @see Logger#error(String, Object...)
     * @see Logger#warn(Marker, String, Object...)
     * @see Logger#warn(String, Object...)
     * @see Logger#info(Marker, String, Object...)
     * @see Logger#info(String, Object...)
     * @see Logger#debug(Marker, String, Object...)
     * @see Logger#debug(String, Object...)
     * @see Logger#trace(Marker, String, Object...)
     * @see Logger#trace(String, Object...)
     * @see StringFormatterMessageFactory
     */
    public static Logger getFormatterLogger(final Object value) {
        return getLogger(
                value != null ? value.getClass() : StackLocatorUtil.getCallerClass(2),
                StringFormatterMessageFactory.INSTANCE);
    }

    /**
     * Returns a formatter Logger with the specified name.
     * <p>
     * This logger let you use a {@link java.util.Formatter} string in the message to format parameters.
     * </p>
     * <p>
     * Short-hand for {@code getLogger(name, StringFormatterMessageFactory.INSTANCE)}
     * </p>
     *
     * @param name The logger name. If null it will default to the name of the calling class.
     * @return The Logger, created with a {@link StringFormatterMessageFactory}
     * @throws UnsupportedOperationException if {@code name} is {@code null} and the calling class cannot be determined.
     * @see Logger#fatal(Marker, String, Object...)
     * @see Logger#fatal(String, Object...)
     * @see Logger#error(Marker, String, Object...)
     * @see Logger#error(String, Object...)
     * @see Logger#warn(Marker, String, Object...)
     * @see Logger#warn(String, Object...)
     * @see Logger#info(Marker, String, Object...)
     * @see Logger#info(String, Object...)
     * @see Logger#debug(Marker, String, Object...)
     * @see Logger#debug(String, Object...)
     * @see Logger#trace(Marker, String, Object...)
     * @see Logger#trace(String, Object...)
     * @see StringFormatterMessageFactory
     */
    public static Logger getFormatterLogger(final String name) {
        return name == null
                ? getFormatterLogger(StackLocatorUtil.getCallerClass(2))
                : getLogger(name, StringFormatterMessageFactory.INSTANCE);
    }

    private static Class<?> callerClass(final Class<?> clazz) {
        if (clazz != null) {
            return clazz;
        }
        final Class<?> candidate = StackLocatorUtil.getCallerClass(3);
        if (candidate == null) {
            throw new UnsupportedOperationException("No class provided, and an appropriate one cannot be found.");
        }
        return candidate;
    }

    /**
     * Returns a Logger with the name of the calling class.
     *
     * @return The Logger for the calling class.
     * @throws UnsupportedOperationException if the calling class cannot be determined.
     */
    public static Logger getLogger() {
        return getLogger(StackLocatorUtil.getCallerClass(2));
    }

    /**
     * Returns a Logger using the fully qualified name of the Class as the Logger name.
     *
     * @param clazz The Class whose name should be used as the Logger name. If null it will default to the calling
     *            class.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code clazz} is {@code null} and the calling class cannot be
     *             determined.
     */
    public static Logger getLogger(final Class<?> clazz) {
        final Class<?> cls = callerClass(clazz);
        return getContext(cls.getClassLoader(), false).getLogger(cls);
    }

    /**
     * Returns a Logger using the fully qualified name of the Class as the Logger name.
     *
     * @param clazz The Class whose name should be used as the Logger name. If null it will default to the calling
     *            class.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *            logger but will log a warning if mismatched.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code clazz} is {@code null} and the calling class cannot be
     *             determined.
     */
    public static Logger getLogger(final Class<?> clazz, final MessageFactory messageFactory) {
        final Class<?> cls = callerClass(clazz);
        return getContext(cls.getClassLoader(), false).getLogger(cls, messageFactory);
    }

    /**
     * Returns a Logger with the name of the calling class.
     *
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *            logger but will log a warning if mismatched.
     * @return The Logger for the calling class.
     * @throws UnsupportedOperationException if the calling class cannot be determined.
     */
    public static Logger getLogger(final MessageFactory messageFactory) {
        return getLogger(StackLocatorUtil.getCallerClass(2), messageFactory);
    }

    /**
     * Returns a Logger using the fully qualified class name of the value as the Logger name.
     *
     * @param value The value whose class name should be used as the Logger name. If null the name of the calling class
     *            will be used as the logger name.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code value} is {@code null} and the calling class cannot be
     *             determined.
     */
    public static Logger getLogger(final Object value) {
        return getLogger(value != null ? value.getClass() : StackLocatorUtil.getCallerClass(2));
    }

    /**
     * Returns a Logger using the fully qualified class name of the value as the Logger name.
     *
     * @param value The value whose class name should be used as the Logger name. If null the name of the calling class
     *            will be used as the logger name.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *            logger but will log a warning if mismatched.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code value} is {@code null} and the calling class cannot be
     *             determined.
     */
    public static Logger getLogger(final Object value, final MessageFactory messageFactory) {
        return getLogger(value != null ? value.getClass() : StackLocatorUtil.getCallerClass(2), messageFactory);
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will be used.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code name} is {@code null} and the calling class cannot be determined.
     */
    public static Logger getLogger(final String name) {
        return name != null ? getContext(false).getLogger(name) : getLogger(StackLocatorUtil.getCallerClass(2));
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will be used.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *            logger but will log a warning if mismatched.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code name} is {@code null} and the calling class cannot be determined.
     */
    public static Logger getLogger(final String name, final MessageFactory messageFactory) {
        return name != null
                ? getContext(false).getLogger(name, messageFactory)
                : getLogger(StackLocatorUtil.getCallerClass(2), messageFactory);
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param fqcn The fully qualified class name of the class that this method is a member of.
     * @param name The logger name.
     * @return The Logger.
     */
    protected static Logger getLogger(final String fqcn, final String name) {
        return factory.getContext(fqcn, null, null, false).getLogger(name);
    }

    /**
     * Returns the root logger.
     *
     * @return the root logger, named {@link #ROOT_LOGGER_NAME}.
     */
    public static Logger getRootLogger() {
        return getLogger(ROOT_LOGGER_NAME);
    }
}
