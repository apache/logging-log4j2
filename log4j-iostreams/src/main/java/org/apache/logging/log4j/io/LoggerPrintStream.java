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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.io.internal.InternalPrintStream;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs each line written to a pre-defined level. Can also be configured with a Marker. This class provides an interface
 * that follows the {@link java.io.PrintStream} methods in spirit, but doesn't require output to any external stream.
 * This class should <em>not</em> be used as a stream for an underlying logger unless it's being used as a bridge.
 * Otherwise, infinite loops may occur!
 *
 * @since 2.1
 */
public class LoggerPrintStream extends PrintStream {
    private static final String FQCN = LoggerPrintStream.class.getName();
    private final InternalPrintStream psLogger;

    protected LoggerPrintStream(final ExtendedLogger logger, final boolean autoFlush, final Charset charset,
                                final String fqcn, final Level level, final Marker marker)
        throws UnsupportedEncodingException {
        super(new PrintStream(new ByteArrayOutputStream()));
        psLogger = new InternalPrintStream(logger, autoFlush, charset, fqcn == null ? FQCN : fqcn, level, marker);
    }

    protected LoggerPrintStream(final OutputStream out, final boolean autoFlush, final Charset charset,
                                final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker)
        throws UnsupportedEncodingException {
        super(new PrintStream(out));
        psLogger = new InternalPrintStream(out, autoFlush, charset, logger, fqcn == null ? FQCN : fqcn, level, marker);
    }

    private static Charset ensureNonNull(final Charset charset) {
        return charset == null ? Charset.defaultCharset() : charset;
    }

    @Override
    public LoggerPrintStream append(final char c) {
        psLogger.append(c);
        return this;
    }

    @Override
    public LoggerPrintStream append(final CharSequence csq) {
        psLogger.append(csq);
        return this;
    }

    @Override
    public LoggerPrintStream append(final CharSequence csq, final int start, final int end) {
        psLogger.append(csq, start, end);
        return this;
    }

    @Override
    public boolean checkError() {
        return psLogger.checkError();
    }

    @Override
    public void close() {
        psLogger.close();
    }

    @Override
    public void flush() {
        psLogger.flush();
    }

    @Override
    public LoggerPrintStream format(final Locale l, final String format, final Object... args) {
        psLogger.format(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintStream format(final String format, final Object... args) {
        psLogger.format(format, args);
        return this;
    }

    @Override
    public void print(final boolean b) {
        psLogger.print(b);
    }

    @Override
    public void print(final char c) {
        psLogger.print(c);
    }

    @Override
    public void print(final char[] s) {
        psLogger.print(s);
    }

    @Override
    public void print(final double d) {
        psLogger.print(d);
    }

    @Override
    public void print(final float f) {
        psLogger.print(f);
    }

    @Override
    public void print(final int i) {
        psLogger.print(i);
    }

    @Override
    public void print(final long l) {
        psLogger.print(l);
    }

    @Override
    public void print(final Object obj) {
        psLogger.print(obj);
    }

    @Override
    public void print(final String s) {
        psLogger.print(s);
    }

    @Override
    public LoggerPrintStream printf(final Locale l, final String format, final Object... args) {
        psLogger.printf(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintStream printf(final String format, final Object... args) {
        psLogger.printf(format, args);
        return this;
    }

    @Override
    public void println() {
        psLogger.println();
    }

    @Override
    public void println(final boolean x) {
        psLogger.println(x);
    }

    @Override
    public void println(final char x) {
        psLogger.println(x);
    }

    @Override
    public void println(final char[] x) {
        psLogger.println(x);
    }

    @Override
    public void println(final double x) {
        psLogger.println(x);
    }

    @Override
    public void println(final float x) {
        psLogger.println(x);
    }

    @Override
    public void println(final int x) {
        psLogger.println(x);
    }

    @Override
    public void println(final long x) {
        psLogger.println(x);
    }

    @Override
    public void println(final Object x) {
        psLogger.println(x);
    }

    @Override
    public void println(final String x) {
        psLogger.println(x);
    }

    @Override
    public String toString() {
        return LoggerPrintStream.class.getSimpleName() + psLogger.toString();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        psLogger.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        psLogger.write(b, off, len);
    }

    @Override
    public void write(final int b) {
        psLogger.write(b);
    }
}
