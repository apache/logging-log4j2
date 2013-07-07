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

import java.net.URI;
import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.selector.NamedContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;

/**
 * This class initializes and deinitializes Log4j no matter how the initialization occurs.
 */
class Log4jWebInitializerImpl implements Log4jWebInitializer {
    private static final Object MUTEX = new Object();

    private final StrSubstitutor substitutor = new StrSubstitutor(new Interpolator());
    private final ServletContext servletContext;

    private String name;
    private NamedContextSelector selector;
    private LoggerContext loggerContext;

    private boolean initialized = false;
    private boolean deinitialized = false;

    private Log4jWebInitializerImpl(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public synchronized void initialize() throws UnavailableException {
        if (this.deinitialized) {
            throw new IllegalStateException("Cannot initialize Log4jWebInitializer after it was destroyed.");
        }

        // only do this once
        if (!this.initialized) {
            this.initialized = true;

            this.name = this.substitutor.replace(this.servletContext.getInitParameter(LOG4J_CONTEXT_NAME));
            String location = this.substitutor.replace(this.servletContext.getInitParameter(LOG4J_CONFIG_LOCATION));
            boolean isJndi = "true".equals(this.servletContext.getInitParameter(LOG4J_CONFIG_IS_JNDI));

            if (isJndi) {
                this.initializeJndi(location);
            } else {
                this.initializeNonJndi(location);
            }

            this.setLoggerContext(); // the application is just now starting to start up
        }
    }

    private void initializeJndi(String location) throws UnavailableException {
        URI configLocation = null;
        if (location != null) {
            try {
                configLocation = new URI(location);
            } catch (Exception e) {
                this.servletContext.log("Unable to convert configuration location [" + location + "] to a URI!", e);
            }
        }

        if (this.name == null) {
            throw new UnavailableException("A log4jContextName context parameter is required");
        }

        LoggerContext loggerContext;
        LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
            if (selector instanceof NamedContextSelector) {
                this.selector = (NamedContextSelector) selector;
                loggerContext = this.selector.locateContext(this.name, configLocation);
            } else {
                this.servletContext.log("Potential problem: Selector is not an instance of NamedContextSelector.");
                return;
            }
        } else {
            this.servletContext.log("Potential problem: Factory is not an instance of Log4jContextFactory.");
            return;
        }
        this.loggerContext = loggerContext;
        this.servletContext.log("Created logger context for [" + this.name + "] using [" +
                loggerContext.getClass().getClassLoader() + "].");
    }

    private void initializeNonJndi(String location) {
        if (this.name == null) {
            this.name = this.servletContext.getServletContextName();
        }

        if (this.name == null && location == null) {
            this.servletContext.log("No Log4j context configuration provided. This is very unusual.");
            return;
        }

        this.loggerContext = Configurator.initialize(this.name, getClassLoader(), location);
    }

    @Override
    public synchronized void deinitialize() {
        if (!this.initialized) {
            throw new IllegalStateException("Cannot deinitialize Log4jWebInitializer because it has not initialized.");
        }

        // only do this once
        if (!this.deinitialized) {
            this.deinitialized = true;

            this.clearLoggerContext(); // the application is finished shutting down now

            if (this.loggerContext != null) {
                this.servletContext.log("Removing LoggerContext for [" + this.name + "].");
                if (this.selector != null) {
                    this.selector.removeContext(this.name);
                }
                this.loggerContext.stop();
            }
        }
    }

    @Override
    public void setLoggerContext() {
        if (this.loggerContext != null) {
            ContextAnchor.THREAD_CONTEXT.set(this.loggerContext);
        }
    }

    @Override
    public void clearLoggerContext() {
        ContextAnchor.THREAD_CONTEXT.remove();
    }

    private ClassLoader getClassLoader() {
        try {
            // if container is Servlet 3.0, use its getClassLoader method
            return (ClassLoader) this.servletContext.getClass().getMethod("getClassLoader").invoke(this.servletContext);
        } catch (Exception ignore) {
            // otherwise, use this class's class loader
            return Log4jWebInitializerImpl.class.getClassLoader();
        }
    }

    static Log4jWebInitializer getLog4jWebInitializer(final ServletContext servletContext) {
        synchronized (MUTEX) {
            Log4jWebInitializer initializer = (Log4jWebInitializer) servletContext.getAttribute(INITIALIZER_ATTRIBUTE);
            if (initializer == null) {
                initializer = new Log4jWebInitializerImpl(servletContext);
                servletContext.setAttribute(INITIALIZER_ATTRIBUTE, initializer);
            }
            return initializer;
        }
    }
}
