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
package org.apache.logging.log4j.core.impl;

import java.net.URI;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.helpers.Constants;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Factory to locate a ContextSelector and then load a LoggerContext.
 */
public class Log4jContextFactory implements LoggerContextFactory {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private ContextSelector selector;

    /**
     * Constructor that initializes the ContextSelector.
     */
    public Log4jContextFactory() {
        final String sel = PropertiesUtil.getProperties().getStringProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        if (sel != null) {
            try {
                final Class<?> clazz = Loader.loadClass(sel);
                if (clazz != null && ContextSelector.class.isAssignableFrom(clazz)) {
                    selector = (ContextSelector) clazz.newInstance();
                }
            } catch (final Exception ex) {
                LOGGER.error("Unable to create context " + sel, ex);
            }
        }
        if (selector == null) {
            selector = new ClassLoaderContextSelector();
        }
        try {
            Server.registerMBeans(selector);
        } catch (final Exception ex) {
            LOGGER.error("Could not start JMX", ex);
        }
    }

    /**
     * Returns the ContextSelector.
     * @return The ContextSelector.
     */
    public ContextSelector getSelector() {
        return selector;
    }

    /**
     * Load the LoggerContext using the ContextSelector.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @return The LoggerContext.
     */
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext);
        if (ctx.getStatus() == LoggerContext.Status.INITIALIZED) {
            ctx.start();
        }
        return ctx;
    }

    /**
     * Load the LoggerContext using the ContextSelector.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param configLocation The location of the configuration for the LoggerContext.
     * @return The LoggerContext.
     */
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final boolean currentContext,
            final URI configLocation) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext, configLocation);
        if (ctx.getStatus() == LoggerContext.Status.INITIALIZED) {
            ctx.start();
        }
        return ctx;
    }


    /**
     * Removes knowledge of a LoggerContext.
     *
     * @param context The context to remove.
     */
    @Override
    public void removeContext(final org.apache.logging.log4j.spi.LoggerContext context) {
        if (context instanceof LoggerContext) {
            selector.removeContext((LoggerContext) context);
        }
    }
}
