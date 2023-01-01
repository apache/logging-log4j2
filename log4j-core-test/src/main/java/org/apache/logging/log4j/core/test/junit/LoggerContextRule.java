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
package org.apache.logging.log4j.core.test.junit;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.CleanFiles;
import org.apache.logging.log4j.test.junit.CleanFolders;
import org.apache.logging.log4j.util.Cast;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.junit.Assert.assertNotNull;

/**
 * JUnit {@link TestRule} for constructing a new LoggerContext using a specified configuration file. If the system
 * property {@code EBUG} is set (e.g., through the command line option {@code -DEBUG}), then the StatusLogger will be
 * set to the debug level. This allows for more debug messages as the StatusLogger will be in the error level until a
 * configuration file has been read and parsed into a tree of Nodes.
 *
 * @see LoggerContextSource
 * @see Named
 */
public class LoggerContextRule implements TestRule, LoggerContextAccessor {

    public static LoggerContextRule createShutdownTimeoutLoggerContextRule(final String config) {
        return new LoggerContextRule(config, 10, TimeUnit.SECONDS);
    }

    private final LoggingTestConfiguration configuration;
    private final String configurationLocation;
    private LoggerContext loggerContext;
    private Class<? extends ContextSelector> contextSelectorClass;
    private String testClassName;

    /**
     * Constructs a new LoggerContextRule without a configuration file.
     */
    public LoggerContextRule() {
        this(null, null);
    }

    /**
     * Constructs a new LoggerContextRule for a given configuration file.
     *
     * @param configurationLocation
     *            path to configuration file
     */
    public LoggerContextRule(final String configurationLocation) {
        this(configurationLocation, null);
    }

    /**
     * Constructs a new LoggerContextRule for a given configuration file and a custom {@link ContextSelector} class.
     *
     * @param configurationLocation
     *            path to configuration file
     * @param contextSelectorClass
     *            custom ContextSelector class to use instead of default
     */
    public LoggerContextRule(final String configurationLocation, final Class<? extends ContextSelector> contextSelectorClass) {
        this(configurationLocation, contextSelectorClass, AbstractLifeCycle.DEFAULT_STOP_TIMEOUT,
                AbstractLifeCycle.DEFAULT_STOP_TIMEUNIT);
    }

    public LoggerContextRule(final String configurationLocation, final Class<? extends ContextSelector> contextSelectorClass,
            final long shutdownTimeout, final TimeUnit shutdownTimeUnit) {
        configuration = new LoggingTestConfiguration()
                .setConfigurationLocation(configurationLocation)
                .setTimeout(shutdownTimeout, shutdownTimeUnit)
                // TODO(ms): can support ReconfigurationPolicy if we allow reuse of the context like in LoggerContextResolver
                .setBootstrap(true);
        this.configurationLocation = configurationLocation;
        this.contextSelectorClass = contextSelectorClass;
    }

    public LoggerContextRule(final String configurationLocation, final int shutdownTimeout, final TimeUnit shutdownTimeUnit) {
        this(configurationLocation, null, shutdownTimeout, shutdownTimeUnit);
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        // Hack: Using -DEBUG as a JVM param sets a property called "EBUG"...
        if (System.getProperties().containsKey("EBUG")) {
            StatusLogger.getLogger().setLevel(Level.DEBUG);
        }
        configuration.setClassLoader(description.getTestClass().getClassLoader())
                .setContextName(Keys.getSpecifiedName(description.getAnnotations()).orElse(description.getMethodName()));
        testClassName = description.getClassName();
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final Consumer<Injector> configurer;
                if (contextSelectorClass != null) {
                    configurer = injector -> injector
                            .registerBinding(ContextSelector.KEY, injector.getFactory(contextSelectorClass));
                } else {
                    configurer = null;
                }
                final LoggingTestContext context = configuration.build();
                context.init(configurer);
                loggerContext = context.getLoggerContext();
                try {
                    base.evaluate();
                } finally {
                    context.close();
                    loggerContext = null;
                    contextSelectorClass = null;
                    StatusLogger.getLogger().reset();
                }
            }
        };

    }

    /**
     * Gets a named Appender for this LoggerContext.
     *
     * @param name
     *            the name of the Appender to look up.
     * @return the named Appender or {@code null} if it wasn't defined in the configuration.
     */
     public <T extends Appender> T getAppender(final String name) {
         return Cast.cast(getConfiguration().getAppenders().get(name));
     }

    /**
     * Gets a named Appender for this LoggerContext.
     *
     * @param <T>
     *            The target Appender class
     * @param name
     *            the name of the Appender to look up.
     * @param cls
     *            The target Appender class
     * @return the named Appender or {@code null} if it wasn't defined in the configuration.
     */
    public <T extends Appender> T getAppender(final String name, final Class<T> cls) {
        return cls.cast(getConfiguration().getAppenders().get(name));
    }

    /**
     * Gets the associated Configuration for the configuration file this was constructed with.
     *
     * @return this LoggerContext's Configuration.
     */
    public Configuration getConfiguration() {
        return loggerContext.getConfiguration();
    }

    /**
     * Gets the configuration location.
     *
     * @return the configuration location.
     */
    public String getConfigurationLocation() {
        return configurationLocation;
    }

    /**
     * Gets the current LoggerContext associated with this rule.
     *
     * @return the current LoggerContext.
     */
    @Override
    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    /**
     * Gets a named ListAppender or throws an exception for this LoggerContext.
     *
     * @param name
     *            the name of the ListAppender to look up.
     * @return the named ListAppender.
     * @throws AssertionError
     *             if the named ListAppender doesn't exist or isn't a ListAppender.
     */
    public ListAppender getListAppender(final String name) {
        final Appender appender = getAppender(name);
        if (appender instanceof ListAppender) {
            return (ListAppender) appender;
        }
        throw new AssertionError("No ListAppender named " + name + " found.");
    }

    /**
     * Gets a named Logger using the test class's name from this LoggerContext.
     *
     * @return the test class's named Logger.
     */
    public Logger getLogger() {
        return loggerContext.getLogger(testClassName);
    }

    /**
     * Gets a named Logger for the given class in this LoggerContext.
     *
     * @param clazz
     *            The Class whose name should be used as the Logger name. If null it will default to the calling class.
     * @return the named Logger.
     */
    public Logger getLogger(final Class<?> clazz) {
        return loggerContext.getLogger(clazz.getName());
    }

    /**
     * Gets a named Logger in this LoggerContext.
     *
     * @param name
     *            the name of the Logger to look up or create.
     * @return the named Logger.
     */
    public Logger getLogger(final String name) {
        return loggerContext.getLogger(name);
    }

    /**
     * Gets a named Appender or throws an exception for this LoggerContext.
     *
     * @param name
     *            the name of the Appender to look up.
     * @return the named Appender.
     * @throws AssertionError
     *             if the Appender doesn't exist.
     */
    public Appender getRequiredAppender(final String name) {
        final Appender appender = getAppender(name);
        assertNotNull("Appender named " + name + " was null.", appender);
        return appender;
    }

    /**
     * Gets a named Appender or throws an exception for this LoggerContext.
     *
     * @param <T>
     *            The target Appender class
     * @param name
     *            the name of the Appender to look up.
     * @param cls
     *            The target Appender class
     * @return the named Appender.
     * @throws AssertionError
     *             if the Appender doesn't exist.
     */
    public <T extends Appender> T getRequiredAppender(final String name, final Class<T> cls) {
        final T appender = getAppender(name, cls);
        assertNotNull("Appender named " + name + " was null in logger context " + loggerContext, appender);
        return appender;
    }

    /**
     * Gets the root logger.
     *
     * @return the root logger.
     */
    public Logger getRootLogger() {
        return loggerContext.getRootLogger();
    }

    public void reconfigure() {
        loggerContext.reconfigure();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LoggerContextRule [configLocation=");
        builder.append(configurationLocation);
        builder.append(", contextSelectorClass=");
        builder.append(contextSelectorClass);
        builder.append("]");
        return builder.toString();
    }

    public RuleChain withCleanFilesRule(final String... files) {
        return RuleChain.outerRule(new CleanFiles(files)).around(this);
    }

    public RuleChain withCleanFoldersRule(final boolean before, final boolean after, final int maxTries, final String... folders) {
        return RuleChain.outerRule(new CleanFolders(before, after, maxTries, folders)).around(this);
    }

    public RuleChain withCleanFoldersRule(final String... folders) {
        return RuleChain.outerRule(new CleanFolders(folders)).around(this);
    }

}
