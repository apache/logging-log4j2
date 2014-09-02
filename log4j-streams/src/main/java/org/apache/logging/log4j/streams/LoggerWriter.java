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

import java.io.IOException;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.streams.util.CharStreamLogger;

/**
 * Logs each line written to a pre-defined level. Can also be configured with a Marker. This class provides an interface
 * that follows the {@link java.io.Writer} methods in spirit, but doesn't require output to any external writer.
 */
public class LoggerWriter extends Writer {
    private static final String FQCN = LoggerWriter.class.getName();

    private final CharStreamLogger logger;
    private final String fqcn;

    public LoggerWriter(final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        this.logger = new CharStreamLogger(logger, level, marker);
        this.fqcn = fqcn;
    }

    public LoggerWriter(final Logger logger, final Level level) {
        this((ExtendedLogger) logger, FQCN, level, null);
    }

    public LoggerWriter(final Logger logger, final Level level, final Marker marker) {
        this((ExtendedLogger) logger, FQCN, level, marker);
    }

    @Override
    public void close() throws IOException {
        logger.close(fqcn);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[fqcn=" + fqcn + ", logger=" + logger + "]";
    }

    @Override
    public void write(final char[] cbuf) throws IOException {
        logger.put(fqcn, cbuf, 0, cbuf.length);
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        logger.put(fqcn, cbuf, off, len);
    }

    @Override
    public void write(final int c) throws IOException {
        logger.put(fqcn, (char) c);
    }

    @Override
    public void write(final String str) throws IOException {
        logger.put(fqcn, str, 0, str.length());
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        logger.put(fqcn, str, off, len);
    }
}
