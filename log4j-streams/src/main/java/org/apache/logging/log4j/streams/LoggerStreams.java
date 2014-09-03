/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.streams;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

public class LoggerStreams {

    public static class BufferedBuilder {
        private final ExtendedLogger logger;
        private final Level level;
        private final Marker marker;
        private final int size;

        BufferedBuilder(final ExtendedLogger logger, final Level level, final Marker marker, final int size) {
            this.logger = logger;
            this.level = level;
            this.marker = marker;
            this.size = size;
        }

        public LoggerBufferedInputStream create(final InputStream in) {
            if (this.size > 0) {
                return new LoggerBufferedInputStream(in, this.size, this.logger, this.level, this.marker);
            }
            return new LoggerBufferedInputStream(in, this.logger, this.level, this.marker);
        }

        public LoggerBufferedInputStream create(final InputStream in, final Charset charset) {
            if (this.size > 0) {
                return new LoggerBufferedInputStream(in, charset, this.size, this.logger, this.level, this.marker);
            }
            return new LoggerBufferedInputStream(in, charset, this.logger, this.level, this.marker);
        }

        public LoggerBufferedReader create(final Reader reader) {
            if (this.size > 0) {
                return new LoggerBufferedReader(reader, this.size, this.logger, this.level, this.marker);
            }
            return new LoggerBufferedReader(reader, this.logger, this.level, this.marker);
        }

        public BufferedBuilder marker(final Marker marker) {
            return new BufferedBuilder(this.logger, this.level, marker, this.size);
        }

        public BufferedBuilder size(final int size) {
            return new BufferedBuilder(this.logger, this.level, this.marker, size);
        }
    }

    public static class Builder {
        private final ExtendedLogger logger;
        private final Level level;
        private final Marker marker;

        Builder(final ExtendedLogger logger, final Level level, final Marker marker) {
            this.logger = logger;
            this.level = level;
            this.marker = marker;
        }

        public BufferedBuilder buffered() {
            return new BufferedBuilder(this.logger, this.level, this.marker, 0);
        }

        public LoggerFilterWriter create(final Writer writer) {
            return new LoggerFilterWriter(writer, this.logger, this.level, this.marker);
        }

        public Builder marker(final Marker marker) {
            return new Builder(this.logger, this.level, marker);
        }

        public PrintingBuilder printing() {
            return new PrintingBuilder(this.logger, this.level, this.marker, false);
        }
    }

    public static class PrintingBuilder {
        private final ExtendedLogger logger;
        private final Level level;
        private final Marker marker;
        private final boolean autoFlush;

        PrintingBuilder(final ExtendedLogger logger, final Level level, final Marker marker, final boolean autoFlush) {
            this.logger = logger;
            this.level = level;
            this.marker = marker;
            this.autoFlush = autoFlush;
        }

        public PrintingBuilder autoFlush() {
            return autoFlush(true);
        }

        public PrintingBuilder autoFlush(final boolean autoFlush) {
            return new PrintingBuilder(this.logger, this.level, this.marker, autoFlush);
        }

        public LoggerPrintStream create(final OutputStream out) {
            return new LoggerPrintStream(out, this.autoFlush, this.logger, this.level, this.marker);
        }

        public LoggerPrintStream create(final OutputStream out, final Charset charset) {
            try {
                return new LoggerPrintStream(out, this.autoFlush, charset, this.logger, this.level, this.marker);
            } catch (final UnsupportedEncodingException e) {
                // Should never occur because the constructor must throw this
                throw new IllegalArgumentException("Invalid charset", e);
            }
        }

        public LoggerPrintWriter create(final Writer writer) {
            return new LoggerPrintWriter(writer, this.autoFlush, this.logger, this.level, this.marker);
        }

        public PrintingBuilder marker(final Marker marker) {
            return new PrintingBuilder(this.logger, this.level, marker, this.autoFlush);
        }
    }

    public static Builder debug(final ExtendedLogger logger) {
        return new Builder(logger, Level.DEBUG, null);
    }

    public static Builder error(final ExtendedLogger logger) {
        return new Builder(logger, Level.ERROR, null);
    }

    public static Builder fatal(final ExtendedLogger logger) {
        return new Builder(logger, Level.FATAL, null);
    }

    public static Builder info(final ExtendedLogger logger) {
        return new Builder(logger, Level.INFO, null);
    }

    public static Builder trace(final ExtendedLogger logger) {
        return new Builder(logger, Level.TRACE, null);
    }

    public static Builder warn(final ExtendedLogger logger) {
        return new Builder(logger, Level.WARN, null);
    }
}
