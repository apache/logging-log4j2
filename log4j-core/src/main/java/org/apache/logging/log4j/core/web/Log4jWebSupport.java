package org.apache.logging.log4j.core.web;

import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Specifies an interface for setting and clearing a thread-bound {@link LoggerContext} in a Java EE web application.
 * Also defines constants for context parameter and attribute names. In most cases you will never need to use this
 * directly because the Log4j filter handles this task automatically. However, in async operations you should wrap
 * code that executes in separate threads with {@link #setLoggerContext} and {@link #clearLoggerContext}.<br>
 * <br>
 * You can obtain the instance of this for your web application by retrieving the {@link javax.servlet.ServletContext}
 * attribute named {@code org.apache.logging.log4j.core.web.Log4jWebSupport.INSTANCE}. If needed, you can also obtain
 * the {@link LoggerContext} instance for your web application by retrieving the {@code ServletContext} attribute named
 * {@code org.apache.logging.log4j.spi.LoggerContext.INSTANCE}.
 */
public interface Log4jWebSupport {
    /**
     * The {@link javax.servlet.ServletContext} context-param name for the name of the
     * {@link org.apache.logging.log4j.core.LoggerContext}.
     */
    String LOG4J_CONTEXT_NAME = "log4jContextName";

    /**
     * The {@link javax.servlet.ServletContext} context-param name for the location of the configuration.
     */
    String LOG4J_CONFIG_LOCATION = "log4jConfiguration";

    /**
     * The {@link javax.servlet.ServletContext} context-param name for the JNDI flag.
     */
    String IS_LOG4J_CONTEXT_SELECTOR_NAMED = "isLog4jContextSelectorNamed";

    /**
     * The attribute key for the {@link javax.servlet.ServletContext} attribute that the singleton support instance
     * is stored in.
     */
    String SUPPORT_ATTRIBUTE = Log4jWebSupport.class.getName() + ".INSTANCE";

    /**
     * The attribute key for the {@link javax.servlet.ServletContext} attribute that the {@link LoggerContext}
     * is stored in.
     */
    String CONTEXT_ATTRIBUTE = LoggerContext.class.getName() + ".INSTANCE";

    /**
     * Sets the logger context so that code executing afterwards can easily and quickly access loggers via
     * {@link org.apache.logging.log4j.LogManager#getLogger}.
     */
    void setLoggerContext();

    /**
     * Clears the logger context set up in {@link #setLoggerContext()}.
     */
    void clearLoggerContext();
}
