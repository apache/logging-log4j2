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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * This is responsible for the following:
 * <ul>
 *     <li>Clearing the logger context when the application has finished starting up.</li>
 *     <li>Setting the logger context before processing a request and clearing it after processing a request.</li>
 *     <li>Setting the logger context when the application is starting to shut down.</li>
 * </ul>
 * This filter is a once-per-request filter. It is capable of filtering all the different types of requests
 * (standard, asynchronous, error, etc.) but will not apply processing if the filter matches multiple times on the same
 * logical request.
 */
public class Log4jServletFilter implements Filter {

    private static final Logger LOGGER = StatusLogger.getLogger();

    static final String ALREADY_FILTERED_ATTRIBUTE = Log4jServletFilter.class.getName() + ".FILTERED";

    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        this.servletContext = filterConfig.getServletContext();
        LOGGER.debug("Log4jServletFilter initialized.");

        this.initializer = WebLoggerContextUtils.getWebLifeCycle(this.servletContext);
        this.initializer.clearLoggerContext(); // the application is mostly finished starting up now
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (request.getAttribute(ALREADY_FILTERED_ATTRIBUTE) != null) {
            chain.doFilter(request, response);
        } else {
            request.setAttribute(ALREADY_FILTERED_ATTRIBUTE, Boolean.TRUE);

            try {
                this.initializer.setLoggerContext();

                chain.doFilter(request, response);
            } finally {
                this.initializer.clearLoggerContext();
            }
        }
    }

    @Override
    public void destroy() {
        if (this.servletContext == null || this.initializer == null) {
            throw new IllegalStateException("Filter destroyed before it was initialized.");
        }
        LOGGER.debug("Log4jServletFilter destroyed.");

        this.initializer.setLoggerContext(); // the application is just now starting to shut down
    }
}
