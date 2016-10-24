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

import java.net.URI;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.Terminable;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ProviderUtil;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * The anchor point for the logging system. The most common usage of this class is to obtain a named {@link Logger}. The
 * method {@link #getLogger()} is provided as the most convenient way to obtain a named Logger based on the calling
 * class name. This class also provides method for obtaining named Loggers that use
 * {@link String#format(String, Object...)} style messages instead of the default type of parameterized messages. These
 * are obtained through the {@link #getFormatterLogger(Class)} family of methods. Other service provider methods are
 * given through the {@link #getContext()} and {@link #getFactory()} family of methods; these methods are not normally
 * useful for typical usage of Log4j.
 */
public class LogManager {

    /**
     * Log4j property to set to the fully qualified class name of a custom implementation of
     * {@link org.apache.logging.log4j.spi.LoggerContextFactory}.
     */
    public static final String FACTORY_PROPERTY_NAME = "log4j2.loggerContextFactory";

    /**
     * The name of the root Logger.
     */
    public static final String ROOT_LOGGER_NAME = Strings.EMPTY;

    private static final Logger LOGGER = StatusLogger.getLogger();

    // for convenience
    private static final String FQCN = LogManager.class.getName();

    private static volatile LoggerContextFactory factory;

    /**
     * Scans the classpath to find all logging implementation. Currently, only one will be used but this could be
     * extended to allow multiple implementations to be used.
     */
    static {
        // Shortcut binding to force a specific logging implementation.
        final PropertiesUtil managerProps = PropertiesUtil.getProperties();
        final String factoryClassName = managerProps.getStringProperty(FACTORY_PROPERTY_NAME);
        if (factoryClassName != null) {
            try {
                factory = LoaderUtil.newCheckedInstanceOf(factoryClassName, LoggerContextFactory.class);
            } catch (final ClassNotFoundException cnfe) {
                LOGGER.error("Unable to locate configured LoggerContextFactory {}", factoryClassName);
            } catch (final Exception ex) {
                LOGGER.error("Unable to create configured LoggerContextFactory {}", factoryClassName, ex);
            }
        }

        if (factory == null) {
            final SortedMap<Integer, LoggerContextFactory> factories = new TreeMap<>();
            // note that the following initial call to ProviderUtil may block until a Provider has been installed when
            // running in an OSGi environment
            if (ProviderUtil.hasProviders()) {
                for (final Provider provider : ProviderUtil.getProviders()) {
                    final Class<? extends LoggerContextFactory> factoryClass = provider.loadLoggerContextFactory();
                    if (factoryClass != null) {
                        try {
                            factories.put(provider.getPriority(), factoryClass.newInstance());
                        } catch (final Exception e) {
                            LOGGER.error("Unable to create class {} specified in {}", factoryClass.getName(), provider
                                    .getUrl().toString(), e);
                        }
                    }
                }

                if (factories.isEmpty()) {
                    LOGGER.error("Log4j2 could not find a logging implementation. "
                            + "Please add log4j-core to the classpath. Using SimpleLogger to log to the console...");
                    factory = new SimpleLoggerContextFactory();
                } else if (factories.size() == 1) {
                    factory = factories.get(factories.lastKey());
                } else {
                    final StringBuilder sb = new StringBuilder("Multiple logging implementations found: \n");
                    for (final Map.Entry<Integer, LoggerContextFactory> entry : factories.entrySet()) {
                        sb.append("Factory: ").append(entry.getValue().getClass().getName());
                        sb.append(", Weighting: ").append(entry.getKey()).append('\n');
                    }
                    factory = factories.get(factories.lastKey());
                    sb.append("Using factory: ").append(factory.getClass().getName());
                    LOGGER.warn(sb.toString());

                }
            } else {
                LOGGER.error("Log4j2 could not find a logging implementation. "
                        + "Please add log4j-core to the classpath. Using SimpleLogger to log to the console...");
                factory = new SimpleLoggerContextFactory();
            }
        }
    }

    /**
     * Prevents instantiation
     */
    protected LogManager() {
    }

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
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, null, null, true);
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
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, null, null, currentContext, null, null);
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
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, loader, null, currentContext);
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
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
            final Object externalContext) {
        try {
            return factory.getContext(FQCN, loader, externalContext, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, loader, externalContext, currentContext);
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
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
            final URI configLocation) {
        try {
            return factory.getContext(FQCN, loader, null, currentContext, configLocation, null);
        } catch (final IllegalStateException ex) {
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, loader, null, currentContext, configLocation,
                    null);
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
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
            final Object externalContext, final URI configLocation) {
        try {
            return factory.getContext(FQCN, loader, externalContext, currentContext, configLocation, null);
        } catch (final IllegalStateException ex) {
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, loader, externalContext, currentContext,
                    configLocation, null);
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
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
            final Object externalContext, final URI configLocation, final String name) {
        try {
            return factory.getContext(FQCN, loader, externalContext, currentContext, configLocation, name);
        } catch (final IllegalStateException ex) {
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(FQCN, loader, externalContext, currentContext,
                    configLocation, name);
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
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(fqcn, null, null, currentContext);
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
    protected static LoggerContext getContext(final String fqcn, final ClassLoader loader,
            final boolean currentContext) {
        try {
            return factory.getContext(fqcn, loader, null, currentContext);
        } catch (final IllegalStateException ex) {
            LOGGER.warn(ex.getMessage() + " Using SimpleLogger");
            return new SimpleLoggerContextFactory().getContext(fqcn, loader, null, currentContext);
        }
    }

    /**
     * Shutdown using the LoggerContext appropriate for the caller of this method.
     * This is equivalent to calling {@code LogManager.shutdown(false)}.
     *
     * This call is synchronous and will block until shut down is complete.
     * This may include flushing pending log events over network connections.
     *
     * @since 2.6
     */
    public static void shutdown() {
        shutdown(false);
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     * This is equivalent to calling {@code LogManager.shutdown(LogManager.getContext(currentContext))}.
     *
     * This call is synchronous and will block until shut down is complete.
     * This may include flushing pending log events over network connections.
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
        shutdown(getContext(currentContext));
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     *
     * This call is synchronous and will block until shut down is complete.
     * This may include flushing pending log events over network connections.
     *
     * @param context the LoggerContext.
     * @since 2.6
     */
    public static void shutdown(final LoggerContext context) {
        if (context != null && context instanceof Terminable) {
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
        return getFormatterLogger(ReflectionUtil.getCallerClass(2));
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
        return getLogger(clazz != null ? clazz : ReflectionUtil.getCallerClass(2),
                StringFormatterMessageFactory.INSTANCE);
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
     * @param value The value's whose class name should be used as the Logger name.
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
        return getLogger(value != null ? value.getClass() : ReflectionUtil.getCallerClass(2),
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
        return name == null ? getFormatterLogger(ReflectionUtil.getCallerClass(2)) : getLogger(name,
                StringFormatterMessageFactory.INSTANCE);
    }

    private static Class<?> callerClass(final Class<?> clazz) {
        if (clazz != null) {
            return clazz;
        }
        final Class<?> candidate = ReflectionUtil.getCallerClass(3);
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
        return getLogger(ReflectionUtil.getCallerClass(2));
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
        return getContext(cls.getClassLoader(), false).getLogger(cls.getName());
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
        return getContext(cls.getClassLoader(), false).getLogger(cls.getName(), messageFactory);
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
        return getLogger(ReflectionUtil.getCallerClass(2), messageFactory);
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
        return getLogger(value != null ? value.getClass() : ReflectionUtil.getCallerClass(2));
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
        return getLogger(value != null ? value.getClass() : ReflectionUtil.getCallerClass(2), messageFactory);
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will be used.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code name} is {@code null} and the calling class cannot be determined.
     */
    public static Logger getLogger(final String name) {
        return name != null ? getContext(false).getLogger(name) : getLogger(ReflectionUtil.getCallerClass(2));
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
        return name != null ? getContext(false).getLogger(name, messageFactory) : getLogger(
                ReflectionUtil.getCallerClass(2), messageFactory);
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
