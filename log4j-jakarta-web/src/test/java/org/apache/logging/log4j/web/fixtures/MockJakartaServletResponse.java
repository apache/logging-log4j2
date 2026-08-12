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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Minimal {@link ServletResponse} stub for filter lifecycle tests.
 */
public class MockJakartaServletResponse implements ServletResponse {

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public void setCharacterEncoding(final String charset) {
        // no-op
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void setContentType(final String type) {
        // no-op
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLength(final int len) {
        // no-op
    }

    @Override
    public void setContentLengthLong(final long len) {
        // no-op
    }

    @Override
    public void setBufferSize(final int size) {
        // no-op
    }

    @Override
    public int getBufferSize() {
        return 8192;
    }

    @Override
    public void flushBuffer() {
        // no-op
    }

    @Override
    public void resetBuffer() {
        // no-op
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        // no-op
    }

    @Override
    public void setLocale(final Locale loc) {
        // no-op
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }
}
