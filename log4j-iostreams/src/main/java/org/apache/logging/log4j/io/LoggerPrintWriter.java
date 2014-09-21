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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logs each line written to a pre-defined level. Can also be configured with a Marker. This class provides an interface
 * that follows the {@link java.io.PrintWriter} methods in spirit, but doesn't require output to any external writer.
 * <p>
 * Integration with JDBC logging can be as simple as:
 * </p>
 * <pre>
 *     PrintWriter pw = IoBuilder.forLogger().setLevel(Level.DEBUG).buildPrintWriter();
 *     DriverManager.setLogWriter(pw);
 *     DataSource ds = ...
 *     ds.setLogWriter(pw);
 * </pre>
 *
 * @since 2.1
 */
// TODO
// All method implementations that call only super are apparently required for the unit tests to pass.
// Not sure if this a bug in the tests or a feature.
public class LoggerPrintWriter extends PrintWriter {
    private static final String FQCN = LoggerPrintWriter.class.getName();

    protected LoggerPrintWriter(final ExtendedLogger logger, final boolean autoFlush, final String fqcn,
                                final Level level, final Marker marker) {
        super(new LoggerWriter(logger, fqcn == null ? FQCN : fqcn, level, marker), autoFlush);
    }

    protected LoggerPrintWriter(final Writer writer, final boolean autoFlush, final ExtendedLogger logger,
                                final String fqcn, final Level level, final Marker marker) {
        super(new LoggerFilterWriter(writer, logger, fqcn == null ? FQCN : fqcn, level, marker), autoFlush);
    }

    @Override
    public LoggerPrintWriter append(final char c) {
        super.append(c);
        return this;
    }

    @Override
    public LoggerPrintWriter append(final CharSequence csq) {
        super.append(csq);
        return this;
    }

    @Override
    public LoggerPrintWriter append(final CharSequence csq, final int start, final int end) {
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
    public LoggerPrintWriter format(final Locale l, final String format, final Object... args) {
        super.format(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintWriter format(final String format, final Object... args) {
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
    public LoggerPrintWriter printf(final Locale l, final String format, final Object... args) {
        super.printf(l, format, args);
        return this;
    }

    @Override
    public LoggerPrintWriter printf(final String format, final Object... args) {
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
        return LoggerPrintWriter.class.getSimpleName() + "{stream=" + this.out + '}';
    }

    @Override
    public void write(final char[] buf) {
        super.write(buf);
    }

    @Override
    public void write(final char[] buf, final int off, final int len) {
        super.write(buf, off, len);
    }

    @Override
    public void write(final int c) {
        super.write(c);
    }

    @Override
    public void write(final String s) {
        super.write(s);
    }

    @Override
    public void write(final String s, final int off, final int len) {
        super.write(s, off, len);
    }
}
