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
import java.util.EventListener;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.util.Strings;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class Log4jServletContainerInitializerTest {
    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    private Log4jServletContainerInitializer containerInitializer;

    @Before
    public void setUp() {
        this.servletContext = createStrictMock(ServletContext.class);
        this.initializer = createStrictMock(Log4jWebLifeCycle.class);

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
    public void testOnStartupWithServletVersion3_xEffectiveVersion2_x() throws Exception {
        expect(this.servletContext.getMajorVersion()).andReturn(3);
        expect(this.servletContext.getEffectiveMajorVersion()).andReturn(2);

        replay(this.servletContext, this.initializer);

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion3_xDisabledTrue() throws Exception {
        expect(this.servletContext.getMajorVersion()).andReturn(3);
        expect(this.servletContext.getEffectiveMajorVersion()).andReturn(3);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))
                .andReturn("true");

        replay(this.servletContext, this.initializer);

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion3_xDisabledTRUE() throws Exception {
        expect(this.servletContext.getMajorVersion()).andReturn(3);
        expect(this.servletContext.getEffectiveMajorVersion()).andReturn(3);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))
                .andReturn("TRUE");

        replay(this.servletContext, this.initializer);

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion3_x() throws Exception {
        final FilterRegistration.Dynamic registration = createStrictMock(FilterRegistration.Dynamic.class);

        final Capture<EventListener> listenerCapture = new Capture<>();
        final Capture<Class<? extends Filter>> filterCapture = new Capture<>();

        expect(this.servletContext.getMajorVersion()).andReturn(3);
        expect(this.servletContext.getEffectiveMajorVersion()).andReturn(3);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))
                .andReturn(null);
        expect(this.servletContext.addFilter(eq("log4jServletFilter"), capture(filterCapture))).andReturn(registration);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.start();
        expectLastCall();
        this.initializer.setLoggerContext();
        expectLastCall();
        this.servletContext.addListener(capture(listenerCapture));
        expectLastCall();
        registration.setAsyncSupported(true);
        expectLastCall();
        registration.addMappingForUrlPatterns(eq(EnumSet.allOf(DispatcherType.class)), eq(false), eq("/*"));
        expectLastCall();

        replay(this.servletContext, this.initializer, registration);

        this.containerInitializer.onStartup(null, this.servletContext);

        assertNotNull("The listener should not be null.", listenerCapture.getValue());
        assertSame("The listener is not correct.", Log4jServletContextListener.class,
                listenerCapture.getValue().getClass());

        assertNotNull("The filter should not be null.", filterCapture.getValue());
        assertSame("The filter is not correct.", Log4jServletFilter.class, filterCapture.getValue());

        verify(registration);
    }

    @Test
    public void testOnStartupCanceledDueToPreExistingFilter() throws Exception {
        final Capture<Class<? extends Filter>> filterCapture = new Capture<>();

        expect(this.servletContext.getMajorVersion()).andReturn(3);
        expect(this.servletContext.getEffectiveMajorVersion()).andReturn(3);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))
                .andReturn("false");
        expect(this.servletContext.addFilter(eq("log4jServletFilter"), capture(filterCapture))).andReturn(null);

        replay(this.servletContext, this.initializer);

        this.containerInitializer.onStartup(null, this.servletContext);

        assertNotNull("The filter should not be null.", filterCapture.getValue());
        assertSame("The filter is not correct.", Log4jServletFilter.class, filterCapture.getValue());
    }

    @Test
    public void testOnStartupFailedDueToInitializerFailure() throws Exception {
        final FilterRegistration.Dynamic registration = createStrictMock(FilterRegistration.Dynamic.class);

        final Capture<Class<? extends Filter>> filterCapture = new Capture<>();
        final IllegalStateException exception = new IllegalStateException(Strings.EMPTY);

        expect(this.servletContext.getMajorVersion()).andReturn(3);
        expect(this.servletContext.getEffectiveMajorVersion()).andReturn(3);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))
                .andReturn("balderdash");
        expect(this.servletContext.addFilter(eq("log4jServletFilter"), capture(filterCapture))).andReturn(registration);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.start();
        expectLastCall().andThrow(exception);

        replay(this.servletContext, this.initializer, registration);

        try {
            this.containerInitializer.onStartup(null, this.servletContext);
            fail("Expected the exception thrown by the initializer; got no exception.");
        } catch (final IllegalStateException e) {
            assertSame("The exception is not correct.", exception, e);
        }

        assertNotNull("The filter should not be null.", filterCapture.getValue());
        assertSame("The filter is not correct.", Log4jServletFilter.class, filterCapture.getValue());

        verify(registration);
    }
}
