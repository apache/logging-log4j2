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
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ProviderUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * The anchor point for the logging system.
 */
public class LogManager {

    private static volatile LoggerContextFactory factory;

    private static final String FACTORY_PROPERTY_NAME = "log4j2.loggerContextFactory";

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * The name of the root Logger.
     */
    public static final String ROOT_LOGGER_NAME = Strings.EMPTY;

    /**
     * Scans the classpath to find all logging implementation. Currently, only one will
     * be used but this could be extended to allow multiple implementations to be used.
     */
    static {
        // Shortcut binding to force a specific logging implementation.
        final PropertiesUtil managerProps = PropertiesUtil.getProperties();
        final String factoryClass = managerProps.getStringProperty(FACTORY_PROPERTY_NAME);
        final ClassLoader cl = ProviderUtil.findClassLoader();
        if (factoryClass != null) {
            try {
                final Class<?> clazz = cl.loadClass(factoryClass);
                if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                    factory = (LoggerContextFactory) clazz.newInstance();
                }
            } catch (final ClassNotFoundException cnfe) {
                LOGGER.error("Unable to locate configured LoggerContextFactory {}", factoryClass);
            } catch (final Exception ex) {
                LOGGER.error("Unable to create configured LoggerContextFactory {}", factoryClass, ex);
            }
        }

        if (factory == null) {
            final SortedMap<Integer, LoggerContextFactory> factories = new TreeMap<Integer, LoggerContextFactory>();

            if (ProviderUtil.hasProviders()) {
                for (final Provider provider : ProviderUtil.getProviders()) {
                    final String className = provider.getClassName();
                    if (className != null) {
                        try {
                            final Class<?> clazz = cl.loadClass(className);
                            if (LoggerContextFactory.class.isAssignableFrom(clazz)) {
                                factories.put(provider.getPriority(), (LoggerContextFactory) clazz.newInstance());
                            } else {
                                LOGGER.error("{} does not implement {}", className, LoggerContextFactory.class.getName());
                            }
                        } catch (final ClassNotFoundException cnfe) {
                            LOGGER.error("Unable to locate class {} specified in {}", className,
                                provider.getURL().toString(), cnfe);
                        } catch (final IllegalAccessException iae) {
                            LOGGER.error("Unable to create class {} specified in {}", className,
                                provider.getURL().toString(), iae);
                        } catch (final Exception e) {
                            LOGGER.error("Unable to create class {} specified in {}", className,
                                provider.getURL().toString(), e);
                        }
                    }
                }

                if (factories.size() == 0) {
                    LOGGER.error("Unable to locate a logging implementation, using SimpleLogger");
                    factory = new SimpleLoggerContextFactory();
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
                LOGGER.error("Unable to locate a logging implementation, using SimpleLogger");
                factory = new SimpleLoggerContextFactory();
            }
        }
    }

    /**
     * Detects if a Logger with the specified name exists. This is a convenience method for porting from version 1.
     *
     * @param name
     *            The Logger name to search for.
     * @return true if the Logger exists, false otherwise.
     * @see LoggerContext#hasLogger(String)
     */
    public static boolean exists(final String name) {
        return getContext().hasLogger(name);
    }

    /**
     * Gets the class name of the caller in the current stack at the given {@code depth}.
     *
     * @param depth a 0-based index in the current stack.
     * @return a class name
     */
    private static String getClassName(final int depth) {
        return new Throwable().getStackTrace()[depth].getClassName();
    }

    /**
     * Returns the current LoggerContext.
     * <p>
     * WARNING - The LoggerContext returned by this method may not be the LoggerContext used to create a Logger
     * for the calling class.
     * @return  The current LoggerContext.
     */
    public static LoggerContext getContext() {
        return factory.getContext(LogManager.class.getName(), null, null, true);
    }

    /**
     * Returns a LoggerContext.
     *
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final boolean currentContext) {
        return factory.getContext(LogManager.class.getName(), null, null, currentContext, null, null);
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     * ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext) {
        return factory.getContext(LogManager.class.getName(), loader, null, currentContext);
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     * ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
                                           final Object externalContext) {
        return factory.getContext(LogManager.class.getName(), loader, externalContext, currentContext);
    }

    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     * ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @param configLocation The URI for the configuration to use.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
                                           final URI configLocation) {
        return factory.getContext(LogManager.class.getName(), loader, null, currentContext, configLocation, null);
    }


    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     * ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param configLocation The URI for the configuration to use.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
                                           final Object externalContext, final URI configLocation) {
        return factory.getContext(LogManager.class.getName(), loader, externalContext, currentContext, configLocation,
            null);
    }


    /**
     * Returns a LoggerContext.
     *
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     * ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param configLocation The URI for the configuration to use.
     * @param name The LoggerContext name.
     * @return a LoggerContext.
     */
    public static LoggerContext getContext(final ClassLoader loader, final boolean currentContext,
                                           final Object externalContext, final URI configLocation,
                                           final String name) {
        return factory.getContext(LogManager.class.getName(), loader, externalContext, currentContext, configLocation,
            name);
    }

    /**
     * Returns a LoggerContext
     * @param fqcn The fully qualified class name of the Class that this method is a member of.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    protected static LoggerContext getContext(final String fqcn, final boolean currentContext) {
        return factory.getContext(fqcn, null, null, currentContext);
    }

    /**
     * Returns a LoggerContext
     * @param fqcn The fully qualified class name of the Class that this method is a member of.
     * @param loader The ClassLoader for the context. If null the context will attempt to determine the appropriate
     * ClassLoader.
     * @param currentContext if false the LoggerContext appropriate for the caller of this method is returned. For
     * example, in a web application if the caller is a class in WEB-INF/lib then one LoggerContext may be
     * returned and if the caller is a class in the container's classpath then a different LoggerContext may be
     * returned. If true then only a single LoggerContext will be returned.
     * @return a LoggerContext.
     */
    protected static LoggerContext getContext(final String fqcn, final ClassLoader loader,
                                              final boolean currentContext) {
        return factory.getContext(fqcn, loader, null, currentContext);
    }

    /**
     * Returns the current LoggerContextFactory.
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
     * Returns a formatter Logger using the fully qualified name of the Class as the Logger name.
     * <p>
     * This logger let you use a {@link java.util.Formatter} string in the message to format parameters.
     * </p>
     * <p>
     * Short-hand for {@code getLogger(clazz, StringFormatterMessageFactory.INSTANCE)}
     * </p>
     *
     * @param clazz
     *            The Class whose name should be used as the Logger name.
     * @return The Logger, created with a {@link StringFormatterMessageFactory}
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
        return getLogger(clazz != null ? clazz.getName() : getClassName(2), StringFormatterMessageFactory.INSTANCE);
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
     * @param value
     *            The value's whose class name should be used as the Logger name.
     * @return The Logger, created with a {@link StringFormatterMessageFactory}
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
        return getLogger(value != null ? value.getClass().getName() : getClassName(2),
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
        return getLogger(name != null ? name : getClassName(2), StringFormatterMessageFactory.INSTANCE);
    }

    /**
     * Returns a Logger with the name of the calling class.
     * @return The Logger for the calling class.
     */
    public static Logger getLogger() {
        return getLogger(getClassName(2));
    }

    /**
     * Returns a Logger using the fully qualified name of the Class as the Logger name.
     * @param clazz The Class whose name should be used as the Logger name. If null it will default to the calling
     *              class.
     * @return The Logger.
     */
    public static Logger getLogger(final Class<?> clazz) {
        return getLogger(clazz != null ? clazz.getName() : getClassName(2));
    }

    /**
     * Returns a Logger using the fully qualified name of the Class as the Logger name.
     * @param clazz The Class whose name should be used as the Logger name. If null it will default to the calling
     *              class.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The Logger.
     */
    public static Logger getLogger(final Class<?> clazz, final MessageFactory messageFactory) {
        return getLogger(clazz != null ? clazz.getName() : getClassName(2), messageFactory);
    }

    /**
     * Returns a Logger with the name of the calling class.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The Logger for the calling class.
     */
    public static Logger getLogger(final MessageFactory messageFactory) {
        return getLogger(getClassName(2), messageFactory);
    }

    /**
     * Returns a Logger using the fully qualified class name of the value as the Logger name.
     * @param value The value whose class name should be used as the Logger name. If null the name of the calling
     *              class will be used as the logger name.
     * @return The Logger.
     */
    public static Logger getLogger(final Object value) {
        return getLogger(value != null ? value.getClass().getName() : getClassName(2));
    }

    /**
     * Returns a Logger using the fully qualified class name of the value as the Logger name.
     * @param value The value whose class name should be used as the Logger name. If null the name of the calling
     *              class will be used as the logger name.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The Logger.
     */
    public static Logger getLogger(final Object value, final MessageFactory messageFactory) {
        return getLogger(value != null ? value.getClass().getName() : getClassName(2), messageFactory);
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will be used.
     * @return The Logger.
     */
    public static Logger getLogger(final String name) {
        final String actualName = name != null ? name : getClassName(2);
        return factory.getContext(LogManager.class.getName(), null, null, false).getLogger(actualName);
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will be used.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The Logger.
     */
    public static Logger getLogger(final String name, final MessageFactory messageFactory) {
        final String actualName = name != null ? name : getClassName(2);
        return factory.getContext(LogManager.class.getName(), null, null, false).getLogger(actualName, messageFactory);
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

    /**
     * Prevents instantiation
     */
    protected LogManager() {
    }

}
