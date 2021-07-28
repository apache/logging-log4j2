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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

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
    public static final ConfigurationSource COMPOSITE_SOURCE = new ConfigurationSource(Constants.EMPTY_BYTE_ARRAY, null, 0);
    private static final String HTTPS = "https";

    private final File file;
    private final URL url;
    private final String location;
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
     * @param stream the input stream
     * @param file the file where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final File file) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.file = Objects.requireNonNull(file, "file is null");
        this.location = file.getAbsolutePath();
        this.url = null;
        this.data = null;
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
     * url.
     *
     * @param stream the input stream
     * @param url the URL where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final URL url) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.url = Objects.requireNonNull(url, "URL is null");
        this.location = url.toString();
        this.file = null;
        this.data = null;
        this.lastModified = 0;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * url.
     *
     * @param stream the input stream
     * @param url the URL where the input stream originated
     * @param lastModified when the source was last modified.
     */
    public ConfigurationSource(final InputStream stream, final URL url, long lastModified) {
        this.stream = Objects.requireNonNull(stream, "stream is null");
        this.url = Objects.requireNonNull(url, "URL is null");
        this.location = url.toString();
        this.file = null;
        this.data = null;
        this.lastModified = lastModified;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream. Since the stream is the only source
     * of data, this constructor makes a copy of the stream contents.
     *
     * @param stream the input stream
     * @throws IOException if an exception occurred reading from the specified stream
     */
    public ConfigurationSource(final InputStream stream) throws IOException {
        this(toByteArray(stream), null, 0);
    }

    public ConfigurationSource(final Source source, final byte[] data, long lastModified) throws IOException {
        Objects.requireNonNull(source, "source is null");
        this.data = Objects.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.file = source.getFile();
        this.url = source.getURI().toURL();
        this.location = source.getLocation();
        this.lastModified = lastModified;
    }

    private ConfigurationSource(final byte[] data, final URL url, long lastModified) {
        this.data = Objects.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.file = null;
        this.url = url;
        this.location = null;
        this.lastModified = lastModified;
        if ( url == null ) {
        	this.data = data;
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
        return file;
    }

    /**
     * Returns the configuration source URL, or {@code null} if this configuration source is based on a file or has
     * neither a file nor an URL.
     *
     * @return the configuration source URL, or {@code null}
     */
    public URL getURL() {
        return url;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setModifiedMillis(long modifiedMillis) {
        this.modifiedMillis = modifiedMillis;
    }

    /**
     * Returns a URI representing the configuration resource or null if it cannot be determined.
     * @return The URI.
     */
    public URI getURI() {
        URI sourceURI = null;
        if (url != null) {
            try {
                sourceURI = url.toURI();
            } catch (final URISyntaxException ex) {
                    /* Ignore the exception */
            }
        }
        if (sourceURI == null && file != null) {
            sourceURI = file.toURI();
        }
        if (sourceURI == null && location != null) {
            try {
                sourceURI = new URI(location);
            } catch (final URISyntaxException ex) {
                // Assume the scheme was missing.
                try {
                    sourceURI = new URI("file://" + location);
                } catch (final URISyntaxException uriEx) {
                    /* Ignore the exception */
                }
            }
        }
        return sourceURI;
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
        return location;
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
        if (source != null) {
            return new ConfigurationSource(source, data, this.lastModified);
        } else if (file != null) {
            return new ConfigurationSource(new FileInputStream(file), file);
        } else if (url != null && data != null) {
            // Creates a ConfigurationSource without accessing the URL since the data was provided.
            return new ConfigurationSource(data, url, modifiedMillis == 0 ? lastModified : modifiedMillis);
        } else if (url != null) {
            return fromUri(getURI());
        } else if (data != null) {
            return new ConfigurationSource(data, null, lastModified);
        }
        return null;
    }

    @Override
    public String toString() {
        if (location != null) {
            return location;
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
            ConfigurationFactory.LOGGER.error("File not found in file system or classpath: {}", configLocation.toString());
            return null;
        }
        try {
            URL url = configLocation.toURL();
            URLConnection urlConnection = UrlConnectionFactory.createConnection(url);
            InputStream is = urlConnection.getInputStream();
            long lastModified = urlConnection.getLastModified();
            return new ConfigurationSource(is, configLocation.toURL(), lastModified);
        } catch (final FileNotFoundException ex) {
            ConfigurationFactory.LOGGER.warn("Could not locate file {}", configLocation.toString());
        } catch (final MalformedURLException ex) {
            ConfigurationFactory.LOGGER.error("Invalid URL {}", configLocation.toString(), ex);
        } catch (final Exception ex) {
            ConfigurationFactory.LOGGER.error("Unable to access {}", configLocation.toString(), ex);
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

    private static ConfigurationSource getConfigurationSource(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();
            AuthorizationProvider provider = ConfigurationFactory.authorizationProvider(PropertiesUtil.getProperties());
            provider.addAuthorization(urlConnection);
            if (url.getProtocol().equals(HTTPS)) {
                SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration();
                if (sslConfiguration != null) {
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
                    if (!sslConfiguration.isVerifyHostName()) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
                    }
                }
            }
            File file = FileUtils.fileFromUri(url.toURI());
            try {
                if (file != null) {
                    return new ConfigurationSource(urlConnection.getInputStream(), FileUtils.fileFromUri(url.toURI()));
                } else {
                    return new ConfigurationSource(urlConnection.getInputStream(), url, urlConnection.getLastModified());
                }
            } catch (FileNotFoundException ex) {
                ConfigurationFactory.LOGGER.info("Unable to locate file {}, ignoring.", url.toString());
                return null;
            }
        } catch (IOException | URISyntaxException ex) {
            ConfigurationFactory.LOGGER.warn("Error accessing {} due to {}, ignoring.", url.toString(),
                    ex.getMessage());
            return null;
        }
    }
}
