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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import javax.servlet.ServletContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

/**
 *
 */
class ServletAppenderTest {

    private static final String CONFIG = "WEB-INF/classes/log4j-servlet.xml";

    @Test
    void testAppender() {
        ContextAnchor.THREAD_CONTEXT.remove();
        final ServletContext servletContext = new MockServletContext();
        servletContext.setAttribute("TestAttr", "AttrValue");
        servletContext.setInitParameter("TestParam", "ParamValue");
        servletContext.setAttribute("Name1", "Ben");
        servletContext.setInitParameter("Name2", "Jerry");
        servletContext.setInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION, CONFIG);
        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
        try {
            initializer.start();
            initializer.setLoggerContext();
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            assertNotNull(ctx, "No LoggerContext");
            assertNotNull(WebLoggerContextUtils.getServletContext(), "No ServletContext");
            final Configuration configuration = ctx.getConfiguration();
            assertNotNull(configuration, "No configuration");
            final Appender appender = configuration.getAppender("Servlet");
            assertNotNull(appender, "No ServletAppender");
            final Logger logger = LogManager.getLogger("Test");
            logger.info("This is a test");
            logger.error("This is a test 2", new IllegalStateException().fillInStackTrace());
        } catch (final IllegalStateException e) {
            fail("Failed to initialize Log4j properly." + e.getMessage());
        } finally {
            initializer.stop();
            ContextAnchor.THREAD_CONTEXT.remove();
        }
    }
}
