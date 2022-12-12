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
package org.apache.logging.log4j.core.util.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.status.StatusLogger;

public class HttpSourceLoader {
    private static final int NOT_MODIFIED = 304;
    private static final int NOT_AUTHORIZED = 401;
    private static final int NOT_FOUND = 404;
    private static final int OK = 200;
    private final Logger logger = StatusLogger.getLogger();
    private final UrlConnectionFactory urlConnectionFactory;

    @Inject
    public HttpSourceLoader(final UrlConnectionFactory urlConnectionFactory) {
        this.urlConnectionFactory = urlConnectionFactory;
    }

    public HttpResponse load(final LastModifiedSource source) {
        final long lastModified = source.getLastModified();
        try {
            final HttpURLConnection connection = urlConnectionFactory.openConnection(source.getURL(), lastModified);
            connection.connect();
            try {
                int code = connection.getResponseCode();
                switch (code) {
                    case NOT_MODIFIED: {
                        logger.debug("Configuration not modified");
                        return new HttpResponse(Status.NOT_MODIFIED);
                    }
                    case NOT_FOUND: {
                        logger.debug("Unable to access {}: Not Found", source);
                        return new HttpResponse(Status.NOT_FOUND);
                    }
                    case OK: {
                        try (InputStream is = connection.getInputStream()) {
                            source.setLastModified(connection.getLastModified());
                            logger.debug("Content was modified for {}. previous lastModified: {}, new lastModified: {}",
                                    source, lastModified, connection.getLastModified());
                            return new HttpResponse(Status.SUCCESS, new ByteArrayInputStream(is.readAllBytes()));
                        } catch (final IOException e) {
                            try (InputStream es = connection.getErrorStream()) {
                                logger.info("Error accessing configuration at {}: {}", source, es.readAllBytes());
                            } catch (final IOException ioe) {
                                logger.error("Error accessing configuration at {}: {}", source, e.getMessage());
                            }
                            throw new ConfigurationException("Unable to access " + source, e);
                        }
                    }
                    case NOT_AUTHORIZED: {
                        throw new ConfigurationException("Authorization failed");
                    }
                    default: {
                        if (code < 0) {
                            logger.info("Invalid response code returned");
                        } else {
                            logger.info("Unexpected response code returned {}", code);
                        }
                        throw new ConfigurationException("Unable to access " + source.toString());
                    }
                }
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            logger.warn("Error accessing {}: {}", source, e.getMessage());
            throw new ConfigurationException("Unable to access " + source, e);
        }
    }
}
