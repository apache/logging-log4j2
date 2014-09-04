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
package org.apache.logging.log4j.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Builder class to wrap {@link org.apache.logging.log4j.Logger Loggers} into Java IO compatible classes.
 *
 * @since 2.1
 */
public class LoggerStreams {
    private final ExtendedLogger logger;
    private Level level;
    private Marker marker;
    private String fqcn;
    private boolean autoFlush;
    private boolean buffered;
    private int bufferSize;
    private Charset charset;
    private Reader reader;
    private Writer writer;
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * Creates a new builder for a given {@link Logger}. The Logger instance must implement {@link ExtendedLogger} or
     * an exception will be thrown.
     *
     * @param logger the Logger to wrap into a LoggerStream
     * @return a new LoggerStream builder
     * @throws IncompatibleLoggerException if {@code logger} does not implement {@link ExtendedLogger} or if
     *                                     {@code logger} is {@code null}
     */
    public static LoggerStreams forLogger(final Logger logger) {
        return new LoggerStreams(logger);
    }

    public static LoggerStreams forLogger(final String loggerName) {
        return new LoggerStreams(LogManager.getLogger(loggerName));
    }

    public static LoggerStreams forLogger(final Class<?> clazz) {
        return new LoggerStreams(LogManager.getLogger(clazz));
    }

    // TODO: arg-less factory (blocked by LOG4J2-809)

    private LoggerStreams(final Logger logger) {
        if (!(logger instanceof ExtendedLogger)) {
            throw new IncompatibleLoggerException(logger);
        }
        this.logger = (ExtendedLogger) logger;
    }

    public LoggerStreams setLevel(final Level level) {
        this.level = level;
        return this;
    }

    public LoggerStreams setMarker(final Marker marker) {
        this.marker = marker;
        return this;
    }

    public LoggerStreams setWrapperClassName(final String fqcn) {
        this.fqcn = fqcn;
        return this;
    }

    public LoggerStreams setAutoFlush(final boolean autoFlush) {
        this.autoFlush = autoFlush;
        return this;
    }

    /**
     * Enables or disables using a buffered variant of the desired IO class. If this is set to {@code true}, then the
     * instances returned by {@link #buildReader()} and {@link #buildInputStream()} can be safely cast (if necessary)
     * to {@link java.io.BufferedReader} and {@link java.io.BufferedInputStream} respectively. This option does not
     * have any effect on the other built variants.
     *
     * @param buffered indicates whether or not an input LoggerStream should be buffered
     * @return {@code this}
     */
    public LoggerStreams setBuffered(final boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    public LoggerStreams setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public LoggerStreams setCharset(final Charset charset) {
        this.charset = charset;
        return this;
    }

    public LoggerStreams filter(final Reader reader) {
        this.reader = reader;
        return this;
    }

    public LoggerStreams filter(final Writer writer) {
        this.writer = writer;
        return this;
    }

    public LoggerStreams filter(final InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public LoggerStreams filter(final OutputStream outputStream) {
        this.outputStream = outputStream;
        return this;
    }

    public Reader buildReader() {
        final Reader r = requireNonNull(this.reader, "reader");
        if (this.buffered) {
            if (this.bufferSize > 0) {
                return new LoggerBufferedReader(r, this.bufferSize, this.logger, this.fqcn, this.level, this.marker);
            } else {
                return new LoggerBufferedReader(r, this.logger, this.fqcn, this.level, this.marker);
            }
        } else {
            return new LoggerReader(requireNonNull(this.reader, "reader"), this.logger, this.fqcn, this.level,
                this.marker);
        }
    }

    public Writer buildWriter() {
        if (this.writer == null) {
            return new LoggerWriter(this.logger, this.fqcn, this.level, this.marker);
        } else {
            return new LoggerFilterWriter(this.writer, this.logger, this.fqcn, this.level, this.marker);
        }
    }

    public PrintWriter buildPrintWriter() {
        if (this.writer == null) {
            return new LoggerPrintWriter(this.logger, this.autoFlush, this.fqcn, this.level, this.marker);
        } else {
            return new LoggerPrintWriter(this.writer, this.autoFlush, this.logger, this.fqcn, this.level, this.marker);
        }
    }

    public InputStream buildInputStream() {
        final InputStream i = requireNonNull(this.inputStream, "inputStream");
        if (this.buffered) {
            if (this.bufferSize > 0) {
                return new LoggerBufferedInputStream(i, this.charset, this.bufferSize, this.logger, this.fqcn,
                    this.level, this.marker);
            } else {
                return new LoggerBufferedInputStream(i, this.charset, this.logger, this.fqcn, this.level, this.marker);
            }
        }
        return new LoggerInputStream(requireNonNull(this.inputStream, "inputStream"), this.charset, this.logger,
            this.fqcn, this.level, this.marker);
    }

    public OutputStream buildOutputStream() {
        if (this.outputStream == null) {
            return new LoggerOutputStream(this.logger, this.level, this.marker, this.charset, this.fqcn);
        } else {
            return new LoggerFilterOutputStream(this.outputStream, this.charset, this.logger, this.fqcn, this.level,
                this.marker);
        }
    }

    public PrintStream buildPrintStream() {
        try {
            if (this.outputStream == null) {
                return new LoggerPrintStream(this.logger, this.autoFlush, this.charset, this.fqcn, this.level,
                    this.marker);
            } else {
                return new LoggerPrintStream(this.outputStream, this.autoFlush, this.charset, this.logger, this.fqcn,
                    this.level, this.marker);
            }
        } catch (final UnsupportedEncodingException e) {
            // this exception shouldn't really happen since we use Charset and not String
            throw new LoggingException(e);
        }
    }

    private static <T> T requireNonNull(final T obj, final String name) {
        if (obj == null) {
            throw new IllegalStateException("The property " + name + " was not set");
        }
        return obj;
    }

}
