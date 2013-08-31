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

import java.util.EnumSet;
import java.util.EventListener;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class Log4jServletContainerInitializerTest {
    private ServletContext servletContext;
    private Log4jWebInitializer initializer;

    private Log4jServletContainerInitializer containerInitializer;

    @Before
    public void setUp() {
        this.servletContext = createStrictMock(ServletContext.class);
        this.initializer = createStrictMock(Log4jWebInitializer.class);

        this.containerInitializer = new Log4jServletContainerInitializer();
    }

    @After
    public void tearDown() {
        verify(this.servletContext, this.initializer);
    }

    @Test
    public void testOnStartupWithServletVersion2_x() throws Exception {
        expect(this.servletContext.getMajorVersion()).andReturn(2);

        replay(this.servletContext, this.initializer);

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_x() throws Exception {
        final FilterRegistration.Dynamic registration = createStrictMock(FilterRegistration.Dynamic.class);

        final Capture<EventListener> listenerCapture = new Capture<EventListener>();
        final Capture<Filter> filterCapture = new Capture<Filter>();

        expect(this.servletContext.getMajorVersion()).andReturn(3);
        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        expect(this.servletContext.getAttribute(Log4jWebInitializer.INITIALIZER_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.initialize();
        expectLastCall();
        this.initializer.setLoggerContext();
        expectLastCall();
        this.servletContext.addListener(capture(listenerCapture));
        expectLastCall();
        expect(this.servletContext.addFilter(eq("log4jServletFilter"), capture(filterCapture))).andReturn(registration);
        registration.addMappingForUrlPatterns(eq(EnumSet.allOf(DispatcherType.class)), eq(false), eq("/*"));
        expectLastCall();

        replay(this.servletContext, this.initializer, registration);

        this.containerInitializer.onStartup(null, this.servletContext);

        verify(registration);
    }

    @Test
    public void testOnStartupFailedDueToPreExistingFilter() throws Exception {
        final Capture<EventListener> listenerCapture = new Capture<EventListener>();
        final Capture<Filter> filterCapture = new Capture<Filter>();

        expect(this.servletContext.getMajorVersion()).andReturn(3);
        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        expect(this.servletContext.getAttribute(Log4jWebInitializer.INITIALIZER_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.initialize();
        expectLastCall();
        this.initializer.setLoggerContext();
        expectLastCall();
        this.servletContext.addListener(capture(listenerCapture));
        expectLastCall();
        expect(this.servletContext.addFilter(eq("log4jServletFilter"), capture(filterCapture))).andReturn(null);

        replay(this.servletContext, this.initializer);

        try {
            this.containerInitializer.onStartup(null, this.servletContext);
            fail("Expected an UnavailableException, got no exception.");
        } catch (final UnavailableException e) {
            assertEquals("The exception is not correct.",
                    "In a Servlet 3.0+ application, you must not define a log4jServletFilter in web.xml. Log4j 2 " +
                            "defines this for you automatically.",
                    e.getMessage());
        }
    }

    @Test
    public void testOnStartupFailedDueToInitializerFailure() throws Exception {
        final UnavailableException exception = new UnavailableException("");

        expect(this.servletContext.getMajorVersion()).andReturn(3);
        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        expect(this.servletContext.getAttribute(Log4jWebInitializer.INITIALIZER_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.initialize();
        expectLastCall().andThrow(exception);

        replay(this.servletContext, this.initializer);

        try {
            this.containerInitializer.onStartup(null, this.servletContext);
            fail("Expected the exception thrown by the initializer; got no exception.");
        } catch (final UnavailableException e) {
            assertSame("The exception is not correct.", exception, e);
        }
    }
}
