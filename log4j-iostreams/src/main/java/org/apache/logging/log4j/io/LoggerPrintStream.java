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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
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

    protected LoggerPrintStream(final ExtendedLogger logger, final boolean autoFlush, final Charset charset,
                                final String fqcn, final Level level, final Marker marker)
        throws UnsupportedEncodingException {
        super(new LoggerOutputStream(logger, level, marker, ensureNonNull(charset), fqcn == null ? FQCN : fqcn),
            autoFlush, ensureNonNull(charset).name());
    }

    protected LoggerPrintStream(final OutputStream out, final boolean autoFlush, final Charset charset,
                                final ExtendedLogger logger, final String fqcn, final Level level, final Marker marker)
        throws UnsupportedEncodingException {
        super(new LoggerFilterOutputStream(out, ensureNonNull(charset), logger, fqcn == null ? FQCN : fqcn, level,
            marker), autoFlush, ensureNonNull(charset).name());
    }

    private static Charset ensureNonNull(final Charset charset) {
        return charset == null ? Charset.defaultCharset() : charset;
    }

    @Override
    public LoggerPrintStream append(final char c) {
        super.append(c);
        return this;
    }

    @Override
    public LoggerPrintStream append(final CharSequence csq) {
        super.append(csq);
        return this;
    }

    @Override
    public LoggerPrintStream append(final CharSequence csq, final int start, final int end) {
        super.append(csq, start, end);
        return this;
    }

    @Override
    public boolean checkError() {
        return super.checkError();
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void flush() {
        super.flush();
    }

    @Override
    public LoggerPrintStream format(final Locale l, final String format, final Object... args) {
        super.format(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintStream format(final String format, final Object... args) {
        super.format(format, args);
        return this;
    }

    @Override
    public void print(final boolean b) {
        super.print(b);
    }

    @Override
    public void print(final char c) {
        super.print(c);
    }

    @Override
    public void print(final char[] s) {
        super.print(s);
    }

    @Override
    public void print(final double d) {
        super.print(d);
    }

    @Override
    public void print(final float f) {
        super.print(f);
    }

    @Override
    public void print(final int i) {
        super.print(i);
    }

    @Override
    public void print(final long l) {
        super.print(l);
    }

    @Override
    public void print(final Object obj) {
        super.print(obj);
    }

    @Override
    public void print(final String s) {
        super.print(s);
    }

    @Override
    public LoggerPrintStream printf(final Locale l, final String format, final Object... args) {
        super.printf(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintStream printf(final String format, final Object... args) {
        super.printf(format, args);
        return this;
    }

    @Override
    public void println() {
        super.println();
    }

    @Override
    public void println(final boolean x) {
        super.println(x);
    }

    @Override
    public void println(final char x) {
        super.println(x);
    }

    @Override
    public void println(final char[] x) {
        super.println(x);
    }

    @Override
    public void println(final double x) {
        super.println(x);
    }

    @Override
    public void println(final float x) {
        super.println(x);
    }

    @Override
    public void println(final int x) {
        super.println(x);
    }

    @Override
    public void println(final long x) {
        super.println(x);
    }

    @Override
    public void println(final Object x) {
        super.println(x);
    }

    @Override
    public void println(final String x) {
        super.println(x);
    }

    @Override
    public String toString() {
        return LoggerPrintStream.class.getSimpleName() + "{stream=" + this.out + '}';
    }

    @Override
    public void write(final byte[] b) throws IOException {
        super.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        super.write(b, off, len);
    }

    @Override
    public void write(final int b) {
        super.write(b);
    }
}
