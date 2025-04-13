/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.reset;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Log4jServletFilterTest {
    @Mock(lenient = true) // because filterConfig is not used in testDestroy
    private FilterConfig filterConfig;

    @Mock(lenient = true) // because filterConfig is not used in testDestroy
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

    @BeforeEach
    void setUp() {
        given(filterConfig.getServletContext()).willReturn(servletContext);
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(initializer);
        this.filter = new Log4jServletFilter();
    }

    @Test
    void testInitAndDestroy() throws Exception {
        this.filter.init(this.filterConfig);

        then(initializer).should().clearLoggerContext();

        this.filter.destroy();

        then(initializer).should().setLoggerContext();
    }

    @Test
    void testDestroy() {
        assertThrows(IllegalStateException.class, () -> {
            this.filter.destroy();
        });
    }

    @Test
    void testDoFilterFirstTime() throws Exception {
        this.filter.init(this.filterConfig);

        then(initializer).should().clearLoggerContext();
        reset(initializer);

        given(request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE))
                .willReturn(null);

        this.filter.doFilter(request, response, chain);

        then(request).should().setAttribute(eq(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE), eq(true));
        then(initializer).should().setLoggerContext();
        then(chain).should().doFilter(same(request), same(response));
        then(chain).shouldHaveNoMoreInteractions();
        then(initializer).should().clearLoggerContext();
        then(request).should().removeAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE);
    }

    @Test
    void testDoFilterSecondTime() throws Exception {
        this.filter.init(this.filterConfig);

        then(initializer).should().clearLoggerContext();

        given(request.getAttribute(Log4jServletFilter.ALREADY_FILTERED_ATTRIBUTE))
                .willReturn(true);

        this.filter.doFilter(request, response, chain);

        then(chain).should().doFilter(same(request), same(response));
        then(chain).shouldHaveNoMoreInteractions();
    }
}
