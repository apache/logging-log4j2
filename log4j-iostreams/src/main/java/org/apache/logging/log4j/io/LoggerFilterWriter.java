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
package org.apache.logging.log4j.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.io.internal.InternalFilterWriter;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs each line written to a pre-defined level. Can also be configured with a Marker. This class provides an interface
 * that follows the {@link java.io.Writer} methods in spirit, but doesn't require output to any external out.
 *
 * @since 2.1
 */
public class LoggerFilterWriter extends FilterWriter {
    private static final String FQCN = LoggerFilterWriter.class.getName();

    private final InternalFilterWriter logger;

    protected LoggerFilterWriter(
            final Writer out, final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker) {
        super(out);
        this.logger = new InternalFilterWriter(out, logger, fqcn == null ? FQCN : fqcn, level, marker);
    }

    @Override
    public void close() throws IOException {
        this.logger.close();
    }

    @Override
    public void flush() throws IOException {
        this.logger.flush();
    }

    @Override
    public String toString() {
        return LoggerFilterWriter.class.getSimpleName() + logger.toString();
    }

    @Override
    public void write(final char[] cbuf) throws IOException {
        this.logger.write(cbuf);
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        this.logger.write(cbuf, off, len);
    }

    @Override
    public void write(final int c) throws IOException {
        this.logger.write(c);
    }

    @Override
    public void write(final String str) throws IOException {
        this.logger.write(str);
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException {
        this.logger.write(str, off, len);
    }
}
