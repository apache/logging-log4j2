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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;

/**
 * Convenience methods for retrieving the {@link org.apache.logging.log4j.core.LoggerContext} associated with a
 * particular ServletContext. These methods are most particularly useful for asynchronous servlets where the
 * Thread Context ClassLoader (TCCL) is potentially different from the TCCL used by the
 * Servlet container that bootstrapped Log4j.
 *
 * @since 2.0.1
 */
public final class WebLoggerContextUtils {
    private WebLoggerContextUtils() {
    }

    private static final Lock WEB_SUPPORT_LOOKUP = new ReentrantLock();

    /**
     * Finds the main {@link org.apache.logging.log4j.core.LoggerContext} configured for the given ServletContext.
     *
     * @param servletContext the ServletContext to locate a LoggerContext for
     * @return the LoggerContext for the given ServletContext
     * @since 2.0.1
     */
    public static LoggerContext getWebLoggerContext(final ServletContext servletContext) {
        return (LoggerContext) servletContext.getAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
    }

    /**
     * Finds the main {@link org.apache.logging.log4j.core.LoggerContext} configured for the given ServletContext.
     *
     * @param servletContext the ServletContext to locate a LoggerContext for
     * @return the LoggerContext for the given ServletContext or {@code null} if none was set
     * @throws java.lang.IllegalStateException if no LoggerContext could be found on the given ServletContext
     * @since 2.0.1
     */
    public static LoggerContext getRequiredWebLoggerContext(final ServletContext servletContext) {
        final LoggerContext loggerContext = getWebLoggerContext(servletContext);
        if (loggerContext == null) {
            throw new IllegalStateException(
                "No LoggerContext found in ServletContext attribute " + Log4jWebSupport.CONTEXT_ATTRIBUTE);
        }
        return loggerContext;
    }

    /**
     * Finds or initializes the {@link org.apache.logging.log4j.web.Log4jWebLifeCycle} singleton for the given
     * ServletContext.
     *
     * @param servletContext the ServletContext to get the Log4jWebLifeCycle for
     * @return the Log4jWebLifeCycle for the given ServletContext
     * @since 2.0.1
     */
    public static Log4jWebLifeCycle getWebLifeCycle(final ServletContext servletContext) {
        WEB_SUPPORT_LOOKUP.lock();
        try {
            Log4jWebLifeCycle webLifeCycle = (Log4jWebLifeCycle) servletContext.getAttribute(
                Log4jWebSupport.SUPPORT_ATTRIBUTE);
            if (webLifeCycle == null) {
                webLifeCycle = Log4jWebInitializerImpl.initialize(servletContext);
            }
            return webLifeCycle;
        } finally {
            WEB_SUPPORT_LOOKUP.unlock();
        }
    }

    /**
     * Wraps a Runnable instance by setting its thread context {@link org.apache.logging.log4j.core.LoggerContext}
     * before execution and clearing it after execution.
     *
     * @param servletContext the ServletContext to locate a LoggerContext for
     * @param runnable       the Runnable to wrap execution for
     * @return a wrapped Runnable
     * @since 2.0.1
     */
    public static Runnable wrapExecutionContext(final ServletContext servletContext, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                final Log4jWebSupport webSupport = getWebLifeCycle(servletContext);
                webSupport.setLoggerContext();
                try {
                    runnable.run();
                } finally {
                    webSupport.clearLoggerContext();
                }
            }
        };
    }

    /**
     * Gets the current {@link ServletContext} if it has already been assigned to a LoggerContext's external context.
     *
     * @return the current ServletContext attached to a LoggerContext or {@code null} if none could be found
     * @since 2.1
     */
    public static ServletContext getServletContext() {
        org.apache.logging.log4j.spi.LoggerContext lc = ContextAnchor.THREAD_CONTEXT.get();
        if (lc == null) {
            lc = LogManager.getContext(false);
        }
        return lc == null ? null :
            lc.getExternalContext() instanceof ServletContext ? (ServletContext) lc.getExternalContext() : null;
    }
}
