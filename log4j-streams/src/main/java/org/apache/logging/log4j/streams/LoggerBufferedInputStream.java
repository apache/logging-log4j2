/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.streams;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

public class LoggerBufferedInputStream extends BufferedInputStream {
    private static final String FQCN = LoggerBufferedInputStream.class.getName();

    public LoggerBufferedInputStream(final InputStream in, final Charset charset, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        super(new LoggerInputStream(in, charset, logger, fqcn, level, marker));
    }

    public LoggerBufferedInputStream(final InputStream in, final Charset charset, final int sz, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        super(new LoggerInputStream(in, charset, logger, fqcn, level, marker), sz);
    }

    public LoggerBufferedInputStream(final InputStream in, final Charset charset, final int sz, final Logger logger, final Level level) {
        this(in, charset, sz, (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerBufferedInputStream(final InputStream in, final Charset charset, final int sz, final Logger logger, final Level level, final Marker marker) {
        this(in, charset, sz, (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerBufferedInputStream(final InputStream in, final Charset charset, final Logger logger, final Level level) {
        this(in, charset, (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerBufferedInputStream(final InputStream in, final Charset charset, final Logger logger, final Level level, final Marker marker) {
        this(in, charset, (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerBufferedInputStream(final InputStream in, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        this(in, Charset.defaultCharset(), logger, fqcn, level, marker);
    }

    public LoggerBufferedInputStream(final InputStream in, final int sz, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        this(in, Charset.defaultCharset(), sz, logger, fqcn, level, marker);
    }

    public LoggerBufferedInputStream(final InputStream in, final int sz, final Logger logger, final Level level) {
        this(in, sz, (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerBufferedInputStream(final InputStream in, final int sz, final Logger logger, final Level level, final Marker marker) {
        this(in, sz, (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerBufferedInputStream(final InputStream in, final Logger logger, final Level level) {
        this(in, (ExtendedLogger) logger, FQCN, level, null);
    }
    
    public LoggerBufferedInputStream(final InputStream in, final Logger logger, final Level level, final Marker marker) {
        this(in, (ExtendedLogger) logger, FQCN, level, marker);
    }
    
    @Override
    public void close() throws IOException {
        super.close();
    }
    
    @Override
    public synchronized int read() throws IOException {
        return super.read();
    }
    
    @Override
    public int read(final byte[] b) throws IOException {
        return super.read(b, 0, b.length);
    }
    
    @Override
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        return super.read(b, off, len);
    }

    @Override
    public String toString() {
        return LoggerBufferedInputStream.class.getSimpleName() + "{stream=" + this.in + '}';
    }
}
