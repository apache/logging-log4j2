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

import java.util.Locale;
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

	private static final int DEFAULT_STOP_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_STOP_TIMEOUT_TIMEUNIT = TimeUnit.SECONDS;

	private static final String KEY_STOP_TIMEOUT = "log4j.stop.timeout";
	private static final String KEY_STOP_TIMEOUT_TIMEUNIT = "log4j.stop.timeout.timeunit";

	private static final Logger LOGGER = StatusLogger.getLogger();

    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        this.servletContext = event.getServletContext();
        LOGGER.debug("Log4jServletContextListener ensuring that Log4j starts up properly.");

        this.initializer = WebLoggerContextUtils.getWebLifeCycle(this.servletContext);
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
			LOGGER.warn("Context destroyed before it was initialized.");
			return;
		}
		LOGGER.debug("Log4jServletContextListener ensuring that Log4j shuts down properly.");

		this.initializer.clearLoggerContext(); // the application is finished
		// shutting down now
		if (initializer instanceof LifeCycle2) {
			final String stopTimeoutStr = servletContext.getInitParameter(KEY_STOP_TIMEOUT);
			final long stopTimeout = Strings.isEmpty(stopTimeoutStr) ? DEFAULT_STOP_TIMEOUT
					: Long.parseLong(stopTimeoutStr);
			final String timeoutTimeUnitStr = servletContext.getInitParameter(KEY_STOP_TIMEOUT_TIMEUNIT);
			final TimeUnit timeoutTimeUnit = Strings.isEmpty(timeoutTimeUnitStr) ? DEFAULT_STOP_TIMEOUT_TIMEUNIT
					: TimeUnit.valueOf(timeoutTimeUnitStr.toUpperCase(Locale.ROOT));
			((LifeCycle2) this.initializer).stop(stopTimeout, timeoutTimeUnit);
		} else {
			this.initializer.stop();
		}
	}
}
