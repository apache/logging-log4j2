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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal {@link ServletContext} stub for Jakarta servlet lifecycle tests.
 */
public class MockJakartaServletContext implements ServletContext {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, String> initParameters = new HashMap<>();
    private final Map<String, Class<? extends Filter>> filters = new HashMap<>();
    private final Set<EventListener> listeners = new HashSet<>();

    private int majorVersion = 5;
    private int effectiveMajorVersion = 5;
    private String contextPath = "/test-app";
    private boolean allowFilterRegistration = true;
    private String requestCharacterEncoding;
    private String responseCharacterEncoding;

    public void setMajorVersion(final int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public void setEffectiveMajorVersion(final int effectiveMajorVersion) {
        this.effectiveMajorVersion = effectiveMajorVersion;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public void setTestInitParameter(final String name, final String value) {
        initParameters.put(name, value);
    }

    public void setAllowFilterRegistration(final boolean allowFilterRegistration) {
        this.allowFilterRegistration = allowFilterRegistration;
    }

    public Map<String, Class<? extends Filter>> getRegisteredFilters() {
        return Collections.unmodifiableMap(filters);
    }

    public Set<EventListener> getRegisteredListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public int getMajorVersion() {
        return majorVersion;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return effectiveMajorVersion;
    }

    @Override
    public String getInitParameter(final String name) {
        return initParameters.get(name);
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(final String name, final Object object) {
        if (object == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, object);
        }
    }

    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(final String filterName, final Class<? extends Filter> filterClass) {
        if (!allowFilterRegistration) {
            return null;
        }
        filters.put(filterName, filterClass);
        return new MockFilterRegistrationDynamic(filterName);
    }

    @Override
    public void addListener(final EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public String getServletContextName() {
        return contextPath;
    }

    @Override
    public ServletContext getContext(final String uripath) {
        return this;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getServerInfo() {
        return "MockJakartaServletContext";
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public boolean setInitParameter(final String name, final String value) {
        return initParameters.putIfAbsent(name, value) == null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public String getMimeType(final String file) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(final String path) {
        return Collections.emptySet();
    }

    @Override
    public URL getResource(final String path) throws MalformedURLException {
        return getClass().getClassLoader().getResource(path.startsWith("/") ? path.substring(1) : path);
    }

    @Override
    public InputStream getResourceAsStream(final String path) {
        return getClass().getClassLoader().getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final String path) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(final String name) {
        return null;
    }

    @Override
    public Servlet getServlet(final String name) throws ServletException {
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return Collections.emptyEnumeration();
    }

    @Override
    public Enumeration<String> getServletNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public void log(final String msg) {
        // no-op
    }

    @Override
    public void log(final Exception exception, final String msg) {
        // no-op
    }

    @Override
    public void log(final String message, final Throwable throwable) {
        // no-op
    }

    @Override
    public String getRealPath(final String path) {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(final String servletName, final Class<? extends Servlet> servletClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(final String servletName, final String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(final String servletName, final String jspFile) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Servlet> T createServlet(final Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration getServletRegistration(final String servletName) {
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.emptyMap();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(final String filterName, final Filter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(final String filterName, final String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Filter> T createFilter(final Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration getFilterRegistration(final String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Collections.emptyMap();
    }

    @Override
    public void addListener(final Class<? extends EventListener> listenerClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(final String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> T createListener(final Class<T> clazz) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void declareRoles(final String... roleNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public int getSessionTimeout() {
        return 30;
    }

    @Override
    public void setSessionTimeout(final int sessionTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSessionTrackingModes(final Set<SessionTrackingMode> sessionTrackingModes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return Collections.emptySet();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return Collections.emptySet();
    }

    @Override
    public String getVirtualServerName() {
        return "mock";
    }

    @Override
    public String getRequestCharacterEncoding() {
        return requestCharacterEncoding;
    }

    @Override
    public void setRequestCharacterEncoding(final String encoding) {
        this.requestCharacterEncoding = encoding;
    }

    @Override
    public String getResponseCharacterEncoding() {
        return responseCharacterEncoding;
    }

    @Override
    public void setResponseCharacterEncoding(final String encoding) {
        this.responseCharacterEncoding = encoding;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    private static final class MockFilterRegistrationDynamic implements FilterRegistration.Dynamic {

        private final String filterName;
        private boolean asyncSupported;

        private MockFilterRegistrationDynamic(final String filterName) {
            this.filterName = filterName;
        }

        @Override
        public void setAsyncSupported(final boolean isAsyncSupported) {
            this.asyncSupported = isAsyncSupported;
        }

        boolean isAsyncSupported() {
            return asyncSupported;
        }

        @Override
        public void addMappingForUrlPatterns(
                final EnumSet<DispatcherType> dispatcherTypes, final boolean isMatchAfter, final String... urlPatterns) {
            // tracked by tests via filter registration side effects
        }

        @Override
        public String getName() {
            return filterName;
        }

        @Override
        public String getClassName() {
            return null;
        }

        @Override
        public boolean setInitParameter(final String name, final String value) {
            return false;
        }

        @Override
        public String getInitParameter(final String name) {
            return null;
        }

        @Override
        public Set<String> setInitParameters(final Map<String, String> initParameters) {
            return Collections.emptySet();
        }

        @Override
        public Map<String, String> getInitParameters() {
            return Collections.emptyMap();
        }

        @Override
        public Collection<String> getUrlPatternMappings() {
            return Collections.emptyList();
        }

        @Override
        public Collection<String> getServletNameMappings() {
            return Collections.emptyList();
        }

        @Override
        public void addMappingForServletNames(
                final EnumSet<DispatcherType> dispatcherTypes,
                final boolean isMatchAfter,
                final String... servletNames) {
            throw new UnsupportedOperationException();
        }
    }
}
