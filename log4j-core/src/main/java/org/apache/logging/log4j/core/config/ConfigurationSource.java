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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.logging.log4j.core.util.Source;

/**
 * Represents the source for the logging configuration.
 */
public class ConfigurationSource {

    /**
     * ConfigurationSource to use with Configurations that do not require a "real" configuration source.
     */
    public static final ConfigurationSource NULL_SOURCE = new ConfigurationSource(new byte[0], null, 0, null);
    /**
     * ConfigurationSource to use with {@link org.apache.logging.log4j.core.config.composite.CompositeConfiguration}.
     */
    public static final ConfigurationSource COMPOSITE_SOURCE = new ConfigurationSource(new byte[0], null, 0, null);

    private final InputStream stream;
    private volatile byte[] data;
    private final Source source;
    // Set when using a URL-based configuration for reloading
    private final ConfigurationResolver configurationResolver;
    private final long lastModified;
    // Set when the configuration has been updated so reset can use it for the next lastModified timestamp.
    private volatile long modifiedMillis;

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * file.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @param file the file where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final File file) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.data = null;
        this.source = new Source(file);
        this.configurationResolver = null;
        long modified = 0;
        try {
            modified = file.lastModified();
        } catch (final Exception ex) {
            // There is a problem with the file. It will be handled somewhere else.
        }
        this.lastModified = modified;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * path.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @param path the path where the input stream originated.
     */
    public ConfigurationSource(final InputStream stream, final Path path) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.data = null;
        this.source = new Source(path);
        this.configurationResolver = null;
        long modified = 0;
        try {
            modified = Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ex) {
            // There is a problem with the file. It will be handled somewhere else.
        }
        this.lastModified = modified;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * url.
     *
     * @param stream the input stream
     * @param url the URL where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final URL url) {
        this(stream, url, null);
    }

    public ConfigurationSource(final InputStream stream, final URL url, final ConfigurationResolver configurationResolver) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.data = null;
        this.lastModified = 0;
        this.source = new Source(url);
        this.configurationResolver = configurationResolver;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * url.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @param url the URL where the input stream originated
     * @param lastModified when the source was last modified.
     */
    public ConfigurationSource(final InputStream stream, final URL url, long lastModified) {
        this(stream, url, lastModified, null);
    }

    public ConfigurationSource(final InputStream stream, final URL url, long lastModified,
                               final ConfigurationResolver configurationResolver) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.data = null;
        this.lastModified = lastModified;
        this.source = new Source(url);
        this.configurationResolver = configurationResolver;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream. Since the stream is the only source
     * of data, this constructor makes a copy of the stream contents.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @throws IOException if an exception occurred reading from the specified stream
     */
    public ConfigurationSource(final InputStream stream) throws IOException {
        this(stream.readAllBytes(), null, 0, null);
    }

    public ConfigurationSource(final Source source, final byte[] data, final long lastModified) throws IOException {
        Objects.requireNonNull(source, "source is null");
        this.data = Objects.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.lastModified = lastModified;
        this.source = source;
        this.configurationResolver = null;
    }

    private ConfigurationSource(final byte[] data, final URL url, final long lastModified, final ConfigurationResolver configurationResolver) {
        this.data = Objects.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.lastModified = lastModified;
        this.source = url == null ? null : new Source(url);
        this.configurationResolver = configurationResolver;
    }

    /**
     * Returns the file configuration source, or {@code null} if this configuration source is based on an URL or has
     * neither a file nor an URL.
     *
     * @return the configuration source file, or {@code null}
     */
    public File getFile() {
        return source == null ? null : source.getFile();
    }

    private boolean isFile() {
        return source != null && source.getFile() != null;
    }

    private boolean isURL() {
        return source != null && source.getURI() != null;
    }

    private boolean isLocation() {
        return source != null && source.getLocation() != null;
    }

    /**
     * Returns the configuration source URL, or {@code null} if this configuration source is based on a file or has
     * neither a file nor an URL.
     *
     * @return the configuration source URL, or {@code null}
     */
    public URL getURL() {
        return source == null ? null : source.getURL();
    }

    public void setData(final byte[] data) {
        this.data = data;
    }

    public void setModifiedMillis(final long modifiedMillis) {
        this.modifiedMillis = modifiedMillis;
    }

    /**
     * Returns a URI representing the configuration resource or null if it cannot be determined.
     * @return The URI.
     */
    public URI getURI() {
        return source == null ? null : source.getURI();
    }

    /**
     * Returns the time the resource was last modified or 0 if it is not available.
     * @return the last modified time of the resource.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Returns a string describing the configuration source file or URL, or {@code null} if this configuration source
     * has neither a file nor an URL.
     *
     * @return a string describing the configuration source file or URL, or {@code null}
     */
    public String getLocation() {
        return source == null ? null : source.getLocation();
    }

    /**
     * Returns the input stream that this configuration source was constructed with.
     *
     * @return the input stream that this configuration source was constructed with.
     */
    public InputStream getInputStream() {
        return stream;
    }

    /**
     * Returns a new {@code ConfigurationSource} whose input stream is reset to the beginning.
     *
     * @return a new {@code ConfigurationSource}
     * @throws IOException if a problem occurred while opening the new input stream
     */
    public ConfigurationSource resetInputStream() throws IOException {
        if (source != null && data != null) {
            return new ConfigurationSource(source, data, this.lastModified);
        } else if (isFile()) {
            return new ConfigurationSource(new FileInputStream(getFile()), getFile());
        } else if (isURL() && data != null) {
            // Creates a ConfigurationSource without accessing the URL since the data was provided.
            return new ConfigurationSource(data, getURL(), modifiedMillis == 0 ? lastModified : modifiedMillis, configurationResolver);
        } else if (isURL()) {
            return configurationResolver.tryResolve(getURI()).orElse(null);
        } else if (data != null) {
            return new ConfigurationSource(data, null, lastModified, configurationResolver);
        }
        return null;
    }

    @Override
    public String toString() {
        if (isLocation()) {
            return getLocation();
        }
        if (this == NULL_SOURCE) {
            return "NULL_SOURCE";
        }
        if (this == COMPOSITE_SOURCE) {
            return "COMPOSITE_SOURCE";
        }
        final int length = data == null ? -1 : data.length;
        return "stream (" + length + " bytes, unknown location)";
    }

}
