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
package org.apache.logging.log4j.core.web;

import javax.servlet.UnavailableException;

/**
 * Specifies an interface for initializing and deinitializing Log4j in a Java EE web application. The default and only
 * implementation is {@link Log4jWebInitializerImpl}. The initializer is based on an interface to improve testability.
 */
interface Log4jWebInitializer {
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
     * The attribute key for the {@link javax.servlet.ServletContext} attribute that the singleton initializer instance
     * is stored in.
     */
    String INITIALIZER_ATTRIBUTE = Log4jWebInitializer.class.getName() + ".INSTANCE";

    /**
     * Starts up Log4j in the web application. Calls {@link #setLoggerContext()} after initialization is complete.
     *
     * @throws UnavailableException if a JNDI config location is specified but no name is specified.
     */
    void initialize() throws UnavailableException;

    /**
     * Shuts down Log4j in the web application. Calls {@link #clearLoggerContext()} immediately before deinitialization
     * begins.
     */
    void deinitialize();

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
