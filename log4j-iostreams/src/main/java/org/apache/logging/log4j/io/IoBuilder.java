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
import java.util.Objects;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.util.ReflectionUtil;

/**
 * Builder class to wrap {@link Logger Loggers} into Java IO compatible classes.
 *
 * <p>Both the {@link InputStream}/{@link OutputStream} and {@link Reader}/{@link Writer} family of classes are
 * supported. {@link OutputStream} and {@link Writer} instances can be wrapped by a filtered version of their
 * corresponding classes ({@link java.io.FilterOutputStream} and {@link java.io.FilterWriter}) in order to log all
 * lines written to these instances. {@link InputStream} and {@link Reader} instances can be wrapped by a sort of
 * wiretapped version of their respective classes; all lines read from these instances will be logged.</p>
 *
 * <p>The main feature, however, is the ability to create a {@link PrintWriter}, {@link PrintStream}, {@link Writer},
 * {@link java.io.BufferedWriter}, {@link OutputStream}, or {@link java.io.BufferedOutputStream} that is backed by a
 * {@link Logger}. The main inspiration for this feature is the JDBC API which uses a PrintWriter to perform debug
 * logging. In order to properly integrate APIs like JDBC into Log4j, create a PrintWriter using this class.</p>
 *
 * <p>The IoBuilder support configuration of the logging {@link Level} it should use (defaults to the level of
 * the underlying Logger), and an optional {@link Marker}. The other configurable objects are explained in more
 * detail below.</p>
 *
 * @since 2.1
 */
public class IoBuilder {
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
     * @return a new IoBuilder
     * @throws UnsupportedOperationException if {@code logger} does not implement {@link ExtendedLogger} or if
     *                                       {@code logger} is {@code null}
     */
    public static IoBuilder forLogger(final Logger logger) {
        return new IoBuilder(logger);
    }

    /**
     * Creates a new builder using a Logger name. The name provided is used to get a Logger from
     * {@link LogManager#getLogger(String)} which will be wrapped into a LoggerStream.
     *
     * @param loggerName the name of the Logger to wrap into a LoggerStream
     * @return a new IoBuilder
     */
    public static IoBuilder forLogger(final String loggerName) {
        return new IoBuilder(LogManager.getLogger(loggerName));
    }

    /**
     * Creates a new builder using a Logger named after a given Class. The Class provided is used to get a Logger from
     * {@link LogManager#getLogger(Class)} which will be wrapped into a LoggerStream.
     *
     * @param clazz the Class to use as the Logger name to wrap into a LoggerStream
     * @return a new IoBuilder
     */
    public static IoBuilder forLogger(final Class<?> clazz) {
        return new IoBuilder(LogManager.getLogger(clazz));
    }

    /**
     * Creates a new builder using a Logger named after the calling Class. This is equivalent to the following:
     * <pre>
     *     IoBuilder builder = IoBuilder.forLogger(LogManager.getLogger());
     * </pre>
     *
     * @return a new IoBuilder
     */
    public static IoBuilder forLogger() {
        return new IoBuilder(LogManager.getLogger(ReflectionUtil.getCallerClass(2)));
    }

    /**
     * Constructs a new IoBuilder for the given Logger. This method is provided for extensibility of this builder
     * class. The static factory methods should be used normally.
     *
     * @param logger the {@link ExtendedLogger} to wrap
     */
    protected IoBuilder(final Logger logger) {
        if (!(logger instanceof ExtendedLogger)) {
            throw new UnsupportedOperationException("The provided Logger [" + String.valueOf(logger) +
                "] does not implement " + ExtendedLogger.class.getName());
        }
        this.logger = (ExtendedLogger) logger;
    }

    /**
     * Specifies the {@link Level} to log at. If no Level is configured, then the Level of the wrapped Logger will be
     * used.
     *
     * @param level the Level to use for logging
     * @return {@code this}
     */
    public IoBuilder setLevel(final Level level) {
        this.level = level;
        return this;
    }

    /**
     * Specifies an optional {@link Marker} to use in all logging messages. If no Marker is specified, then no Marker
     * will be used.
     *
     * @param marker the Marker to associate with all logging messages
     * @return {@code this}
     */
    public IoBuilder setMarker(final Marker marker) {
        this.marker = marker;
        return this;
    }

    /**
     * Specifies the fully qualified class name of the IO wrapper class implementation. This method should only be
     * used when making significant extensions to the provided classes in this component and is normally unnecessary.
     *
     * @param fqcn the fully qualified class name of the IO wrapper class being built
     * @return {@code this}
     */
    public IoBuilder setWrapperClassName(final String fqcn) {
        this.fqcn = fqcn;
        return this;
    }

    /**
     * Indicates whether or not a built {@link PrintWriter} or {@link PrintStream} should automatically flush when
     * one of the {@code println}, {@code printf}, or {@code format} methods are invoked, or when a new line character
     * is printed.
     *
     * @param autoFlush if {@code true}, then {@code println}, {@code printf}, and {@code format} will auto flush
     * @return {@code this}
     */
    public IoBuilder setAutoFlush(final boolean autoFlush) {
        this.autoFlush = autoFlush;
        return this;
    }

    /**
     * Enables or disables using a buffered variant of the desired IO class. If this is set to {@code true}, then the
     * instances returned by {@link #buildReader()} and {@link #buildInputStream()} can be safely cast (if necessary)
     * to {@link java.io.BufferedReader} and {@link java.io.BufferedInputStream} respectively. This option does not
     * have any effect on the other built variants.
     *
     * @param buffered indicates whether or not a wrapped {@link InputStream} or {@link Reader} should be buffered
     * @return {@code this}
     */
    public IoBuilder setBuffered(final boolean buffered) {
        this.buffered = buffered;
        return this;
    }

    /**
     * Configures the buffer size to use when building a {@link java.io.BufferedReader} or
     * {@link java.io.BufferedInputStream} LoggerStream.
     *
     * @param bufferSize the buffer size to use or a non-positive integer to use the default size
     * @return {@code this}
     */
    public IoBuilder setBufferSize(final int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Specifies the character set to use when building an {@link InputStream}, {@link OutputStream}, or
     * {@link PrintStream}. If no character set is specified, then {@link java.nio.charset.Charset#defaultCharset()}
     * is used.
     *
     * @param charset the character set to use when building an InputStream, OutputStream, or PrintStream
     * @return {@code this}
     */
    public IoBuilder setCharset(final Charset charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Configures a {@link Reader} to be wiretapped when building a Reader. This must be set to a non-{@code null}
     * value in order to call {@link #buildReader()}.
     *
     * @param reader the Reader to wiretap
     * @return {@code this}
     */
    public IoBuilder filter(final Reader reader) {
        this.reader = reader;
        return this;
    }

    /**
     * Configures a {@link Writer} to be written to in addition to the underlying Logger. If no Writer is specified,
     * then the built Writer or PrintWriter will only write to the underlying Logger.
     *
     * @param writer the Writer to write to in addition to the Logger
     * @return {@code this}
     */
    public IoBuilder filter(final Writer writer) {
        this.writer = writer;
        return this;
    }

    /**
     * Configures an {@link InputStream} to be wiretapped when building an InputStream. This must be set to a
     * non-{@code null} value in order to call {@link #buildInputStream()}.
     *
     * @param inputStream the InputStream to wiretap
     * @return {@code this}
     */
    public IoBuilder filter(final InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    /**
     * Configures an {@link OutputStream} to be written to in addition to the underlying Logger. If no OutputStream is
     * specified, then the built OutputStream or PrintStream will only write to the underlying Logger.
     *
     * @param outputStream the OutputStream to write to in addition to the Logger
     * @return {@code this}
     */
    public IoBuilder filter(final OutputStream outputStream) {
        this.outputStream = outputStream;
        return this;
    }

    // TODO: could this builder use generics to infer the desired IO class?

    /**
     * Builds a new {@link Reader} that is wiretapped by its underlying Logger. If buffering is enabled, then a
     * {@link java.io.BufferedReader} will be returned.
     *
     * @return a new Reader wiretapped by a Logger
     * @throws IllegalStateException if no Reader was configured for this builder
     */
    public Reader buildReader() {
        final Reader in = Objects.requireNonNull(this.reader, "reader");
        if (this.buffered) {
            if (this.bufferSize > 0) {
                return new LoggerBufferedReader(in, this.bufferSize, this.logger, this.fqcn, this.level, this.marker);
            }
            return new LoggerBufferedReader(in, this.logger, this.fqcn, this.level, this.marker);
        }
        return new LoggerReader(in, this.logger, this.fqcn, this.level, this.marker);
    }

    /**
     * Builds a new {@link Writer} that is backed by a Logger and optionally writes to another Writer as well. If no
     * Writer is configured for this builder, then the returned Writer will only write to its underlying Logger.
     *
     * @return a new Writer or {@link java.io.FilterWriter} backed by a Logger
     */
    public Writer buildWriter() {
        if (this.writer == null) {
            return new LoggerWriter(this.logger, this.fqcn, this.level, this.marker);
        }
        return new LoggerFilterWriter(this.writer, this.logger, this.fqcn, this.level, this.marker);
    }

    /**
     * Builds a new {@link PrintWriter} that is backed by a Logger and optionally writes to another Writer as well. If
     * no Writer is configured for this builder, then the returned PrintWriter will only write to its underlying
     * Logger.
     *
     * @return a new PrintWriter that optionally writes to another Writer in addition to its underlying Logger
     */
    public PrintWriter buildPrintWriter() {
        if (this.writer == null) {
            return new LoggerPrintWriter(this.logger, this.autoFlush, this.fqcn, this.level, this.marker);
        }
        return new LoggerPrintWriter(this.writer, this.autoFlush, this.logger, this.fqcn, this.level, this.marker);
    }

    /**
     * Builds a new {@link InputStream} that is wiretapped by its underlying Logger. If buffering is enabled, then a
     * {@link java.io.BufferedInputStream} will be returned.
     *
     * @return a new InputStream wiretapped by a Logger
     * @throws IllegalStateException if no InputStream was configured for this builder
     */
    public InputStream buildInputStream() {
        final InputStream in = Objects.requireNonNull(this.inputStream, "inputStream");
        if (this.buffered) {
            if (this.bufferSize > 0) {
                return new LoggerBufferedInputStream(in, this.charset, this.bufferSize, this.logger, this.fqcn,
                    this.level, this.marker);
            }
            return new LoggerBufferedInputStream(in, this.charset, this.logger, this.fqcn, this.level, this.marker);
        }
        return new LoggerInputStream(in, this.charset, this.logger, this.fqcn, this.level, this.marker);
    }

    /**
     * Builds a new {@link OutputStream} that is backed by a Logger and optionally writes to another OutputStream as
     * well. If no OutputStream is configured for this builder, then the returned OutputStream will only write to its
     * underlying Logger.
     *
     * @return a new OutputStream that optionally writes to another OutputStream in addition to its underlying Logger
     */
    public OutputStream buildOutputStream() {
        if (this.outputStream == null) {
            return new LoggerOutputStream(this.logger, this.level, this.marker, this.charset, this.fqcn);
        }
        return new LoggerFilterOutputStream(this.outputStream, this.charset, this.logger, this.fqcn, this.level,
            this.marker);
    }

    /**
     * Builds a new {@link PrintStream} that is backed by a Logger and optionally writes to another OutputStream as
     * well. If no OutputStream is configured for this builder, then the returned PrintStream will only write to its
     * underlying Logger.
     *
     * @return a new PrintStream that optionally writes to another OutputStream in addition to its underlying Logger
     * @throws LoggingException if the configured character set is unsupported by {@link PrintStream}
     */
    public PrintStream buildPrintStream() {
        try {
            if (this.outputStream == null) {
                return new LoggerPrintStream(this.logger, this.autoFlush, this.charset, this.fqcn, this.level,
                    this.marker);
            }
            return new LoggerPrintStream(this.outputStream, this.autoFlush, this.charset, this.logger, this.fqcn,
                this.level, this.marker);
        } catch (final UnsupportedEncodingException e) {
            // this exception shouldn't really happen since we use Charset and not String
            throw new LoggingException(e);
        }
    }

}
