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

import java.util.EnumSet;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * In a Servlet 3.0 or newer environment, this initializer is responsible for starting up Log4j logging before anything
 * else happens in application initialization. For consistency across all containers, if the effective Servlet major
 * version of the application is less than 3.0, this initializer does nothing.
 */
public class Log4jServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(final Set<Class<?>> classes, final ServletContext servletContext) throws ServletException {
        if (servletContext.getMajorVersion() > 2 && servletContext.getEffectiveMajorVersion() > 2 &&
                !"true".equalsIgnoreCase(servletContext.getInitParameter(
                        Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED
                ))) {
            servletContext.log("Log4jServletContainerInitializer starting up Log4j in Servlet 3.0+ environment.");

            final FilterRegistration.Dynamic filter =
                    servletContext.addFilter("log4jServletFilter", Log4jServletFilter.class);
            if (filter == null) {
                servletContext.log("WARNING: In a Servlet 3.0+ application, you should not define a " +
                        "log4jServletFilter in web.xml. Log4j 2 normally does this for you automatically. Log4j 2 " +
                        "web auto-initialization has been canceled.");
                return;
            }

            final Log4jWebInitializer initializer = Log4jWebInitializerImpl.getLog4jWebInitializer(servletContext);
            initializer.initialize();
            initializer.setLoggerContext(); // the application is just now starting to start up

            servletContext.addListener(new Log4jServletContextListener());

            filter.setAsyncSupported(true); // supporting async when the user isn't using async has no downsides
            filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
        }
    }
}
