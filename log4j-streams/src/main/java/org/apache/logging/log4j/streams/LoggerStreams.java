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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

public class LoggerStreams {

    public static Builder trace(final Logger logger) {
        return new Builder(logger, Level.TRACE, null);
    }

    public static Builder debug(final Logger logger) {
        return new Builder(logger, Level.DEBUG, null);
    }

    public static Builder info(final Logger logger) {
        return new Builder(logger, Level.INFO, null);
    }

    public static Builder warn(final Logger logger) {
        return new Builder(logger, Level.WARN, null);
    }

    public static Builder error(final Logger logger) {
        return new Builder(logger, Level.ERROR, null);
    }

    public static class Builder {
        private final Logger logger;
        private final Level level;
        private final Marker marker;

        Builder(final Logger logger, final Level level, final Marker marker) {
            this.logger = logger;
            this.level = level;
            this.marker = marker;
        }

        public Builder marker(final Marker marker) {
            return new Builder(logger, level, marker);
        }

        public PrintingBuilder printing() {
            return new PrintingBuilder(logger, level, marker, false);
        }

        public BufferedBuilder buffered() {
            return new BufferedBuilder(logger, level, marker, 0);
        }

        public LoggerWriter create(final Writer writer) {
            return new LoggerWriter(writer, logger, level, marker);
        }
    }

    public static class PrintingBuilder {
        private final Logger logger;
        private final Level level;
        private final Marker marker;
        private final boolean autoFlush;

        PrintingBuilder(final Logger logger, final Level level, final Marker marker, final boolean autoFlush) {
            this.logger = logger;
            this.level = level;
            this.marker = marker;
            this.autoFlush = autoFlush;
        }

        public PrintingBuilder marker(final Marker marker) {
            return new PrintingBuilder(logger, level, marker, autoFlush);
        }

        public PrintingBuilder autoFlush() {
            return autoFlush(true);
        }

        public PrintingBuilder autoFlush(final boolean autoFlush) {
            return new PrintingBuilder(logger, level, marker, autoFlush);
        }

        public LoggerPrintWriter create(final Writer writer) {
            return new LoggerPrintWriter(writer, autoFlush, logger, level, marker);
        }

        public LoggerPrintStream create(final OutputStream out) {
            return new LoggerPrintStream(out, autoFlush, logger, level, marker);
        }

        public LoggerPrintStream create(final OutputStream out, final Charset charset) {
            try {
                return new LoggerPrintStream(out, autoFlush, charset, logger, level, marker);
            } catch (final UnsupportedEncodingException e) {
                // Should never occur because the constructor must throw this
                throw new IllegalArgumentException("Invalid charset", e);
            }
        }
    }

    public static class BufferedBuilder {
        private final Logger logger;
        private final Level level;
        private final Marker marker;
        private final int size;

        BufferedBuilder(final Logger logger, final Level level, final Marker marker, final int size) {
            this.logger = logger;
            this.level = level;
            this.marker = marker;
            this.size = size;
        }

        public BufferedBuilder marker(final Marker marker) {
            return new BufferedBuilder(logger, level, marker, size);
        }

        public BufferedBuilder size(final int size) {
            return new BufferedBuilder(logger, level, marker, size);
        }

        public LoggerBufferedReader create(final Reader reader) {
            if (size > 0) {
                return new LoggerBufferedReader(reader, size, logger, level, marker);
            }
            return new LoggerBufferedReader(reader, logger, level, marker);
        }

        public LoggerBufferedInputStream create(final InputStream in) {
            if (size > 0) {
                return new LoggerBufferedInputStream(in, size, logger, level, marker);
            }
            return new LoggerBufferedInputStream(in, logger, level, marker);
        }

        public LoggerBufferedInputStream create(final InputStream in, final Charset charset) {
            if (size > 0) {
                return new LoggerBufferedInputStream(in, charset, size, logger, level, marker);
            }
            return new LoggerBufferedInputStream(in, charset, logger, level, marker);
        }
    }
}
