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

package org.apache.logging.log4j.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs each line read to a pre-defined level. Can also be configured with a Marker.
 * 
 * @since 2.1
 */
public class LoggerReader extends FilterReader {
    private static final String FQCN = LoggerReader.class.getName();

    private final CharStreamLogger logger;
    private final String fqcn;

    protected LoggerReader(final Reader reader, final ExtendedLogger logger, final String fqcn, final Level level,
                           final Marker marker) {
        super(reader);
        this.logger = new CharStreamLogger(logger, level, marker);
        this.fqcn = fqcn == null ? FQCN : fqcn;
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.logger.close(this.fqcn);
    }

    @Override
    public int read() throws IOException {
        final int c = super.read();
        this.logger.put(this.fqcn, c);
        return c;
    }

    @Override
    public int read(final char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        final int charsRead = super.read(cbuf, off, len);
        this.logger.put(this.fqcn, cbuf, off, charsRead);
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
    public String toString() {
        return LoggerReader.class.getSimpleName() + "{stream=" + this.in + '}';
    }
}
