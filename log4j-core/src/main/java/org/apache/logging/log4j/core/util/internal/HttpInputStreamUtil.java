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
package org.apache.logging.log4j.core.util.internal;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Utility method for reading data from an HTTP InputStream.
 */
public final class HttpInputStreamUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int NOT_MODIFIED = 304;
    private static final int NOT_AUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int OK = 200;
    private static final int BUF_SIZE = 1024;

    /**
     * Retrieves an HTTP resource if it has been modified.
     * <p>
     *     Side effects: if the request is successful, the last modified time of the {@code source}
     *     parameter is modified.
     * </p>
     * @param source The location of the HTTP resource
     * @param authorizationProvider The authentication data for the HTTP request
     * @return A {@link Result} object containing the status code and body of the response
     */
    public static Result getInputStream(
            final LastModifiedSource source, final AuthorizationProvider authorizationProvider) {
        final Result result = new Result();
        try {
            final long lastModified = source.getLastModified();
            final HttpURLConnection connection = UrlConnectionFactory.createConnection(
                    source.getURI().toURL(),
                    lastModified,
                    SslConfigurationFactory.getSslConfiguration(),
                    authorizationProvider);
            connection.connect();
            try {
                final int code = connection.getResponseCode();
                switch (code) {
                    case NOT_MODIFIED: {
                        LOGGER.debug(
                                "{} resource {}: not modified since {}",
                                formatProtocol(source),
                                () -> source,
                                () -> Instant.ofEpochMilli(lastModified));
                        result.status = Status.NOT_MODIFIED;
                        return result;
                    }
                    case NOT_FOUND: {
                        LOGGER.debug("{} resource {}: not found", formatProtocol(source), () -> source);
                        result.status = Status.NOT_FOUND;
                        return result;
                    }
                    case OK: {
                        try (final InputStream is = connection.getInputStream()) {
                            source.setLastModified(connection.getLastModified());
                            LOGGER.debug(
                                    "{} resource {}: last modified on {}",
                                    formatProtocol(source),
                                    () -> source,
                                    () -> Instant.ofEpochMilli(connection.getLastModified()));
                            result.status = Status.SUCCESS;
                            result.bytes = readStream(is);
                            return result;
                        } catch (final IOException e) {
                            try (final InputStream es = connection.getErrorStream()) {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug(
                                            "Error accessing {} resource at {}: {}",
                                            formatProtocol(source).get(),
                                            source,
                                            readStream(es),
                                            e);
                                }
                            } catch (final IOException ioe) {
                                LOGGER.debug(
                                        "Error accessing {} resource at {}",
                                        formatProtocol(source),
                                        () -> source,
                                        () -> e);
                            }
                            throw new ConfigurationException("Unable to access " + source, e);
                        }
                    }
                    case NOT_AUTHORIZED: {
                        throw new ConfigurationException("Authentication required for " + source);
                    }
                    case FORBIDDEN: {
                        throw new ConfigurationException("Access denied to " + source);
                    }
                    default: {
                        if (code < 0) {
                            LOGGER.debug("{} resource {}: invalid response code", formatProtocol(source), source);
                        } else {
                            LOGGER.debug(
                                    "{} resource {}: unexpected response code {}",
                                    formatProtocol(source),
                                    source,
                                    code);
                        }
                        throw new ConfigurationException("Unable to access " + source);
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            LOGGER.debug("Error accessing {} resource at {}", formatProtocol(source), source, e);
            throw new ConfigurationException("Unable to access " + source, e);
        }
    }

    private static Supplier<String> formatProtocol(Source source) {
        return () -> toRootUpperCase(source.getURI().getScheme());
    }

    public static byte[] readStream(final InputStream is) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[BUF_SIZE];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }

    @NullMarked
    public static class Result {

        private byte @Nullable [] bytes = null;
        private Status status;

        public Result() {
            this(Status.ERROR);
        }

        public Result(final Status status) {
            this.status = status;
        }

        /**
         * Returns the data if the status is {@link Status#SUCCESS}.
         * <p>
         *     In any other case the result is {@code null}.
         * </p>
         * @return The contents of the HTTP response or null if empty.
         */
        public @Nullable InputStream getInputStream() {
            return bytes != null ? new ByteArrayInputStream(bytes) : null;
        }

        public Status getStatus() {
            return status;
        }
    }
}
