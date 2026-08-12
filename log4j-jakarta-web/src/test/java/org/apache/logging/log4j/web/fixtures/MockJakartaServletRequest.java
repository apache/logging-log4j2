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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal {@link ServletRequest} stub for filter lifecycle tests.
 */
public class MockJakartaServletRequest implements ServletRequest {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private ServletContext servletContext;

    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
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
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public AsyncContext startAsync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return "localhost";
    }

    @Override
    public int getServerPort() {
        return 8080;
    }

    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    @Override
    public String getRemoteHost() {
        return "localhost";
    }

    @Override
    public int getRemotePort() {
        return 12345;
    }

    @Override
    public String getLocalName() {
        return "localhost";
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        return 8080;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(final String path) {
        return null;
    }

    @Override
    public String getRealPath(final String path) {
        return null;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Collections.singletonList(getLocale()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(final String env) {
        // no-op
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public long getContentLengthLong() {
        return -1;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParameter(final String name) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String[] getParameterValues(final String name) {
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.emptyMap();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }
}
