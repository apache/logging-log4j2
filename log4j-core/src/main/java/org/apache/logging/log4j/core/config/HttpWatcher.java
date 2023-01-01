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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.AbstractWatcher;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.core.util.internal.HttpResponse;
import org.apache.logging.log4j.core.util.internal.HttpSourceLoader;
import org.apache.logging.log4j.core.util.internal.LastModifiedSource;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
@Namespace(Watcher.CATEGORY)
@Plugin("http")
@PluginAliases("https")
public class HttpWatcher extends AbstractWatcher {

    private final Logger LOGGER = StatusLogger.getLogger();

    private URL url;
    private final long lastModifiedMillis;
    private final HttpSourceLoader httpSourceLoader;
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    public HttpWatcher(final Configuration configuration, final Reconfigurable reconfigurable,
            final List<ConfigurationListener> configurationListeners, final long lastModifiedMillis) {
        super(configuration, reconfigurable, configurationListeners);
        this.lastModifiedMillis = lastModifiedMillis;
        this.httpSourceLoader = configuration.getInstance(HttpSourceLoader.class);
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
            final LastModifiedSource source = new LastModifiedSource(url.toURI(), lastModifiedMillis);
            final HttpResponse response = httpSourceLoader.load(source);
            switch (response.getStatus()) {
                case NOT_MODIFIED: {
                    LOGGER.debug("Configuration Not Modified");
                    return false;
                }
                case SUCCESS: {
                    final ConfigurationSource configSource = getConfiguration().getConfigurationSource();
                    try {
                        configSource.setData(response.getInputStream().readAllBytes());
                        configSource.setModifiedMillis(source.getLastModified());
                        LOGGER.debug("Content was modified for {}", url.toString());
                        return true;
                    } catch (final IOException e) {
                        LOGGER.error("Error accessing configuration at {}: {}", url, e.getMessage());
                        return false;
                    }
                }
                case NOT_FOUND: {
                    LOGGER.info("Unable to locate configuration at {}", url.toString());
                    return false;
                }
                default: {
                    LOGGER.warn("Unexpected error accessing configuration at {}", url.toString());
                    return false;
                }
            }
        } catch(final URISyntaxException ex) {
            LOGGER.error("Bad configuration URL: {}, {}", url.toString(), ex.getMessage());
            return false;
        }
    }
}
