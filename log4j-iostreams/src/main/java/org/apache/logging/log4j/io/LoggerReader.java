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
import org.apache.logging.log4j.io.internal.InternalReader;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs each line read to a pre-defined level. Can also be configured with a Marker.
 *
 * @since 2.1
 */
public class LoggerReader extends FilterReader {
    private static final String FQCN = LoggerReader.class.getName();

    private final InternalReader reader;

    protected LoggerReader(final Reader reader, final ExtendedLogger logger, final String fqcn, final Level level,
                           final Marker marker) {
        super(reader);
        this.reader = new InternalReader(reader, logger, fqcn == null ? FQCN : fqcn, level, marker);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int read() throws IOException {
        return reader.read();
    }

    @Override
    public int read(final char[] cbuf) throws IOException {
        return reader.read(cbuf);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public int read(final CharBuffer target) throws IOException {
        return reader.read(target);
    }

    @Override
    public String toString() {
        return LoggerReader.class.getSimpleName() + this.reader.toString();
    }
}
