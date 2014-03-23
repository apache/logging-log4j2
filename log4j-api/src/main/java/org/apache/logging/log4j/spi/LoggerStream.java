/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Output stream that logs each line written to a pre-defined level. Can also
 * be configured with a Marker. This class provides an interface that follows
 * the {@link java.io.PrintStream} methods in spirit, but doesn't output to
 * any external stream. This class should <em>not</em> be used as a stream for an
 * underlying logger unless it's being used as a bridge. Otherwise, infinite
 * loops may occur!
 */
public class LoggerStream extends PrintStream {

    final PrintStream stream;

    public LoggerStream(final AbstractLogger logger, final Level level) {
        super(System.out);
        stream = new PrintStream(new HelperStream(logger, null, level), true);
    }

    public LoggerStream(final AbstractLogger logger, final Marker marker, final Level level) {
        super(System.out);
        stream = new PrintStream(new HelperStream(logger, marker, level), true);
    }

    @Override
    public void write(int b) {
        stream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        stream.write(b, off, len);
    }

    @Override
    public void flush() {
        stream.flush();
    }

    @Override
    public void close() {
        stream.close();
    }

    @Override
    public void print(boolean b) {
        stream.print(b);
    }

    @Override
    public void print(char c) {
        stream.print(c);
    }

    @Override
    public void print(int i) {
        stream.print(i);
    }

    @Override
    public void print(long l) {
        stream.print(l);
    }

    @Override
    public void print(float f) {
        stream.print(f);
    }

    @Override
    public void print(double d) {
        stream.print(d);
    }

    @Override
    public void print(char[] s) {
        stream.print(s);
    }

    @Override
    public void print(String s) {
        stream.print(s);
    }

    @Override
    public void print(Object obj) {
        stream.print(obj);
    }

    @Override
    public void println() {
        stream.println();
    }

    @Override
    public void println(boolean x) {
        stream.println(x);
    }

    @Override
    public void println(char x) {
        stream.println(x);
    }

    @Override
    public void println(int x) {
        stream.println(x);
    }

    @Override
    public void println(long x) {
        stream.println(x);
    }

    @Override
    public void println(float x) {
        stream.println(x);
    }

    @Override
    public void println(double x) {
        stream.println(x);
    }

    @Override
    public void println(char[] x) {
        stream.println(x);
    }

    @Override
    public void println(String x) {
        stream.println(x);
    }

    @Override
    public void println(Object x) {
        stream.println(x);
    }

    @Override
    public LoggerStream printf(String format, Object... args) {
        stream.printf(format, args);
        return this;
    }

    @Override
    public LoggerStream printf(Locale l, String format, Object... args) {
        stream.printf(l, format, args);
        return this;
    }

    @Override
    public LoggerStream append(char c) {
        stream.append(c);
        return this;
    }

    @Override
    public LoggerStream append(CharSequence csq) {
        stream.append(csq);
        return this;
    }

    @Override
    public LoggerStream append(CharSequence csq, int start, int end) {
        stream.append(csq, start, end);
        return this;
    }

    @Override
    public LoggerStream format(String format, Object... args) {
        stream.format(format, args);
        return this;
    }

    @Override
    public LoggerStream format(Locale l, String format, Object... args) {
        stream.format(l, format, args);
        return this;
    }

    @Override
    public boolean checkError() {
        return stream.checkError();
    }

    @Override
    public String toString() {
        return "LoggerStream{" +
                "stream=" + stream +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || !(other == null || getClass() != other.getClass())
                && stream.equals(((LoggerStream) other).stream);
    }

    @Override
    public int hashCode() {
        return stream.hashCode();
    }

    private static class HelperStream extends ByteArrayOutputStream {
        private static final String FQCN = LoggerStream.class.getName();
        private final AbstractLogger logger;
        private final Level level;
        private final Marker marker;

        private HelperStream(AbstractLogger logger, Marker marker, Level level) {
            this.logger = logger;
            this.marker = marker;
            this.level = level;
        }

        private void log(int upTo) {
            if (upTo < 0 || upTo >= count) {
                throw new IndexOutOfBoundsException();
            }
            final Message message = logger.getMessageFactory().newMessage(extractLine(upTo));
            logger.logIfEnabled(FQCN, level, marker, message, null);
        }

        private String extractLine(int upTo) {
            final String line = new String(buf, 0, upTo);
            leftShiftBuffer(upTo + 1);
            return line;
        }

        private void leftShiftBuffer(int numBytes) {
            int remaining = count - numBytes;
            if (remaining > 0) {
                System.arraycopy(buf, numBytes, buf, 0, remaining);
                count = remaining + 1;
            } else {
                reset();
            }
        }

        @Override
        public synchronized void write(int b) {
            if (b == '\r') {
                return;
            }
            super.write(b);
            if (b == '\n') {
                log(count - 1);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            for (int i = 0; i < len; ++i) {
                write(b[off + i]);
            }
        }
    }
}
