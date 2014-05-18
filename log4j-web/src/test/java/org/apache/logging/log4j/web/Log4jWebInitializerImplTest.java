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
import javax.servlet.UnavailableException;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class Log4jWebInitializerImplTest {
    private ServletContext servletContext;

    private Log4jWebInitializerImpl initializer;

    @Before
    public void setUp() {
        final Capture<Log4jWebInitializer> initializerCapture = new Capture<Log4jWebInitializer>();

        this.servletContext = createStrictMock(ServletContext.class);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(null);
        this.servletContext.setAttribute(eq(Log4jWebSupport.SUPPORT_ATTRIBUTE), capture(initializerCapture));
        expectLastCall();

        replay(this.servletContext);

        final Log4jWebInitializer initializer = Log4jWebInitializerImpl.getLog4jWebInitializer(this.servletContext);

        assertNotNull("The initializer should not be null.", initializer);
        assertSame("The capture is not correct.", initializer, initializerCapture.getValue());
        assertTrue("The initializer is not correct.", initializer instanceof Log4jWebInitializerImpl);
        verify(this.servletContext);
        reset(this.servletContext);

        this.initializer = (Log4jWebInitializerImpl)initializer;
    }

    @After
    public void tearDown() {
        verify(this.servletContext);
    }

    @Test
    public void testDeinitializeBeforeInitialize() {
        replay(this.servletContext);

        try {
            this.initializer.deinitialize();
            fail("Expected an IllegalStateException.");
        } catch (final IllegalStateException ignore) {

        }
    }

    @Test
    public void testSetLoggerContextBeforeInitialize() {
        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testClearLoggerContextBeforeInitialize() {
        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.clearLoggerContext();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeWithNoParametersThenSetLoggerContextThenDeinitialize() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld01");
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);
        org.apache.logging.log4j.spi.LoggerContext loggerContext =
                (org.apache.logging.log4j.spi.LoggerContext)loggerContextCapture.getValue();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("The context should not be null.", context);
        assertSame("The context is not correct.", loggerContext, context);

        this.initializer.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializer.deinitialize();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeWithClassLoaderNoParametersThenSetLoggerContextThenDeinitialize() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("false");
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld02");
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);
        org.apache.logging.log4j.spi.LoggerContext loggerContext =
                (org.apache.logging.log4j.spi.LoggerContext)loggerContextCapture.getValue();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("The context should not be null.", context);
        assertSame("The context is not correct.", loggerContext, context);

        this.initializer.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializer.deinitialize();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeIsIdempotent() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("nothing");
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld03");
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        this.initializer.initialize();
        this.initializer.initialize();
        this.initializer.initialize();

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializer.deinitialize();
    }

    @Test
    public void testInitializeFailsAfterDeinitialize() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld04");
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializer.deinitialize();

        try {
            this.initializer.initialize();
            fail("Expected an IllegalStateException.");
        } catch (final IllegalStateException ignore) {

        }
    }

    @Test
    public void testDeinitializeIsIdempotent() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld05");
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializer.deinitialize();
        this.initializer.deinitialize();
        this.initializer.deinitialize();
    }

    @Test
    public void testInitializeUsingJndiSelectorFails() throws Exception {
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("true");

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        try {
            this.initializer.initialize();
            fail("Expected an UnavailableException.");
        } catch (final UnavailableException ignore) {

        }
    }

    @Test
    public void testInitializeUsingJndiSelector() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn("helloWorld6");
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("true");
        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNull("The context attribute should be null.", loggerContextCapture.getValue());

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        assertNull("The context should still be null because no named selector.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        this.initializer.deinitialize();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testWrapExecutionWithNoParameters() throws Exception {
        Capture<Object> loggerContextCapture = new Capture<Object>();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld01");
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.initialize();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);
        final org.apache.logging.log4j.spi.LoggerContext loggerContext =
                (org.apache.logging.log4j.spi.LoggerContext)loggerContextCapture.getValue();

        verify(this.servletContext);
        reset(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        Runnable runnable = createStrictMock(Runnable.class);
        runnable.run();
        expectLastCall().andAnswer(new IAnswer<Void>() {
            @Override
            public Void answer() {
                final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
                assertNotNull("The context should not be null.", context);
                assertSame("The context is not correct.", loggerContext, context);
                return null;
            }
        });

        replay(this.servletContext, runnable);

        this.initializer.wrapExecution(runnable);

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext, runnable);
        reset(this.servletContext);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializer.deinitialize();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializer.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }
}
