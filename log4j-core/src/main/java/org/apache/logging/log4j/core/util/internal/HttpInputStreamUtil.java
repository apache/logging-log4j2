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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Utility method for reading data from an HTTP InputStream.
 */
public final class HttpInputStreamUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int NOT_MODIFIED = 304;
    private static final int NOT_AUTHORIZED = 401;
    private static final int NOT_FOUND = 404;
    private static final int OK = 200;
    private static final int BUF_SIZE = 1024;

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
                        LOGGER.debug("Configuration not modified");
                        result.status = Status.NOT_MODIFIED;
                        return result;
                    }
                    case NOT_FOUND: {
                        LOGGER.debug("Unable to access {}: Not Found", source.toString());
                        result.status = Status.NOT_FOUND;
                        return result;
                    }
                    case OK: {
                        try (final InputStream is = connection.getInputStream()) {
                            source.setLastModified(connection.getLastModified());
                            LOGGER.debug(
                                    "Content was modified for {}. previous lastModified: {}, new lastModified: {}",
                                    source.toString(),
                                    lastModified,
                                    connection.getLastModified());
                            result.status = Status.SUCCESS;
                            result.inputStream = new ByteArrayInputStream(readStream(is));
                            return result;
                        } catch (final IOException e) {
                            try (final InputStream es = connection.getErrorStream()) {
                                LOGGER.info(
                                        "Error accessing configuration at {}: {}", source.toString(), readStream(es));
                            } catch (final IOException ioe) {
                                LOGGER.error(
                                        "Error accessing configuration at {}: {}", source.toString(), e.getMessage());
                            }
                            throw new ConfigurationException("Unable to access " + source.toString(), e);
                        }
                    }
                    case NOT_AUTHORIZED: {
                        throw new ConfigurationException("Authorization failed");
                    }
                    default: {
                        if (code < 0) {
                            LOGGER.info("Invalid response code returned");
                        } else {
                            LOGGER.info("Unexpected response code returned {}", code);
                        }
                        throw new ConfigurationException("Unable to access " + source.toString());
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            LOGGER.warn("Error accessing {}: {}", source.toString(), e.getMessage());
            throw new ConfigurationException("Unable to access " + source.toString(), e);
        }
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

    public static class Result {

        private InputStream inputStream;
        private Status status;

        public Result() {}

        public Result(final Status status) {
            this.status = status;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public Status getStatus() {
            return status;
        }
    }
}
