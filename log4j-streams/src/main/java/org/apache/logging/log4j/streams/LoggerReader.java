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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.streams.util.CharStreamLogger;

/**
 * Writer that logs each line written to a pre-defined level. Can also be configured with a Marker.
 * This class provides an interface that follows the {@link java.io.Writer} methods in spirit, but
 * doesn't require output to any external writer.
 */
public class LoggerReader extends FilterReader {
    private static final String FQCN = LoggerReader.class.getName();

    private final CharStreamLogger logger;
    private final String fqcn;

    public LoggerReader(final Reader reader, final Logger logger, final Level level) {
        this(reader, (ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerReader(final Reader reader, final Logger logger, final Level level, final Marker marker) {
        this(reader, (ExtendedLogger) logger, FQCN, level, marker);
    }

    public LoggerReader(final Reader reader, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        super(reader);
        this.logger = new CharStreamLogger(logger, level, marker);
        this.fqcn = fqcn;
    }

    @Override
    public int read() throws IOException {
        final int c = super.read();
        logger.put(fqcn, c);
        return c;
    }

    @Override
    public int read(final char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        final int charsRead = super.read(cbuf, off, len);
        logger.put(fqcn, cbuf, off, charsRead);
        return charsRead;
    }

    @Override
    public int read(final CharBuffer target) throws IOException {
        final int len = target.remaining();
        final char[] cbuf = new char[len];
        final int charsRead = read(cbuf, 0, len);
        if (charsRead > 0) {
            target.put(cbuf, 0, charsRead);
        }
        return charsRead;
    }

    @Override
    public void close() throws IOException {
        super.close();
        logger.close(fqcn);
    }

    @Override
    public String toString() {
        return LoggerReader.class.getSimpleName() + "{stream=" + in + '}';
    }
}
