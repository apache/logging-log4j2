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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.streams.helpers.ByteStreamLogger;

/**
 * Input stream that logs each line read to a pre-defined level. Can also be configured with a
 * Marker.
 */
public class LoggerInputStream extends FilterInputStream {
    private static final String FQCN = LoggerInputStream.class.getName();

    private final String fqcn;
    private final ByteStreamLogger logger;

    public LoggerInputStream(final InputStream in, final Logger logger, final Level level) {
        this(in, Charset.defaultCharset(), (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerInputStream(final InputStream in, final Charset charset, final Logger logger, final Level level) {
        this(in, charset, (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerInputStream(final InputStream in, final Logger logger, final Level level, final Marker marker) {
        this(in, Charset.defaultCharset(), (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerInputStream(final InputStream in, final Charset charset, final Logger logger, final Level level, final Marker marker) {
        this(in, charset, (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerInputStream(final InputStream in, final Charset charset, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        super(in);
        this.logger = new ByteStreamLogger(logger, level, marker, charset);
        this.fqcn = fqcn;
    }

    @Override
    public int read() throws IOException {
        final int b = super.read();
        logger.put(fqcn, b);
        return b;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int bytesRead = super.read(b, off, len);
        logger.put(fqcn, b, off, bytesRead);
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        logger.close(fqcn);
        super.close();
    }

    @Override
    public String toString() {
        return LoggerInputStream.class.getSimpleName() + "{stream=" + in + '}';
    }
}
