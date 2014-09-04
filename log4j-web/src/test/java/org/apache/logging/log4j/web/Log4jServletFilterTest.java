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

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;

public class Log4jServletFilterTest {
    private FilterConfig filterConfig;
    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    private Log4jServletFilter filter;

    @Before
    public void setUp() {
        this.filterConfig = createStrictMock(FilterConfig.class);
        this.servletContext = createStrictMock(ServletContext.class);
        this.initializer = createStrictMock(Log4jWebLifeCycle.class);

        this.filter = new Log4jServletFilter();
    }

    @After
    public void tearDown() {
        verify(this.filterConfig, this.servletContext, this.initializer);
    }

    @Test
    public void testInitAndDestroy() throws Exception {
        expect(this.filterConfig.getServletContext()).andReturn(this.servletContext);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.clearLoggerContext();
        expectLastCall();

        replay(this.filterConfig, this.servletContext, this.initializer);

        this.filter.init(this.filterConfig);

        verify(this.filterConfig, this.servletContext, this.initializer);
        reset(this.filterConfig, this.servletContext, this.initializer);

        this.initializer.setLoggerContext();
        expectLastCall();

        replay(this.filterConfig, this.servletContext, this.initializer);

        this.filter.destroy();
    }

    @Test(expected = IllegalStateException.class)
    public void testDestroy() {
        replay(this.filterConfig, this.servletContext, this.initializer);

        this.filter.destroy();
    }

    @Test
    public void testDoFilterFirstTime() throws Exception {
        expect(this.filterConfig.getServletContext()).andReturn(this.servletContext);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.clearLoggerContext();
        expectLastCall();

        replay(this.filterConfig, this.servletContext, this.initializer);

        this.filter.init(this.filterConfig);

        verify(this.filterConfig, this.servletContext, this.initializer);
        reset(this.filterConfig, this.servletContext, this.initializer);

        final ServletRequest request = createStrictMock(ServletRequest.class);
        final ServletResponse response = createStrictMock(ServletResponse.class);
        final FilterChain chain = createStrictMock(FilterChain.class);

        expect(request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE)).andReturn(null);
        request.setAttribute(eq(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE), eq(Boolean.TRUE));
        expectLastCall();
        this.initializer.setLoggerContext();
        expectLastCall();
        chain.doFilter(same(request), same(response));
        expectLastCall();
        this.initializer.clearLoggerContext();
        expectLastCall();

        replay(this.filterConfig, this.servletContext, this.initializer, request, response, chain);

        this.filter.doFilter(request, response, chain);

        verify(request, response, chain);
    }

    @Test
    public void testDoFilterSecondTime() throws Exception {
        expect(this.filterConfig.getServletContext()).andReturn(this.servletContext);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.clearLoggerContext();
        expectLastCall();

        replay(this.filterConfig, this.servletContext, this.initializer);

        this.filter.init(this.filterConfig);

        verify(this.filterConfig, this.servletContext, this.initializer);
        reset(this.filterConfig, this.servletContext, this.initializer);

        final ServletRequest request = createStrictMock(ServletRequest.class);
        final ServletResponse response = createStrictMock(ServletResponse.class);
        final FilterChain chain = createStrictMock(FilterChain.class);

        expect(request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE)).andReturn(true);
        expectLastCall();
        chain.doFilter(same(request), same(response));
        expectLastCall();

        replay(this.filterConfig, this.servletContext, this.initializer, request, response, chain);

        this.filter.doFilter(request, response, chain);

        verify(request, response, chain);
    }
}
