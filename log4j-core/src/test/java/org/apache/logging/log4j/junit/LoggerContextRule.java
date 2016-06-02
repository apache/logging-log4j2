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
package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.junit.Assert.*;

/**
 * JUnit {@link TestRule} for constructing a new LoggerContext using a specified configuration file.
 * If the system property {@code EBUG} is set (e.g., through the command line option {@code -DEBUG}), then the
 * StatusLogger will be set to the debug level. This allows for more debug messages as the StatusLogger will be in the
 * error level until a configuration file has been read and parsed into a tree of Nodes.
 */
public class LoggerContextRule implements TestRule {

    private static final String SYS_PROP_KEY_DISPLAY_NAME = "org.apache.logging.log4j.junit.LoggerContextRule#DisplayName";
    private static final String SYS_PROP_KEY_CLASS_NAME = "org.apache.logging.log4j.junit.LoggerContextRule#ClassName";
    private final String configLocation;
    private final Class<? extends ContextSelector> contextSelectorClass;

    private LoggerContext context;

    private String testClassName;

    /**
     * Constructs a new LoggerContextRule for a given configuration file.
     *
     * @param configLocation path to configuration file
     */
    public LoggerContextRule(final String configLocation) {
        this(configLocation, null);
    }

    /**
     * Constructs a new LoggerContextRule for a given configuration file and a custom {@link ContextSelector} class.
     *
     * @param configLocation       path to configuration file
     * @param contextSelectorClass custom ContextSelector class to use instead of default
     * @since 2.5
     */
    public LoggerContextRule(final String configLocation, final Class<? extends ContextSelector> contextSelectorClass) {
        this.configLocation = configLocation;
        this.contextSelectorClass = contextSelectorClass;
    }


    @Override
    public Statement apply(final Statement base, final Description description) {
        // Hack: Using -DEBUG as a JVM param sets a property called "EBUG"... 
        if (System.getProperties().containsKey("EBUG")) {
            StatusLogger.getLogger().setLevel(Level.DEBUG);
        }
        testClassName = description.getClassName();
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (contextSelectorClass != null) {
                    System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, contextSelectorClass.getName());
                }
                System.setProperty(SYS_PROP_KEY_CLASS_NAME, description.getClassName());
                System.setProperty(SYS_PROP_KEY_DISPLAY_NAME, description.getDisplayName());
                context = Configurator.initialize(
                    description.getDisplayName(),
                    description.getTestClass().getClassLoader(),
                    configLocation
                );
                try {
                    base.evaluate();
                } finally {
                    Configurator.shutdown(context);
                    StatusLogger.getLogger().reset();
                    System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
                    System.clearProperty(SYS_PROP_KEY_CLASS_NAME);
                    System.clearProperty(SYS_PROP_KEY_DISPLAY_NAME);
                }
            }
        };
    }

    /**
     * Gets a named Appender for this LoggerContext.
     *
     * @param name the name of the Appender to look up.
     * @return the named Appender or {@code null} if it wasn't defined in the configuration.
     */
    public Appender getAppender(final String name) {
        return getConfiguration().getAppenders().get(name);
    }

    /**
     * Gets a named Appender for this LoggerContext.
     *
     * @param <T>  The target Appender class
     * @param name the name of the Appender to look up.
     * @param cls  The target Appender class
     * @return the named Appender or {@code null} if it wasn't defined in the configuration.
     */
    public <T extends Appender> T getAppender(final String name, Class<T> cls) {
        return cls.cast(getConfiguration().getAppenders().get(name));
    }

    /**
     * Gets the associated Configuration for the configuration file this was constructed with.
     *
     * @return this LoggerContext's Configuration.
     */
    public Configuration getConfiguration() {
        return context.getConfiguration();
    }

    /**
     * Gets the current LoggerContext associated with this rule.
     *
     * @return the current LoggerContext.
     */
    public LoggerContext getContext() {
        return context;
    }

    /**
     * Gets a named ListAppender or throws an exception for this LoggerContext.
     *
     * @param name the name of the ListAppender to look up.
     * @return the named ListAppender.
     * @throws AssertionError if the named ListAppender doesn't exist or isn't a ListAppender.
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
        return context.getLogger(testClassName);
    }

    /**
     * Gets a named Logger in this LoggerContext.
     *
     * @param name the name of the Logger to look up or create.
     * @return the named Logger.
     */
    public Logger getLogger(final String name) {
        return context.getLogger(name);
    }

    /**
     * Gets a named Appender or throws an exception for this LoggerContext.
     *
     * @param name the name of the Appender to look up.
     * @return the named Appender.
     * @throws AssertionError if the Appender doesn't exist.
     */
    public Appender getRequiredAppender(final String name) {
        final Appender appender = getAppender(name);
        assertNotNull("Appender named " + name + " was null.", appender);
        return appender;
    }

    /**
     * Gets a named Appender or throws an exception for this LoggerContext.
     *
     * @param <T>  The target Appender class
     * @param name the name of the Appender to look up.
     * @param cls  The target Appender class
     * @return the named Appender.
     * @throws AssertionError if the Appender doesn't exist.
     */
    public <T extends Appender> T getRequiredAppender(final String name, Class<T> cls) {
        final T appender = getAppender(name, cls);
        assertNotNull("Appender named " + name + " was null.", appender);
        return appender;
    }

    /**
     * Gets the root logger.
     *
     * @return the root logger.
     */
    public Logger getRootLogger() {
        return context.getRootLogger();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LoggerContextRule [configLocation=");
        builder.append(configLocation);
        builder.append(", contextSelectorClass=");
        builder.append(contextSelectorClass);
        builder.append("]");
        return builder.toString();
    }
}
