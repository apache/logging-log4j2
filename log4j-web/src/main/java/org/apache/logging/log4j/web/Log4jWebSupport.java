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
package org.apache.logging.log4j.web;

import org.apache.logging.log4j.spi.LoggerContext;

/**
 * Specifies an interface for setting and clearing a thread-bound {@link LoggerContext} in a Java EE web application.
 * Also defines constants for context parameter and attribute names. In most cases you will never need to use this
 * directly because the Log4j filter handles this task automatically. However, in async operations you should wrap
 * code that executes in separate threads with {@link #setLoggerContext} and {@link #clearLoggerContext}.
 *
 * <p>
 * You can obtain the instance of this for your web application by retrieving the {@link javax.servlet.ServletContext}
 * attribute named {@code org.apache.logging.log4j.core.web.Log4jWebSupport.INSTANCE}. If needed, you can also obtain
 * the {@link LoggerContext} instance for your web application by retrieving the {@code ServletContext} attribute named
 * {@code org.apache.logging.log4j.spi.LoggerContext.INSTANCE}.
 * </p>
 */
public interface Log4jWebSupport {
    /**
     * The {@link javax.servlet.ServletContext} parameter name for the name of the
     * {@link org.apache.logging.log4j.core.LoggerContext}.
     */
    String LOG4J_CONTEXT_NAME = "log4jContextName";

    /**
     * The {@link javax.servlet.ServletContext} parameter name for the location of the configuration.
     */
    String LOG4J_CONFIG_LOCATION = "log4jConfiguration";

    /**
     * The {@link javax.servlet.ServletContext} parameter name for the JNDI flag.
     */
    String IS_LOG4J_CONTEXT_SELECTOR_NAMED = "isLog4jContextSelectorNamed";

    /**
     * The {@link javax.servlet.ServletContext} parameter name for the flag that disables Log4j's auto-initialization
     * in Servlet 3.0+ web applications. Set a context parameter with this name to "true" to disable
     * auto-initialization.
     */
    String IS_LOG4J_AUTO_INITIALIZATION_DISABLED = "isLog4jAutoInitializationDisabled";

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
     * Clears the logger context set up in {@link #setLoggerContext}.
     */
    void clearLoggerContext();

    /**
     * Sets the logger context by calling {@link #setLoggerContext}, executes the runnable argument, then clears the
     * logger context by calling {@link #clearLoggerContext}.
     *
     * @param runnable The runnable to execute wrapped with a configured logger context
     */
    void wrapExecution(Runnable runnable);
}
