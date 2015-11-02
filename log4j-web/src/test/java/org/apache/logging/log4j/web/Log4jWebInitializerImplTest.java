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
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class Log4jWebInitializerImplTest {
    private ServletContext servletContext;

    private Log4jWebInitializerImpl initializerImpl;

    @Before
    public void setUp() {
        final Capture<Log4jWebLifeCycle> initializerCapture = EasyMock.newCapture();

        this.servletContext = createStrictMock(ServletContext.class);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(null);
        this.servletContext.setAttribute(eq(Log4jWebSupport.SUPPORT_ATTRIBUTE), capture(initializerCapture));
        expectLastCall();

        replay(this.servletContext);

        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(this.servletContext);

        assertNotNull("The initializer should not be null.", initializer);
        assertSame("The capture is not correct.", initializer, initializerCapture.getValue());
        assertTrue("The initializer is not correct.", initializer instanceof Log4jWebInitializerImpl);
        verify(this.servletContext);
        reset(this.servletContext);

        this.initializerImpl = (Log4jWebInitializerImpl) initializer;
    }

    @After
    public void tearDown() {
        verify(this.servletContext);
    }

    @Test
    public void testDeinitializeBeforeInitialize() {
        replay(this.servletContext);

        try {
            this.initializerImpl.stop();
            fail("Expected an IllegalStateException.");
        } catch (final IllegalStateException ignore) {

        }
    }

    @Test
    public void testSetLoggerContextBeforeInitialize() {
        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testClearLoggerContextBeforeInitialize() {
        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeWithNoParametersThenSetLoggerContextThenDeinitialize() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld01");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);
        final org.apache.logging.log4j.spi.LoggerContext loggerContext =
                (org.apache.logging.log4j.spi.LoggerContext)loggerContextCapture.getValue();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("The context should not be null.", context);
        assertSame("The context is not correct.", loggerContext, context);

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializerImpl.stop();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeWithClassLoaderNoParametersThenSetLoggerContextThenDeinitialize() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("false");
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld02");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);
        final org.apache.logging.log4j.spi.LoggerContext loggerContext =
                (org.apache.logging.log4j.spi.LoggerContext)loggerContextCapture.getValue();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull("The context should not be null.", context);
        assertSame("The context is not correct.", loggerContext, context);

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializerImpl.stop();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testInitializeIsIdempotent() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("nothing");
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld03");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        this.initializerImpl.start();
        this.initializerImpl.start();
        this.initializerImpl.start();

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializerImpl.stop();
    }

    @Test
    public void testInitializeFailsAfterDeinitialize() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld04");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializerImpl.stop();

        try {
            this.initializerImpl.start();
            fail("Expected an IllegalStateException.");
        } catch (final IllegalStateException ignore) {

        }
    }

    @Test
    public void testDeinitializeIsIdempotent() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld05");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        expect(this.servletContext.getClassLoader()).andReturn(this.getClass().getClassLoader());
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);

        verify(this.servletContext);
        reset(this.servletContext);

        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializerImpl.stop();
        this.initializerImpl.stop();
        this.initializerImpl.stop();
    }

    @Test
    public void testInitializeUsingJndiSelectorFails() throws Exception {
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("true");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        try {
            this.initializerImpl.start();
            fail("Expected an IllegalStateException.");
        } catch (final IllegalStateException ignore) {
            // ignore
        }
    }

    @Test
    public void testInitializeUsingJndiSelector() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn("helloWorld6");
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn("true");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNull("The context attribute should be null.", loggerContextCapture.getValue());

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should still be null because no named selector.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.clearLoggerContext();

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        this.initializerImpl.stop();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }

    @Test
    public void testWrapExecutionWithNoParameters() throws Exception {
        final Capture<Object> loggerContextCapture = EasyMock.newCapture();

        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONTEXT_NAME)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.LOG4J_CONFIG_LOCATION)).andReturn(null);
        expect(this.servletContext.getInitParameter(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))
                .andReturn(null);
        expect(this.servletContext.getServletContextName()).andReturn("helloWorld01");
        expect(this.servletContext.getResourcePaths("/WEB-INF/")).andReturn(null);
        this.servletContext.setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), capture(loggerContextCapture));
        expectLastCall();

        replay(this.servletContext);

        assertNull("The context should be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.start();

        assertNotNull("The context attribute should not be null.", loggerContextCapture.getValue());
        assertTrue("The context attribute is not correct.",
                loggerContextCapture.getValue() instanceof org.apache.logging.log4j.spi.LoggerContext);
        final org.apache.logging.log4j.spi.LoggerContext loggerContext =
                (org.apache.logging.log4j.spi.LoggerContext)loggerContextCapture.getValue();

        verify(this.servletContext);
        reset(this.servletContext);

        assertNull("The context should still be null.", ContextAnchor.THREAD_CONTEXT.get());

        final Runnable runnable = createStrictMock(Runnable.class);
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

        this.initializerImpl.wrapExecution(runnable);

        assertNull("The context should be null again.", ContextAnchor.THREAD_CONTEXT.get());

        verify(this.servletContext, runnable);
        reset(this.servletContext);

        this.servletContext.removeAttribute(Log4jWebSupport.CONTEXT_ATTRIBUTE);
        expectLastCall();

        replay(this.servletContext);

        this.initializerImpl.stop();

        verify(this.servletContext);
        reset(this.servletContext);
        replay(this.servletContext);

        assertNull("The context should again still be null.", ContextAnchor.THREAD_CONTEXT.get());

        this.initializerImpl.setLoggerContext();

        assertNull("The context should finally still be null.", ContextAnchor.THREAD_CONTEXT.get());
    }
}
