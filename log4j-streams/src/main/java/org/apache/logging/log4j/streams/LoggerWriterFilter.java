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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.streams.util.CharStreamLogger;

/**
 * Writer that logs each line written to a pre-defined level. Can also be configured with a Marker. This class provides
 * an interface that follows the {@link java.io.Writer} methods in spirit, but doesn't require output to any external
 * out.
 */
public class LoggerWriterFilter extends FilterWriter {
    private static final String FQCN = LoggerWriterFilter.class.getName();

    private final CharStreamLogger logger;
    private final String fqcn;

    public LoggerWriterFilter(final Writer out, final Logger logger, final Level level) {
        this(out, (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerWriterFilter(final Writer out, final Logger logger, final Level level, final Marker marker) {
        this(out, (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerWriterFilter(final Writer out, final ExtendedLogger logger, final String fqcn, final Level level,
            final Marker marker) {
        super(out);
        this.logger = new CharStreamLogger(logger, level, marker);
        this.fqcn = fqcn;
    }

    @Override
    public void write(final int c) throws IOException {
        out.write(c);
        logger.put(fqcn, (char) c);
    }

    @Override
    public void write(final char[] cbuf) throws IOException {
        out.write(cbuf);
        logger.put(fqcn, cbuf, 0, cbuf.length);
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        out.write(cbuf, off, len);
        logger.put(fqcn, cbuf, off, len);
    }

    @Override
    public void write(final String str) throws IOException {
        out.write(str);
        logger.put(fqcn, str, 0, str.length());
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        out.write(str, off, len);
        logger.put(fqcn, str, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
        logger.close(fqcn);
    }

    @Override
    public String toString() {
        return LoggerWriterFilter.class.getSimpleName() + "{writer=" + out + '}';
    }
}
