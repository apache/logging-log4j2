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
package org.apache.logging.log4j.core.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class Log4jContextListenerTest {

    private static final String CONFIG_NAME = "ConfigTest";

    @Test
    public void testFromFile() throws Exception {
        final MockServletContext context = new MockServletContext();
        context.setInitParameter(Log4jContextListener.LOG4J_CONTEXT_NAME, "Test1");
        context.setInitParameter(Log4jContextListener.LOG4J_CONFIG, "target/test-classes/log4j2-config.xml");
        final Log4jContextListener listener = new Log4jContextListener();
        final ServletContextEvent event = new ServletContextEvent(context);
        listener.contextInitialized(event);
        final Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }

    @Test
    public void testFromClassPath() throws Exception {
        final MockServletContext context = new MockServletContext();
        context.setInitParameter(Log4jContextListener.LOG4J_CONTEXT_NAME, "Test1");
        context.setInitParameter(Log4jContextListener.LOG4J_CONFIG, "log4j2-config.xml");
        final Log4jContextListener listener = new Log4jContextListener();
        final ServletContextEvent event = new ServletContextEvent(context);
        listener.contextInitialized(event);
        final Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }

    @Test
    public void testByName() throws Exception {
        final MockServletContext context = new MockServletContext();
        context.setInitParameter(Log4jContextListener.LOG4J_CONTEXT_NAME, "-config");
        final Log4jContextListener listener = new Log4jContextListener();
        final ServletContextEvent event = new ServletContextEvent(context);
        listener.contextInitialized(event);
        final Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }


    private class MockServletContext implements ServletContext {
        private String name;

        private final Hashtable<String, String> params = new Hashtable<String, String>();

        private final Hashtable<String, Object> attrs = new Hashtable<String, Object>();


        public ServletContext getContext(final String s) {
            return null;
        }

        public int getMajorVersion() {
            return 0;
        }

        public int getMinorVersion() {
            return 0;
        }

        public String getMimeType(final String s) {
            return null;
        }

        public Set getResourcePaths(final String s) {
            return null;
        }

        public URL getResource(final String s) throws MalformedURLException {
            return null;
        }

        public InputStream getResourceAsStream(final String s) {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(final String s) {
            return null;
        }

        public RequestDispatcher getNamedDispatcher(final String s) {
            return null;
        }

        public Servlet getServlet(final String s) throws ServletException {
            return null;
        }

        public Enumeration getServlets() {
            return null;
        }

        public Enumeration getServletNames() {
            return null;
        }

        public void log(final String s) {
            System.out.println(s);
        }

        public void log(final Exception e, final String s) {
            System.out.println(s);
            e.printStackTrace();
        }

        public void log(final String s, final Throwable throwable) {
            System.out.println(s);
            throwable.printStackTrace();
        }

        public String getRealPath(final String s) {
            return null;
        }

        public String getServerInfo() {
            return "Mock";
        }

        public void setInitParameter(final String key, final String value) {
            params.put(key, value);
        }

        public String getInitParameter(final String s) {
            return params.get(s);
        }

        public Enumeration getInitParameterNames() {
            return params.keys();
        }

        public Object getAttribute(final String s) {
            return attrs.get(s);
        }

        public Enumeration getAttributeNames() {
            return attrs.keys();
        }

        public void setAttribute(final String s, final Object o) {
            attrs.put(s, o);
        }

        public void removeAttribute(final String s) {
            attrs.remove(s);
        }

        public String getServletContextName() {
            return null;
        }
    }
}
