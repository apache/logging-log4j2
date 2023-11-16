/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.web;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * In environments older than Servlet 3.0, this initializer is responsible for starting up Log4j logging before anything
 * else happens in application initialization. In all environments, this shuts down Log4j after the application shuts
 * down.
 */
public class Log4jServletContextListener implements ServletContextListener {

    static final String START_COUNT_ATTR = Log4jServletContextListener.class.getName() + ".START_COUNT";

    private static final int DEFAULT_STOP_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_STOP_TIMEOUT_TIMEUNIT = TimeUnit.SECONDS;

    private static final String KEY_STOP_TIMEOUT = "log4j.stop.timeout";
    private static final String KEY_STOP_TIMEOUT_TIMEUNIT = "log4j.stop.timeout.timeunit";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    private int getAndIncrementCount() {
        Integer count = (Integer) servletContext.getAttribute(START_COUNT_ATTR);
        if (count == null) {
            count = 0;
        }
        servletContext.setAttribute(START_COUNT_ATTR, count + 1);
        return count;
    }

    private int decrementAndGetCount() {
        Integer count = (Integer) servletContext.getAttribute(START_COUNT_ATTR);
        if (count == null) {
            LOGGER.warn(
                    "{} received a 'contextDestroyed' message without a corresponding 'contextInitialized' message.",
                    getClass().getName());
            count = 1;
        }
        servletContext.setAttribute(START_COUNT_ATTR, --count);
        return count;
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        this.servletContext = event.getServletContext();
        if ("true".equalsIgnoreCase(servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_SHUTDOWN_DISABLED))) {
            throw new IllegalStateException("Do not use " + getClass().getSimpleName() + " when "
                    + Log4jWebSupport.IS_LOG4J_AUTO_SHUTDOWN_DISABLED + " is true. Please use "
                    + Log4jShutdownOnContextDestroyedListener.class.getSimpleName() + " instead of "
                    + getClass().getSimpleName() + ".");
        }

        this.initializer = WebLoggerContextUtils.getWebLifeCycle(this.servletContext);
        if (getAndIncrementCount() != 0) {
            LOGGER.debug(
                    "Skipping Log4j context initialization, since {} is registered multiple times.",
                    getClass().getSimpleName());
            return;
        }
        LOGGER.info("{} triggered a Log4j context initialization.", getClass().getSimpleName());
        try {
            this.initializer.start();
            this.initializer.setLoggerContext(); // the application is just now starting to start up
        } catch (final IllegalStateException e) {
            throw new IllegalStateException("Failed to initialize Log4j properly.", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (this.servletContext == null || this.initializer == null) {
            LOGGER.warn("Servlet context destroyed before it was initialized.");
            return;
        }

        if (decrementAndGetCount() != 0) {
            LOGGER.debug(
                    "Skipping Log4j context shutdown, since {} is registered multiple times.",
                    getClass().getSimpleName());
            return;
        }
        LOGGER.info("{} triggered a Log4j context shutdown.", getClass().getSimpleName());
        try {
            this.initializer.clearLoggerContext(); // the application is finished
            // shutting down now
            if (initializer instanceof LifeCycle2) {
                final String stopTimeoutStr = servletContext.getInitParameter(KEY_STOP_TIMEOUT);
                final long stopTimeout =
                        Strings.isEmpty(stopTimeoutStr) ? DEFAULT_STOP_TIMEOUT : Long.parseLong(stopTimeoutStr);
                final String timeoutTimeUnitStr = servletContext.getInitParameter(KEY_STOP_TIMEOUT_TIMEUNIT);
                final TimeUnit timeoutTimeUnit = Strings.isEmpty(timeoutTimeUnitStr)
                        ? DEFAULT_STOP_TIMEOUT_TIMEUNIT
                        : TimeUnit.valueOf(toRootUpperCase(timeoutTimeUnitStr));
                ((LifeCycle2) this.initializer).stop(stopTimeout, timeoutTimeUnit);
            } else {
                this.initializer.stop();
            }
        } catch (final IllegalStateException e) {
            throw new IllegalStateException("Failed to shutdown Log4j properly.", e);
        }
    }
}
