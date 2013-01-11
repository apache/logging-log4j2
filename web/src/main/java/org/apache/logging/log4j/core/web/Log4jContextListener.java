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

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.LoggerContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Method;

/**
 * Saves the LoggerContext into the ServletContext as an attribute.
 */
public class Log4jContextListener implements ServletContextListener {

    /**
     * The name of the attribute to use to store the LoggerContext into the ServletContext.
     */
    public static final String LOG4J_CONTEXT_ATTRIBUTE = "Log4JContext";

    /**
     * The location of the configuration.
     */
    public static final String LOG4J_CONFIG = "log4jConfiguration";

    /**
     * The name of the LoggerContext.
     */
    public static final String LOG4J_CONTEXT_NAME = "log4jContextName";

    /**
     * Initialize Logging for the web application.
     * @param event The ServletContextEvent.
     */
    public void contextInitialized(final ServletContextEvent event) {
        final ServletContext context = event.getServletContext();
        final String locn = context.getInitParameter(LOG4J_CONFIG);
        String name = context.getInitParameter(LOG4J_CONTEXT_NAME);
        if (name == null) {
            name = context.getServletContextName();
        }
        if (name == null && locn == null) {
            context.log("No Log4j context configuration provided");
            return;
        }
        context.setAttribute(LOG4J_CONTEXT_ATTRIBUTE, Configurator.initialize(name, getClassLoader(context), locn));
    }

    /**
     * Shutdown logging for the web application.
     * @param event The ServletContextEvent.
     */
    public void contextDestroyed(final ServletContextEvent event) {
        final LoggerContext ctx = (LoggerContext) event.getServletContext().getAttribute(LOG4J_CONTEXT_ATTRIBUTE);
        Configurator.shutdown(ctx);
    }

    private ClassLoader getClassLoader(final ServletContext context) {
        final Method[] methods = context.getClass().getMethods();
        Method getClassLoader = null;
        for (final Method method : methods) {
            if (method.getName().equals("getClassLoader")) {
                getClassLoader = method;
                break;
            }
        }

        if (getClassLoader != null) {
            try {
                return (ClassLoader) getClassLoader.invoke(context, null);
            } catch (final Exception ex) {
                // Ignore the exception
            }
        }

        return Log4jContextListener.class.getClassLoader();
    }
}
