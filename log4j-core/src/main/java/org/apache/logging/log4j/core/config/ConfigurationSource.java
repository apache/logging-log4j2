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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Represents the source for the logging configuration.
 */
public class ConfigurationSource {

    /**
     * ConfigurationSource to use with Configurations that do not require a "real" configuration source.
     */
    public static final ConfigurationSource NULL_SOURCE = new ConfigurationSource(Constants.EMPTY_BYTE_ARRAY, null, 0);

    /**
     * ConfigurationSource to use with {@link org.apache.logging.log4j.core.config.composite.CompositeConfiguration}.
     */
    public static final ConfigurationSource COMPOSITE_SOURCE =
            new ConfigurationSource(Constants.EMPTY_BYTE_ARRAY, null, 0);

    private final InputStream stream;
    private volatile byte[] data;
    private volatile Source source;
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
        long modified = 0;
        try {
            modified = file.lastModified();
        } catch (Exception ex) {
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
     * URL.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @param url the URL where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final URL url) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.data = null;
        this.lastModified = 0;
        this.source = new Source(url);
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * URL.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @param url the URL where the input stream originated
     * @param lastModified when the source was last modified.
     */
    public ConfigurationSource(final InputStream stream, final URL url, final long lastModified) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.data = null;
        this.lastModified = lastModified;
        this.source = new Source(url);
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream. Since the stream is the only source
     * of data, this constructor makes a copy of the stream contents.
     *
     * @param stream the input stream, the caller is responsible for closing this resource.
     * @throws IOException if an exception occurred reading from the specified stream
     */
    public ConfigurationSource(final InputStream stream) throws IOException {
        this(toByteArray(stream), null, 0);
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified source.
     *
     * @param source a Source.
     * @param data data from the source
     * @param lastModified when the source was last modified.
     */
    public ConfigurationSource(final Source source, final byte[] data, final long lastModified) {
        Objects.requireNonNull(source, "source is null");
        this.data = Objects.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.lastModified = lastModified;
        this.source = source;
    }

    private ConfigurationSource(final byte[] data, final URL url, final long lastModified) {
        this.data = Objects.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.lastModified = lastModified;
        if (url == null) {
            this.data = data;
        } else {
            this.source = new Source(url);
        }
    }

    /**
     * Returns the contents of the specified {@code InputStream} as a byte array.
     *
     * @param inputStream the stream to read
     * @return the contents of the specified stream
     * @throws IOException if a problem occurred reading from the stream
     */
    private static byte[] toByteArray(final InputStream inputStream) throws IOException {
        final int buffSize = Math.max(4096, inputStream.available());
        final ByteArrayOutputStream contents = new ByteArrayOutputStream(buffSize);
        final byte[] buff = new byte[buffSize];

        int length = inputStream.read(buff);
        while (length > 0) {
            contents.write(buff, 0, length);
            length = inputStream.read(buff);
        }
        return contents.toByteArray();
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
        return source == null ? false : source.getFile() != null;
    }

    private boolean isURL() {
        return source == null ? false : source.getURI() != null;
    }

    private boolean isLocation() {
        return source == null ? false : source.getLocation() != null;
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

    /**
     * @deprecated Not used internally, no replacement. TODO remove and make source final.
     */
    @Deprecated
    public void setSource(final Source source) {
        this.source = source;
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
            return new ConfigurationSource(data, getURL(), modifiedMillis == 0 ? lastModified : modifiedMillis);
        } else if (isURL()) {
            return fromUri(getURI());
        } else if (data != null) {
            return new ConfigurationSource(data, null, lastModified);
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
        final int length = data == null ? -1 : data.length;
        return "stream (" + length + " bytes, unknown location)";
    }

    /**
     * Loads the configuration from a URI.
     * @param configLocation A URI representing the location of the configuration.
     * @return The ConfigurationSource for the configuration.
     */
    public static ConfigurationSource fromUri(final URI configLocation) {
        final File configFile = FileUtils.fileFromUri(configLocation);
        if (configFile != null && configFile.exists() && configFile.canRead()) {
            try {
                return new ConfigurationSource(new FileInputStream(configFile), configFile);
            } catch (final FileNotFoundException ex) {
                ConfigurationFactory.LOGGER.error("Cannot locate file {}", configLocation.getPath(), ex);
            }
        }
        if (ConfigurationFactory.isClassLoaderUri(configLocation)) {
            final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
            final String path = ConfigurationFactory.extractClassLoaderUriPath(configLocation);
            return fromResource(path, loader);
        }
        if (!configLocation.isAbsolute()) { // LOG4J2-704 avoid confusing error message thrown by uri.toURL()
            ConfigurationFactory.LOGGER.error(
                    "File not found in file system or classpath: {}", configLocation.toString());
            return null;
        }
        try {
            return getConfigurationSource(configLocation.toURL());
        } catch (final MalformedURLException ex) {
            ConfigurationFactory.LOGGER.error("Invalid URL {}", configLocation.toString(), ex);
        }
        return null;
    }

    /**
     * Retrieves the configuration via the ClassLoader.
     * @param resource The resource to load.
     * @param loader The default ClassLoader to use.
     * @return The ConfigurationSource for the configuration.
     */
    public static ConfigurationSource fromResource(final String resource, final ClassLoader loader) {
        final URL url = Loader.getResource(resource, loader);
        if (url == null) {
            return null;
        }
        return getConfigurationSource(url);
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The name of the accessed files is based on a configuration value.")
    private static ConfigurationSource getConfigurationSource(final URL url) {
        try {
            final File file = FileUtils.fileFromUri(url.toURI());
            final URLConnection urlConnection = UrlConnectionFactory.createConnection(url);
            try {
                if (file != null) {
                    return new ConfigurationSource(urlConnection.getInputStream(), FileUtils.fileFromUri(url.toURI()));
                } else if (urlConnection instanceof JarURLConnection) {
                    // Work around https://bugs.openjdk.java.net/browse/JDK-6956385.
                    URL jarFileUrl = ((JarURLConnection) urlConnection).getJarFileURL();
                    File jarFile = new File(jarFileUrl.getFile());
                    long lastModified = jarFile.lastModified();
                    return new ConfigurationSource(urlConnection.getInputStream(), url, lastModified);
                } else {
                    return new ConfigurationSource(
                            urlConnection.getInputStream(), url, urlConnection.getLastModified());
                }
            } catch (FileNotFoundException ex) {
                ConfigurationFactory.LOGGER.info("Unable to locate file {}, ignoring.", url.toString());
                return null;
            }
        } catch (IOException | URISyntaxException ex) {
            ConfigurationFactory.LOGGER.warn(
                    "Error accessing {} due to {}, ignoring.", url.toString(), ex.getMessage());
            return null;
        }
    }
}
