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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@RunWith(MockitoJUnitRunner.class)
public class Log4jWebInitializerImplTest {
    @Mock
    private ServletContext servletContext;
    @Captor
    private ArgumentCaptor<Log4jWebLifeCycle> initializerCaptor;
    @Captor
    private ArgumentCaptor<org.apache.logging.log4j.spi.LoggerContext> loggerContextCaptor;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Log4jWebInitializerImpl initializerImpl;

    @Before
    public void setUp() {
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(null);

        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(this.servletContext);

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.SUPPORT_ATTRIBUTE), initializerCaptor.capture());
        assertNotNull("The initializer should not be null.", initializer);
        assertSame("The capture is not correct.", initializer, initializerCaptor.getValue());
        assertTrue("The initializer is not correct.", initializer instanceof Log4jWebInitializerImpl);

        this.initializerImpl = (Log4jWebInitializerImpl) initializer;
    }

    @Test
    public void testDeinitializeBeforeInitialize() {
        expectedException.expect(IllegalStateException.class);
        this.initializerImpl.stop();
    }

    @Test
    public void testSetLoggerContextBeforeInitialize() {
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testClearLoggerContextBeforeInitialize() {
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeWithNoParametersThenSetLoggerContextThenDeinitialize() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld01");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull("The context attribute should not be null.", loggerContextCaptor.getValue());
        final org.apache.logging.log4j.spi.LoggerContext loggerContext = loggerContextCaptor.getValue();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("The context should not be null.", context);
        assertSame("The context is not correct.", loggerContext, context);

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeWithClassLoaderNoParametersThenSetLoggerContextThenDeinitialize() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("false");
        given(servletContext.getServletContextName()).willReturn("helloWorld02");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull("The context attribute should not be null.", loggerContextCaptor.getValue());
        final org.apache.logging.log4j.spi.LoggerContext loggerContext = loggerContextCaptor.getValue();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("The context should not be null.", context);
        assertSame("The context is not correct.", loggerContext, context);

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeIsIdempotent() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("nothing");
        given(servletContext.getServletContextName()).willReturn("helloWorld03");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull("The context attribute should not be null.", loggerContextCaptor.getValue());

        this.initializerImpl.start();
        this.initializerImpl.start();
        this.initializerImpl.start();
        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));
    }

    @Test
    public void testInitializeFailsAfterDeinitialize() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld04");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull("The context attribute should not be null.", loggerContextCaptor.getValue());

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        expectedException.expect(IllegalStateException.class);
        this.initializerImpl.start();
    }

    @Test
    public void testDeinitializeIsIdempotent() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld05");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull("The context attribute should not be null.", loggerContextCaptor.getValue());

        this.initializerImpl.stop();
        this.initializerImpl.stop();
        this.initializerImpl.stop();
        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));
    }

    @Test
    public void testInitializeUsingJndiSelectorFails() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("true");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        expectedException.expect(IllegalStateException.class);
        this.initializerImpl.start();
    }

    @Test
    public void testInitializeUsingJndiSelector() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn("helloWorld06");
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("true");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNull("The context attribute should be null.", loggerContextCaptor.getValue());

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should still be null because no named selector.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.stop();

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testWrapExecutionWithNoParameters() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld07");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull("The context attribute should not be null.", loggerContextCaptor.getValue());
        final org.apache.logging.log4j.spi.LoggerContext loggerContext = loggerContextCaptor.getValue();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
                assertNotNull("The context should not be null.", context);
                assertSame("The context is not correct.", loggerContext, context);
            }
        };

        this.initializerImpl.wrapExecution(runnable);

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }
}
