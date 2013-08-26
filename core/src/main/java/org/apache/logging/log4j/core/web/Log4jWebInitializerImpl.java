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
final class Log4jWebInitializerImpl implements Log4jWebInitializer {
    private static final Object MUTEX = new Object();

    static {
        try {
            Class.forName("org.apache.logging.log4j.core.web.JNDIContextFilter");
            throw new IllegalStateException("You are using Log4j 2 in a web application with the old, extinct " +
                    "log4j-web artifact. This is not supported and could cause serious runtime problems. Please" +
                    "remove the log4j-web JAR file from your application.");
        } catch (final ClassNotFoundException ignore) {
            /* Good. They don't have the old log4j-web artifact loaded. */
        }
    }

    private final StrSubstitutor substitutor = new StrSubstitutor(new Interpolator());
    private final ServletContext servletContext;

    private String name;
    private NamedContextSelector selector;
    private LoggerContext loggerContext;

    private boolean initialized = false;
    private boolean deinitialized = false;

    private Log4jWebInitializerImpl(final ServletContext servletContext) {
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
            final String location = this.substitutor.replace(this.servletContext.getInitParameter(LOG4J_CONFIG_LOCATION));
            final boolean isJndi = "true".equals(this.servletContext.getInitParameter(IS_LOG4J_CONTEXT_SELECTOR_NAMED));

            if (isJndi) {
                this.initializeJndi(location);
            } else {
                this.initializeNonJndi(location);
            }
        }
    }

    private void initializeJndi(final String location) throws UnavailableException {
        URI configLocation = null;
        if (location != null) {
            try {
                configLocation = new URI(location);
            } catch (final Exception e) {
                this.servletContext.log("Unable to convert configuration location [" + location + "] to a URI!", e);
            }
        }

        if (this.name == null) {
            throw new UnavailableException("A log4jContextName context parameter is required");
        }

        LoggerContext loggerContext;
        final LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            final ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
            if (selector instanceof NamedContextSelector) {
                this.selector = (NamedContextSelector) selector;
                loggerContext = this.selector.locateContext(this.name, this.servletContext, configLocation);
                ContextAnchor.THREAD_CONTEXT.set(loggerContext);
                if (loggerContext.getStatus() == LoggerContext.Status.INITIALIZED) {
                    loggerContext.start();
                }
                ContextAnchor.THREAD_CONTEXT.remove();
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

    private void initializeNonJndi(final String location) {
        if (this.name == null) {
            this.name = this.servletContext.getServletContextName();
        }

        if (this.name == null && location == null) {
            this.servletContext.log("No Log4j context configuration provided. This is very unusual.");
            return;
        }

        this.loggerContext = Configurator.initialize(this.name, this.getClassLoader(), location, this.servletContext);
    }

    @Override
    public synchronized void deinitialize() {
        if (!this.initialized) {
            throw new IllegalStateException("Cannot deinitialize Log4jWebInitializer because it has not initialized.");
        }

        // only do this once
        if (!this.deinitialized) {
            this.deinitialized = true;

            if (this.loggerContext != null) {
                this.servletContext.log("Removing LoggerContext for [" + this.name + "].");
                if (this.selector != null) {
                    this.selector.removeContext(this.name);
                }
                this.loggerContext.stop();
                this.loggerContext.setExternalContext(null);
                this.loggerContext = null;
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
            // this may look odd, but the call below will throw NoSuchMethodError if user is on Servlet 2.5
            // we compile against 3.0 to support Log4jServletContainerInitializer, but we don't require 3.0
            return this.servletContext.getClassLoader();
        } catch (final Throwable ignore) {
            // otherwise, use this class's class loader
            return Log4jWebInitializerImpl.class.getClassLoader();
        }
    }

    /**
     * Get the current initializer from the {@link ServletContext}. If the initializer does not exist, create a new one
     * and add it to the {@link ServletContext}, then return that.
     *
     * @param servletContext The {@link ServletContext} for this web application
     * @return the initializer, never {@code null}.
     */
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
