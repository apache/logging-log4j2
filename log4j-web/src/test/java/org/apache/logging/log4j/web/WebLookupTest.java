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

import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;

import static org.junit.Assert.*;

public class WebLookupTest {

    @Test
    public void testLookup() throws Exception {
        ContextAnchor.THREAD_CONTEXT.remove();
        final ServletContext servletContext = new MockServletContext();
        servletContext.setAttribute("TestAttr", "AttrValue");
        servletContext.setInitParameter("TestParam", "ParamValue");
        servletContext.setAttribute("Name1", "Ben");
        servletContext.setInitParameter("Name2", "Jerry");
        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
        try {
            initializer.start();
            initializer.setLoggerContext();
            final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
            assertNotNull("No LoggerContext", ctx);
            assertNotNull("No ServletContext", ctx.getExternalContext());
            final Configuration config = ctx.getConfiguration();
            assertNotNull("No Configuration", config);
            final StrSubstitutor substitutor = config.getStrSubstitutor();
            assertNotNull("No Interpolator", substitutor);
            String value = substitutor.replace("${web:initParam.TestParam}");
            assertNotNull("No value for TestParam", value);
            assertEquals("Incorrect value for TestParam: " + value, "ParamValue", value);
            value = substitutor.replace("${web:attr.TestAttr}");
            assertNotNull("No value for TestAttr", value);
            assertEquals("Incorrect value for TestAttr: " + value, "AttrValue", value);
            value = substitutor.replace("${web:Name1}");
            assertNotNull("No value for Name1", value);
            assertEquals("Incorrect value for Name1: " + value, "Ben", value);
            value = substitutor.replace("${web:Name2}");
            assertNotNull("No value for Name2", value);
            assertEquals("Incorrect value for Name2: " + value, "Jerry", value);
        } catch (final IllegalStateException e) {
            fail("Failed to initialize Log4j properly." + e.getMessage());
        }
        initializer.stop();
        ContextAnchor.THREAD_CONTEXT.remove();
    }

    @Test
    public void testLookup2() throws Exception {
        ContextAnchor.THREAD_CONTEXT.remove();
        final ServletContext servletContext = new MockServletContext();
        servletContext.setAttribute("TestAttr", "AttrValue");
        servletContext.setInitParameter("myapp.logdir", "target");
        servletContext.setAttribute("Name1", "Ben");
        servletContext.setInitParameter("Name2", "Jerry");
        servletContext.setInitParameter("log4jConfiguration", "WEB-INF/classes/log4j-webvar.xml");
        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(servletContext);
        initializer.start();
        initializer.setLoggerContext();
        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("No LoggerContext", ctx);
        assertNotNull("No ServletContext", ctx.getExternalContext());
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No Configuration", config);
        final Map<String, Appender> appenders = config.getAppenders();
        for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
            if (entry.getKey().equals("file")) {
                final FileAppender fa = (FileAppender) entry.getValue();
                assertEquals("target/myapp.log", fa.getFileName());
            }
        }
        initializer.stop();
        ContextAnchor.THREAD_CONTEXT.remove();
    }

}
