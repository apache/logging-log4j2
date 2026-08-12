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
package org.apache.logging.log4j.web.fixtures;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Minimal {@link FilterConfig} stub backed by a {@link MockJakartaServletContext}.
 */
public class MockJakartaFilterConfig implements FilterConfig {

    private final String filterName;
    private final MockJakartaServletContext servletContext;

    public MockJakartaFilterConfig(final String filterName, final MockJakartaServletContext servletContext) {
        this.filterName = filterName;
        this.servletContext = servletContext;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(final String name) {
        return servletContext.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }
}
