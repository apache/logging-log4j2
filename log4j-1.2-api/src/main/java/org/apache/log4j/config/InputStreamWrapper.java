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
package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;

class InputStreamWrapper extends InputStream {

    private final String description;
    private final InputStream input;

    public InputStreamWrapper(final InputStream input, final String description) {
        this.input = input;
        this.description = description;
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @Override
    public boolean equals(final Object obj) {
        return input.equals(obj);
    }

    @Override
    public int hashCode() {
        return input.hashCode();
    }

    @Override
    public synchronized void mark(final int readlimit) {
        input.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return input.markSupported();
    }

    @Override
    public int read() throws IOException {
        return input.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return input.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public synchronized void reset() throws IOException {
        input.reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        return input.skip(n);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [description=" + description + ", input=" + input + "]";
    }

}
