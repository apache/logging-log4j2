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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

public class ConfigurationResolver {
    private final Logger logger = StatusLogger.getLogger();
    private final UrlConnectionFactory urlConnectionFactory;

    @Inject
    public ConfigurationResolver(final UrlConnectionFactory urlConnectionFactory) {
        this.urlConnectionFactory = urlConnectionFactory;
    }

    public ConfigurationSource resolve(final URL url) throws IOException, URISyntaxException {
        final URLConnection urlConnection = urlConnectionFactory.openConnection(url);
        urlConnection.connect();
        try {
            final InputStream inputStream = urlConnection.getInputStream();
            final File file = FileUtils.fileFromUri(url.toURI());
            if (file != null) {
                return new ConfigurationSource(inputStream, file);
            }
            return new ConfigurationSource(inputStream, url, urlConnection.getLastModified(), this);
        } finally {
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).disconnect();
            }
        }
    }

    public Optional<ConfigurationSource> tryResolve(final URL url) {
        try {
            return Optional.of(resolve(url));
        } catch (final IOException | URISyntaxException e) {
            logger.warn("Error accessing {} due to {}, ignoring.", url, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ConfigurationSource> tryResolve(final String resource, final ClassLoader classLoader) {
        return Optional.ofNullable(Loader.getResource(resource, classLoader))
                .flatMap(this::tryResolve);
    }

    public Optional<ConfigurationSource> tryResolve(final URI uri) {
        final File configFile = FileUtils.fileFromUri(uri);
        if (configFile != null && configFile.exists() && configFile.canRead()) {
            try {
                return Optional.of(new ConfigurationSource(new FileInputStream(configFile), configFile));
            } catch (final FileNotFoundException e) {
                logger.error("Cannot locate file {}", configFile);
            }
        }
        if (ConfigurationFactory.isClassLoaderUri(uri)) {
            final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
            final String path = ConfigurationFactory.extractClassLoaderUriPath(uri);
            return tryResolve(path, loader);
        }
        if (!uri.isAbsolute()) { // LOG4J2-704 avoid confusing error message thrown by uri.toURL()
            logger.error("File not found in file system or classpath: {}", uri);
            return Optional.empty();
        }
        try {
            return tryResolve(uri.toURL());
        } catch (final MalformedURLException e) {
            logger.error("Invalid configuration URL: {}", uri, e);
            return Optional.empty();
        }
    }
}
