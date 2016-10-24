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

import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.selector.NamedContextSelector;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.SetUtils;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * This class initializes and deinitializes Log4j no matter how the initialization occurs.
 */
final class Log4jWebInitializerImpl extends AbstractLifeCycle implements Log4jWebLifeCycle {

    private static final String WEB_INF = "/WEB-INF/";

    static {
        if (Loader.isClassAvailable("org.apache.logging.log4j.core.web.JNDIContextFilter")) {
            throw new IllegalStateException("You are using Log4j 2 in a web application with the old, extinct "
                    + "log4j-web artifact. This is not supported and could cause serious runtime problems. Please"
                    + "remove the log4j-web JAR file from your application.");
        }
    }

    private final Map<String, String> map = new ConcurrentHashMap<>();
    private final StrSubstitutor substitutor = new StrSubstitutor(new Interpolator(map));
    private final ServletContext servletContext;

    private String name;
    private NamedContextSelector namedContextSelector;
    private LoggerContext loggerContext;

    private Log4jWebInitializerImpl(final ServletContext servletContext) {
        this.servletContext = servletContext;
        this.map.put("hostName", NetUtils.getLocalHostname());
    }

    /**
     * Initializes the Log4jWebLifeCycle attribute of a ServletContext. Those who wish to obtain this object should use
     * the {@link org.apache.logging.log4j.web.WebLoggerContextUtils#getWebLifeCycle(javax.servlet.ServletContext)}
     * method instead.
     *
     * @param servletContext
     *        the ServletContext to initialize
     * @return a new Log4jWebLifeCycle
     * @since 2.0.1
     */
    protected static Log4jWebInitializerImpl initialize(final ServletContext servletContext) {
        final Log4jWebInitializerImpl initializer = new Log4jWebInitializerImpl(servletContext);
        servletContext.setAttribute(SUPPORT_ATTRIBUTE, initializer);
        return initializer;
    }

    @Override
    public synchronized void start() {
        if (this.isStopped() || this.isStopping()) {
            throw new IllegalStateException("Cannot start this Log4jWebInitializerImpl after it was stopped.");
        }

        // only do this once
        if (this.isInitialized()) {
            super.setStarting();

            this.name = this.substitutor.replace(this.servletContext.getInitParameter(LOG4J_CONTEXT_NAME));
            final String location = this.substitutor.replace(this.servletContext
                    .getInitParameter(LOG4J_CONFIG_LOCATION));
            final boolean isJndi = "true".equalsIgnoreCase(this.servletContext
                    .getInitParameter(IS_LOG4J_CONTEXT_SELECTOR_NAMED));

            if (isJndi) {
                this.initializeJndi(location);
            } else {
                this.initializeNonJndi(location);
            }
            if (this.loggerContext instanceof AsyncLoggerContext) {
                ((AsyncLoggerContext) this.loggerContext).setUseThreadLocals(false);
            }

            this.servletContext.setAttribute(CONTEXT_ATTRIBUTE, this.loggerContext);
            super.setStarted();
        }
    }

    private void initializeJndi(final String location) {
        final URI configLocation = getConfigURI(location);

        if (this.name == null) {
            throw new IllegalStateException("A log4jContextName context parameter is required");
        }

        LoggerContext context;
        final LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            final ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
            if (selector instanceof NamedContextSelector) {
                this.namedContextSelector = (NamedContextSelector) selector;
                context = this.namedContextSelector.locateContext(this.name, this.servletContext, configLocation);
                ContextAnchor.THREAD_CONTEXT.set(context);
                if (context.isInitialized()) {
                    context.start();
                }
                ContextAnchor.THREAD_CONTEXT.remove();
            } else {
                LOGGER.warn("Potential problem: Selector is not an instance of NamedContextSelector.");
                return;
            }
        } else {
            LOGGER.warn("Potential problem: LoggerContextFactory is not an instance of Log4jContextFactory.");
            return;
        }
        this.loggerContext = context;
        LOGGER.debug("Created logger context for [{}] using [{}].", this.name, context.getClass().getClassLoader());
    }

    private void initializeNonJndi(final String location) {
        if (this.name == null) {
            this.name = this.servletContext.getServletContextName();
            LOGGER.debug("Using the servlet context name \"{}\".", this.name);
        }
        if (this.name == null) {
            this.name = this.servletContext.getContextPath();
            LOGGER.debug("Using the servlet context context-path \"{}\".", this.name);
        }

        if (this.name == null && location == null) {
            LOGGER.error("No Log4j context configuration provided. This is very unusual.");
            this.name = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS").format(new Date());
        }

        final URI uri = getConfigURI(location);
        this.loggerContext = Configurator.initialize(this.name, this.getClassLoader(), uri, this.servletContext);
    }

    private URI getConfigURI(final String location) {
        try {
            String configLocation = location;
            if (configLocation == null) {
                final String[] paths = SetUtils.prefixSet(servletContext.getResourcePaths(WEB_INF), WEB_INF + "log4j2");
                LOGGER.debug("getConfigURI found resource paths {} in servletContext at [{}]", Arrays.toString(paths), WEB_INF);
                if (paths.length == 1) {
                    configLocation = paths[0];
                } else if (paths.length > 1) {
                    final String prefix = WEB_INF + "log4j2-" + this.name + ".";
                    boolean found = false;
                    for (final String str : paths) {
                        if (str.startsWith(prefix)) {
                            configLocation = str;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        configLocation = paths[0];
                    }
                }
            }
            if (configLocation != null) {
                final URL url = servletContext.getResource(configLocation);
                if (url != null) {
                    final URI uri = url.toURI();
                    LOGGER.debug("getConfigURI found resource [{}] in servletContext at [{}]", uri, configLocation);
                    return uri;
                }
            }
        } catch (final Exception ex) {
            // Just try passing the location.
        }
        if (location != null) {
            try {
                final URI correctedFilePathUri = NetUtils.toURI(location);
                LOGGER.debug("getConfigURI found [{}] in servletContext at [{}]", correctedFilePathUri, location);
                return correctedFilePathUri;
            } catch (final Exception e) {
                LOGGER.error("Unable to convert configuration location [{}] to a URI", location, e);
            }
        }
        return null;
    }

    @Override
    public synchronized boolean stop(final long timeout, final TimeUnit timeUnit) {
        if (!this.isStarted() && !this.isStopped()) {
            throw new IllegalStateException("Cannot stop this Log4jWebInitializer because it has not started.");
        }

        // only do this once
        if (this.isStarted()) {
            this.setStopping();
            if (this.loggerContext != null) {
                LOGGER.debug("Removing LoggerContext for [{}].", this.name);
                this.servletContext.removeAttribute(CONTEXT_ATTRIBUTE);
                if (this.namedContextSelector != null) {
                    this.namedContextSelector.removeContext(this.name);
                }
                this.loggerContext.stop(timeout, timeUnit);
                this.loggerContext.setExternalContext(null);
                this.loggerContext = null;
            }
            this.setStopped();
        }
        return super.stop(timeout, timeUnit);
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

    @Override
    public void wrapExecution(final Runnable runnable) {
        this.setLoggerContext();

        try {
            runnable.run();
        } finally {
            this.clearLoggerContext();
        }
    }

    private ClassLoader getClassLoader() {
        try {
            // if container is Servlet 3.0, use its getClassLoader method
            // this may look odd, but the call below will throw NoSuchMethodError if user is on Servlet 2.5
            // we compile against 3.0 to support Log4jServletContainerInitializer, but we don't require 3.0
            return this.servletContext.getClassLoader();
        } catch (final Throwable ignore) {
            // LOG4J2-248: use TCCL if possible
            return LoaderUtil.getThreadContextClassLoader();
        }
    }

}
