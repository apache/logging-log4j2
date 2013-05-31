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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertTrue("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + NullConfiguration.NULL_NAME + " but found " +
            config.getName(), NullConfiguration.NULL_NAME.equals(config.getName()));
    }


    @Test
    public void testFromProperty() throws Exception {
        System.setProperty("targetDir", "target/test-classes");
        final MockServletContext context = new MockServletContext();
        context.setInitParameter(Log4jContextListener.LOG4J_CONTEXT_NAME, "Test1");
        context.setInitParameter(Log4jContextListener.LOG4J_CONFIG, "${sys:targetDir}/log4j2-config.xml");
        final Log4jContextListener listener = new Log4jContextListener();
        final ServletContextEvent event = new ServletContextEvent(context);
        listener.contextInitialized(event);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertTrue("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + NullConfiguration.NULL_NAME + " but found " +
            config.getName(), NullConfiguration.NULL_NAME.equals(config.getName()));
        System.clearProperty("targetDir");
    }

    @Test
    public void testFromClassPath() throws Exception {
        final MockServletContext context = new MockServletContext();
        context.setInitParameter(Log4jContextListener.LOG4J_CONTEXT_NAME, "Test1");
        context.setInitParameter(Log4jContextListener.LOG4J_CONFIG, "log4j2-config.xml");
        final Log4jContextListener listener = new Log4jContextListener();
        final ServletContextEvent event = new ServletContextEvent(context);
        listener.contextInitialized(event);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertTrue("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + NullConfiguration.NULL_NAME + " but found " +
            config.getName(), NullConfiguration.NULL_NAME.equals(config.getName()));
    }

    @Test
    public void testByName() throws Exception {
        final MockServletContext context = new MockServletContext();
        context.setInitParameter(Log4jContextListener.LOG4J_CONTEXT_NAME, "-config");
        final Log4jContextListener listener = new Log4jContextListener();
        final ServletContextEvent event = new ServletContextEvent(context);
        listener.contextInitialized(event);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        final Map<String, Appender<?>> map = config.getAppenders();
        assertTrue("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        listener.contextDestroyed(event);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + NullConfiguration.NULL_NAME + " but found " +
            config.getName(), NullConfiguration.NULL_NAME.equals(config.getName()));
    }


    private class MockServletContext implements ServletContext {
        private final Hashtable<String, String> params = new Hashtable<String, String>();

        private final Hashtable<String, Object> attrs = new Hashtable<String, Object>();

        @Override
        public ServletContext getContext(final String s) {
            return null;
        }

        @Override
        public int getMajorVersion() {
            return 0;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public String getMimeType(final String s) {
            return null;
        }

        @Override
        public Set getResourcePaths(final String s) {
            return null;
        }

        @Override
        public URL getResource(final String s) throws MalformedURLException {
            return null;
        }

        @Override
        public InputStream getResourceAsStream(final String s) {
            return null;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(final String s) {
            return null;
        }

        @Override
        public RequestDispatcher getNamedDispatcher(final String s) {
            return null;
        }

        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Servlet getServlet(final String s) throws ServletException {
            return null;
        }

        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Enumeration getServlets() {
            return null;
        }

        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public Enumeration getServletNames() {
            return null;
        }

        @Override
        public void log(final String s) {
            System.out.println(s);
        }

        @Override
        @Deprecated
        @SuppressWarnings("deprecation")
        public void log(final Exception e, final String s) {
            System.out.println(s);
            e.printStackTrace();
        }

        @Override
        public void log(final String s, final Throwable throwable) {
            System.out.println(s);
            throwable.printStackTrace();
        }

        @Override
        public String getRealPath(final String s) {
            return null;
        }

        @Override
        public String getServerInfo() {
            return "Mock";
        }

        public void setInitParameter(final String key, final String value) {
            params.put(key, value);
        }

        @Override
        public String getInitParameter(final String s) {
            return params.get(s);
        }

        @Override
        public Enumeration getInitParameterNames() {
            return params.keys();
        }

        @Override
        public Object getAttribute(final String s) {
            return attrs.get(s);
        }

        @Override
        public Enumeration getAttributeNames() {
            return attrs.keys();
        }

        @Override
        public void setAttribute(final String s, final Object o) {
            attrs.put(s, o);
        }

        @Override
        public void removeAttribute(final String s) {
            attrs.remove(s);
        }

        @Override
        public String getServletContextName() {
            return null;
        }

        @Override
        public String getContextPath() {
            return null;
        }
    }
}
