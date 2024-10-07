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
package org.apache.logging.log4j.core.config;

import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.CoreProperties.AuthenticationProperties;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AbstractWatcher;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.core.util.internal.HttpInputStreamUtil;
import org.apache.logging.log4j.core.util.internal.LastModifiedSource;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
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

    private final PropertyEnvironment properties;
    private final SslConfiguration sslConfiguration;
    private final AuthorizationProvider authorizationProvider;
    private URL url;
    private volatile long lastModifiedMillis;
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    public HttpWatcher(
            final Configuration configuration,
            final Reconfigurable reconfigurable,
            final List<Consumer<Reconfigurable>> configurationListeners,
            final long lastModifiedMillis) {
        super(configuration, reconfigurable, configurationListeners);
        properties = configuration.getEnvironment();
        sslConfiguration = SslConfigurationFactory.getSslConfiguration(properties);
        authorizationProvider =
                AuthorizationProvider.getAuthorizationProvider(properties.getProperty(AuthenticationProperties.class));
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
        if (!source.getURI().getScheme().equals(HTTP)
                && !source.getURI().getScheme().equals(HTTPS)) {
            throw new IllegalArgumentException("HttpWatcher requires a url using the HTTP or HTTPS protocol, not "
                    + source.getURI().getScheme());
        }
        try {
            url = source.getURI().toURL();
        } catch (final MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid URL for HttpWatcher " + source.getURI(), ex);
        }
        super.watching(source);
    }

    @Override
    public Watcher newWatcher(
            final Reconfigurable reconfigurable,
            final List<Consumer<Reconfigurable>> listeners,
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
            final HttpInputStreamUtil.Result result =
                    HttpInputStreamUtil.getInputStream(source, properties, authorizationProvider, sslConfiguration);
            // Update lastModifiedMillis
            // https://github.com/apache/logging-log4j2/issues/2937
            lastModifiedMillis = source.getLastModified();
            // The result of the HTTP/HTTPS request is already logged at `DEBUG` by `HttpInputStreamUtil`
            // We only log the important events at `INFO` or more.
            switch (result.getStatus()) {
                case NOT_MODIFIED: {
                    return false;
                }
                case SUCCESS: {
                    final ConfigurationSource configSource = getConfiguration().getConfigurationSource();
                    try {
                        // In this case `result.getInputStream()` is not null.
                        configSource.setData(
                                requireNonNull(result.getInputStream()).readAllBytes());
                        configSource.setModifiedMillis(source.getLastModified());
                        LOGGER.info(
                                "{} resource at {} was modified on {}",
                                () -> toRootUpperCase(url.getProtocol()),
                                () -> url.toExternalForm(),
                                () -> Instant.ofEpochMilli(source.getLastModified()));
                        return true;
                    } catch (final IOException e) {
                        // Dead code since result.getInputStream() is a ByteArrayInputStream
                        LOGGER.error("Error accessing configuration at {}", url.toExternalForm(), e);
                        return false;
                    }
                }
                case NOT_FOUND: {
                    LOGGER.warn(
                            "{} resource at {} was not found",
                            () -> toRootUpperCase(url.getProtocol()),
                            () -> url.toExternalForm());
                    return false;
                }
                default: {
                    LOGGER.warn(
                            "Unexpected error retrieving {} resource at {}",
                            () -> toRootUpperCase(url.getProtocol()),
                            () -> url.toExternalForm());
                    return false;
                }
            }
        } catch (final URISyntaxException ex) {
            LOGGER.error("Bad configuration file URL {}", url.toExternalForm(), ex);
            return false;
        }
    }
}
