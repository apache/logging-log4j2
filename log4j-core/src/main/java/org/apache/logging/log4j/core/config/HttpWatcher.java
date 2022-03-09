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
package org.apache.logging.log4j.core.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AbstractWatcher;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
@Plugin(name = "http", category = Watcher.CATEGORY, elementType = Watcher.ELEMENT_TYPE, printObject = true)
@PluginAliases("https")
public class HttpWatcher extends AbstractWatcher {

    private final Logger LOGGER = StatusLogger.getLogger();

    private final SslConfiguration sslConfiguration;
    private URL url;
    private volatile long lastModifiedMillis;
    private static final int NOT_MODIFIED = 304;
    private static final int OK = 200;
    private static final int BUF_SIZE = 1024;
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    public HttpWatcher(final Configuration configuration, final Reconfigurable reconfigurable,
                       final List<ConfigurationListener> configurationListeners, final long lastModifiedMillis) {
        super(configuration, reconfigurable, configurationListeners);
        sslConfiguration = SslConfigurationFactory.getSslConfiguration();
        this.lastModifiedMillis = lastModifiedMillis;
    }

    @Override
    public long getLastModified() {
        return lastModifiedMillis;
    }

    @Override
    public boolean isModified() {
        return refreshConfiguration();
    }

    @Override
    public void watching(final Source source) {
        if (!source.getURI().getScheme().equals(HTTP) && !source.getURI().getScheme().equals(HTTPS)) {
            throw new IllegalArgumentException(
                "HttpWatcher requires a url using the HTTP or HTTPS protocol, not " + source.getURI().getScheme());
        }
        try {
            url = source.getURI().toURL();
        } catch (final MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid URL for HttpWatcher " + source.getURI(), ex);
        }
        super.watching(source);
    }

    @Override
    public Watcher newWatcher(final Reconfigurable reconfigurable, final List<ConfigurationListener> listeners,
           final long lastModifiedMillis) {
        final HttpWatcher watcher = new HttpWatcher(getConfiguration(), reconfigurable, listeners, lastModifiedMillis);
        if (getSource() != null) {
            watcher.watching(getSource());
        }
        return watcher;
    }

    private boolean refreshConfiguration() {
        try {
            final HttpURLConnection urlConnection = UrlConnectionFactory.createConnection(url, lastModifiedMillis,
                sslConfiguration);
            urlConnection.connect();

            try {
                final int code = urlConnection.getResponseCode();
                switch (code) {
                    case NOT_MODIFIED: {
                        LOGGER.debug("Configuration Not Modified");
                        return false;
                    }
                    case OK: {
                        try (final InputStream is = urlConnection.getInputStream()) {
                            final ConfigurationSource configSource = getConfiguration().getConfigurationSource();
                            configSource.setData(readStream(is));
                            lastModifiedMillis = urlConnection.getLastModified();
                            configSource.setModifiedMillis(lastModifiedMillis);
                            LOGGER.debug("Content was modified for {}", url.toString());
                            return true;
                        } catch (final IOException e) {
                            try (final InputStream es = urlConnection.getErrorStream()) {
                                LOGGER.info("Error accessing configuration at {}: {}", url, readStream(es));
                            } catch (final IOException ioe) {
                                LOGGER.error("Error accessing configuration at {}: {}", url, e.getMessage());
                            }
                            return false;
                        }
                    }
                    default: {
                        if (code < 0) {
                            LOGGER.info("Invalid response code returned");
                        } else {
                            LOGGER.info("Unexpected response code returned {}", code);
                        }
                        return false;
                    }
                }
            } catch (final IOException ioe) {
                LOGGER.error("Error accessing configuration at {}: {}", url, ioe.getMessage());
            } finally {
                urlConnection.disconnect();
            }
        } catch (final IOException ioe) {
            LOGGER.error("Error connecting to configuration at {}: {}", url, ioe.getMessage());
        }
        return false;
    }

    private byte[] readStream(final InputStream is) throws IOException {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();
        final byte[] buffer = new byte[BUF_SIZE];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }
}
