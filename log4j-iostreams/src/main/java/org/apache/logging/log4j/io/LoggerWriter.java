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

import java.io.IOException;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.io.internal.InternalWriter;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs each line written to a pre-defined level. Can also be configured with a Marker. This class provides an interface
 * that follows the {@link java.io.Writer} methods in spirit, but doesn't require output to any external writer.
 *
 * @since 2.1
 */
public class LoggerWriter extends Writer {
    private static final String FQCN = LoggerWriter.class.getName();

    private final InternalWriter writer;

    protected LoggerWriter(final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        this.writer = new InternalWriter(logger, fqcn == null ? FQCN : fqcn, level, marker);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        // do nothing
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[fqcn=" + writer.toString();
    }

    @Override
    public void write(final char[] cbuf) throws IOException {
        writer.write(cbuf);
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override
    public void write(final int c) throws IOException {
        writer.write(c);
    }

    @Override
    public void write(final String str) throws IOException {
        writer.write(str);
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        writer.write(str, off, len);
    }
}
