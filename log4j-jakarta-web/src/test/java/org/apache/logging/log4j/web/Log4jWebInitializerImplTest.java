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

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import jakarta.servlet.ServletContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class Log4jWebInitializerImplTest {
	/* Marking servletContext lenient because otherwise testCompositeLocationParameterWithEmptyUriListSetsDefaultConfiguration fails
	 * when null is passed in as the initial param because Mockito deciced null isn't a String rather than the absence of a string.
	 */
	@Mock(lenient = true)
	private ServletContext servletContext;
    @Captor
    private ArgumentCaptor<Log4jWebLifeCycle> initializerCaptor;
    @Captor
    private ArgumentCaptor<LoggerContext> loggerContextCaptor;

    private Log4jWebInitializerImpl initializerImpl;

    @BeforeEach
    public void setUp() {
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(null);

        final Log4jWebLifeCycle initializer = WebLoggerContextUtils.getWebLifeCycle(this.servletContext);

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.SUPPORT_ATTRIBUTE), initializerCaptor.capture());
        assertNotNull(initializer, "The initializer should not be null.");
        assertSame(initializer, initializerCaptor.getValue(), "The capture is not correct.");
        assertTrue(initializer instanceof Log4jWebInitializerImpl, "The initializer is not correct.");

        this.initializerImpl = (Log4jWebInitializerImpl) initializer;
    }

    @Test
    public void testDeinitializeBeforeInitialize() {
    	assertThrows(IllegalStateException.class, () -> {
    		this.initializerImpl.stop();
    	});
    }

    @Test
    public void testSetLoggerContextBeforeInitialize() {
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.setLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null.");
    }

    @Test
    public void testClearLoggerContextBeforeInitialize() {
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.clearLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null.");
    }

    @Test
    public void testInitializeWithNoParametersThenSetLoggerContextThenDeinitialize() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld01");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");
        final org.apache.logging.log4j.spi.LoggerContext loggerContext = loggerContextCaptor.getValue();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null.");

        this.initializerImpl.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull(context, "The context should not be null.");
        assertSame(loggerContext, context, "The context is not correct.");

        this.initializerImpl.clearLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null again.");

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should again still be null.");

        this.initializerImpl.setLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should finally still be null.");
    }

    @Test
    public void testInitializeWithClassLoaderNoParametersThenSetLoggerContextThenDeinitialize() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("false");
        given(servletContext.getServletContextName()).willReturn("helloWorld02");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");
        final org.apache.logging.log4j.spi.LoggerContext loggerContext = loggerContextCaptor.getValue();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null.");

        this.initializerImpl.setLoggerContext();

        final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
        assertNotNull(context, "The context should not be null.");
        assertSame(loggerContext, context, "The context is not correct.");

        this.initializerImpl.clearLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null again.");

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should again still be null.");

        this.initializerImpl.setLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should finally still be null.");
    }

    @Test
    public void testInitializeIsIdempotent() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("nothing");
        given(servletContext.getServletContextName()).willReturn("helloWorld03");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        this.initializerImpl.start();
        this.initializerImpl.start();
        this.initializerImpl.start();
        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));
    }

    @Test
    public void testInitializeFailsAfterDeinitialize() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld04");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

    	assertThrows(IllegalStateException.class, () -> {
    		this.initializerImpl.start();
    	});
    }

    @Test
    public void testDeinitializeIsIdempotent() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld05");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        given(servletContext.getClassLoader()).willReturn(getClass().getClassLoader());
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        this.initializerImpl.stop();
        this.initializerImpl.stop();
        this.initializerImpl.stop();
        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));
    }

    @Test
    public void testInitializeUsingJndiSelectorFails() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("true");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

    	assertThrows(IllegalStateException.class, () -> {
    		this.initializerImpl.start();
    	});
    }

    @Test
    public void testInitializeUsingJndiSelector() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn("helloWorld06");
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn("true");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNull(loggerContextCaptor.getValue(), "The context attribute should be null.");

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null.");

        this.initializerImpl.setLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null because no named selector.");

        this.initializerImpl.clearLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null again.");

        this.initializerImpl.stop();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should again still be null.");

        this.initializerImpl.setLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should finally still be null.");
    }

    @Test
    public void testWrapExecutionWithNoParameters() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(null);
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_CONTEXT_SELECTOR_NAMED))).willReturn(null);
        given(servletContext.getServletContextName()).willReturn("helloWorld07");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(null);
        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null.");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");
        final org.apache.logging.log4j.spi.LoggerContext loggerContext = loggerContextCaptor.getValue();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should still be null.");

        final Runnable runnable = () -> {
            final LoggerContext context = ContextAnchor.THREAD_CONTEXT.get();
            assertNotNull(context, "The context should not be null.");
            assertSame(loggerContext, context, "The context is not correct.");
        };

        this.initializerImpl.wrapExecution(runnable);

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should be null again.");

        this.initializerImpl.stop();

        then(servletContext).should().removeAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE));

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should again still be null.");

        this.initializerImpl.setLoggerContext();

        assertNull(ContextAnchor.THREAD_CONTEXT.get(), "The context should finally still be null.");
    }

    @Test
    public void testMissingLocationParameterWithNoMatchingResourceSetsNoConfigLocation() {
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(new HashSet<String>());

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        assertThat(loggerContextCaptor.getValue().getConfigLocation(), is(nullValue()));

        this.initializerImpl.stop();
    }

    @Test
    public void testMissingLocationParameterWithOneMatchingResourceUsesResourceConfigLocation() throws Exception {
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(new HashSet<>(singletonList("/WEB-INF/log4j2.xml")));
        given(servletContext.getResource("/WEB-INF/log4j2.xml")).willReturn(new URL("file:/a/b/c/WEB-INF/log4j2.xml"));

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        assertThat(loggerContextCaptor.getValue().getConfigLocation(), is(URI.create("file:/a/b/c/WEB-INF/log4j2.xml")));

        this.initializerImpl.stop();
    }

    @Test
    public void testMissingLocationParameterWithManyMatchingResourcesUsesFirstMatchingResourceConfigLocation() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn("mycontext");
        given(servletContext.getResourcePaths("/WEB-INF/")).willReturn(
                new HashSet<>(asList("/WEB-INF/a.xml", "/WEB-INF/log4j2-mycontext.xml", "/WEB-INF/log4j2.xml")));
        given(servletContext.getResource("/WEB-INF/log4j2-mycontext.xml")).willReturn(
                new URL("file:/a/b/c/WEB-INF/log4j2-mycontext.xml"));

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        assertThat(loggerContextCaptor.getValue().getConfigLocation(),
                is(URI.create("file:/a/b/c/WEB-INF/log4j2-mycontext.xml")));

        this.initializerImpl.stop();
    }

    @Test
    public void testCompositeLocationParameterWithEmptyUriListSetsDefaultConfiguration() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(",,,");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        assertThat(loggerContextCaptor.getValue().getConfiguration(), is(instanceOf(DefaultConfiguration.class)));

        this.initializerImpl.stop();
    }

    @Test
    public void testCompositeLocationParameterSetsCompositeConfiguration() {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONTEXT_NAME))).willReturn("mycontext");
        given(servletContext.getInitParameter(eq(Log4jWebSupport.LOG4J_CONFIG_LOCATION))).willReturn(
                "log4j2-combined.xml,log4j2-override.xml");

        this.initializerImpl.start();

        then(servletContext).should().setAttribute(eq(Log4jWebSupport.CONTEXT_ATTRIBUTE), loggerContextCaptor.capture());
        assertNotNull(loggerContextCaptor.getValue(), "The context attribute should not be null.");

        assertThat(loggerContextCaptor.getValue().getConfiguration(), is(instanceOf(CompositeConfiguration.class)));

        this.initializerImpl.stop();
    }
}
