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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.reset;

@RunWith(MockitoJUnitRunner.class)
public class Log4jServletFilterTest {
    @Mock
    private FilterConfig filterConfig;
    @Mock
    private ServletContext servletContext;
    @Mock
    private Log4jWebLifeCycle initializer;
    @Mock
    private ServletRequest request;
    @Mock
    private ServletResponse response;
    @Mock
    private FilterChain chain;

    private Log4jServletFilter filter;

    @Before
    public void setUp() {
        given(filterConfig.getServletContext()).willReturn(servletContext);
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(initializer);
        this.filter = new Log4jServletFilter();
    }

    @Test
    public void testInitAndDestroy() throws Exception {
        this.filter.init(this.filterConfig);

        then(initializer).should().clearLoggerContext();

        this.filter.destroy();

        then(initializer).should().setLoggerContext();
    }

    @Test(expected = IllegalStateException.class)
    public void testDestroy() {
        this.filter.destroy();
    }

    @Test
    public void testDoFilterFirstTime() throws Exception {
        this.filter.init(this.filterConfig);

        then(initializer).should().clearLoggerContext();
        reset(initializer);

        given(request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE)).willReturn(null);

        this.filter.doFilter(request, response, chain);

        then(request).should().setAttribute(eq(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE), eq(true));
        then(initializer).should().setLoggerContext();
        then(chain).should().doFilter(same(request), same(response));
        then(chain).shouldHaveNoMoreInteractions();
        then(initializer).should().clearLoggerContext();
    }

    @Test
    public void testDoFilterSecondTime() throws Exception {
        this.filter.init(this.filterConfig);

        then(initializer).should().clearLoggerContext();

        given(request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE)).willReturn(true);

        this.filter.doFilter(request, response, chain);

        then(chain).should().doFilter(same(request), same(response));
        then(chain).shouldHaveNoMoreInteractions();
    }
}
