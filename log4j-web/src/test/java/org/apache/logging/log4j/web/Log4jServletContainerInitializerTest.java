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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Log4jServletContainerInitializerTest {
    @Mock
    private ServletContext servletContext;
    @Mock
    private Log4jWebLifeCycle initializer;
    @Captor
    private ArgumentCaptor<Class<? extends Filter>> filterCaptor;
    @Captor
    private ArgumentCaptor<EventListener> listenerCaptor;

    private Log4jServletContainerInitializer containerInitializer;

    @Before
    public void setUp() {
        this.containerInitializer = new Log4jServletContainerInitializer();
    }

    @Test
    public void testOnStartupWithServletVersion2_x() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(2);

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion2_x() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(2);

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion3_xDisabledTrue() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(3);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))).willReturn(
            "true");

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion3_xDisabledTRUE() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(3);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))).willReturn(
            "TRUE");

        this.containerInitializer.onStartup(null, this.servletContext);
    }

    @Test
    public void testOnStartupWithServletVersion3_xEffectiveVersion3_x() throws Exception {
        final FilterRegistration.Dynamic registration = mock(FilterRegistration.Dynamic.class);
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(3);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))).willReturn(
            null);
        given(servletContext.addFilter(eq("log4jServletFilter"), filterCaptor.capture())).willReturn(registration);
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(initializer);

        containerInitializer.onStartup(null, servletContext);

        then(initializer).should().start();
        then(initializer).should().setLoggerContext();
        then(servletContext).should().addListener(listenerCaptor.capture());
        then(registration).should().setAsyncSupported(eq(true));
        then(registration).should().addMappingForUrlPatterns(eq(EnumSet.allOf(DispatcherType.class)), eq(false), eq("/*"));

        assertNotNull("The listener should not be null.", listenerCaptor.getValue());
        assertSame("The listener is not correct.", Log4jServletContextListener.class,
            listenerCaptor.getValue().getClass());

        assertNotNull("The filter should not be null.", filterCaptor.getValue());
        assertSame("The filter is not correct.", Log4jServletFilter.class, filterCaptor.getValue());
    }

    @Test
    public void testOnStartupCanceledDueToPreExistingFilter() throws Exception {
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(3);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))).willReturn(
            "false");
        given(servletContext.addFilter(eq("log4jServletFilter"), filterCaptor.capture())).willReturn(null);

        this.containerInitializer.onStartup(null, this.servletContext);

        assertNotNull("The filter should not be null.", filterCaptor.getValue());
        assertSame("The filter is not correct.", Log4jServletFilter.class, filterCaptor.getValue());
    }

    @Test
    public void testOnStartupFailedDueToInitializerFailure() throws Exception {
        final FilterRegistration.Dynamic registration = mock(FilterRegistration.Dynamic.class);
        final IllegalStateException exception = new IllegalStateException(Strings.EMPTY);
        given(servletContext.getMajorVersion()).willReturn(3);
        given(servletContext.getEffectiveMajorVersion()).willReturn(3);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_INITIALIZATION_DISABLED))).willReturn(
            "balderdash");
        given(servletContext.addFilter(eq("log4jServletFilter"), filterCaptor.capture())).willReturn(registration);
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(initializer);
        willThrow(exception).given(initializer).start();

        try {
            this.containerInitializer.onStartup(null, this.servletContext);
            fail("Expected the exception thrown by the initializer; got no exception.");
        } catch (final IllegalStateException e) {
            assertSame("The exception is not correct.", exception, e);
        }

        then(initializer).should().start();
        assertNotNull("The filter should not be null.", filterCaptor.getValue());
        assertSame("The filter is not correct.", Log4jServletFilter.class, filterCaptor.getValue());
    }
}
