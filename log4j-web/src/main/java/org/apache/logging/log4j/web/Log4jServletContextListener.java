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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.UnavailableException;

/**
 * In environments older than Servlet 3.0, this initializer is responsible for starting up Log4j logging before anything
 * else happens in application initialization. In all environments, this shuts down Log4j after the application shuts
 * down.
 */
public class Log4jServletContextListener implements ServletContextListener {

    private ServletContext servletContext;
    private Log4jWebInitializer initializer;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        this.servletContext = event.getServletContext();
        this.servletContext.log("Log4jServletContextListener ensuring that Log4j starts up properly.");

        this.initializer = Log4jWebInitializerImpl.getLog4jWebInitializer(this.servletContext);
        try {
            this.initializer.initialize();
            this.initializer.setLoggerContext(); // the application is just now starting to start up
        } catch (final UnavailableException e) {
            throw new RuntimeException("Failed to initialize Log4j properly.", e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (this.servletContext == null || this.initializer == null) {
            throw new IllegalStateException("Context destroyed before it was initialized.");
        }
        this.servletContext.log("Log4jServletContextListener ensuring that Log4j shuts down properly.");

        this.initializer.clearLoggerContext(); // the application is finished shutting down now
        this.initializer.deinitialize();
    }
}
