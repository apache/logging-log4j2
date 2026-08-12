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
package org.apache.logging.log4j.core.util;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the source for the logging configuration as an immutable object.
 *
 * <p>Compatibility facade delegating storage to {@link org.apache.logging.log4j.common.util.Source}.</p>
 */
@NullMarked
public class Source {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static String normalize(final File file) {
        try {
            return file.getCanonicalFile().getAbsolutePath();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static @Nullable File toFile(Path path) {
        try {
            return requireNonNull(path, "path").toFile();
        } catch (final UnsupportedOperationException e) {
            return null;
        }
    }

    // LOG4J2-3527 - Don't use Paths.get().
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The URI should be specified in a configuration file.")
    private static @Nullable File toFile(URI uri) {
        try {
            final String scheme = requireNonNull(uri, "uri").getScheme();
            if (Strings.isBlank(scheme) || scheme.equals("file")) {
                return new File(uri.getPath());
            } else {
                LOGGER.debug("uri does not represent a local file: " + uri);
                return null;
            }
        } catch (final Exception e) {
            LOGGER.debug("uri is malformed: " + uri);
            return null;
        }
    }

    private static URI toURI(final URL url) {
        try {
            return requireNonNull(url, "url").toURI();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private final org.apache.logging.log4j.common.util.Source delegate;

    /**
     * Constructs a Source from a ConfigurationSource.
     *
     * @param source The ConfigurationSource.
     * @throws NullPointerException if {@code source} is {@code null}.
     */
    public Source(final ConfigurationSource source) {
        this(new org.apache.logging.log4j.common.util.Source(
                source.getFile(), requireNonNull(source.getURI()), requireNonNull(source.getLocation())));
    }

    private Source(final org.apache.logging.log4j.common.util.Source delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a new {@code Source} with the specified file.
     * file.
     *
     * @param file the file where the input stream originated.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public Source(final File file) {
        this(new org.apache.logging.log4j.common.util.Source(
                requireNonNull(file, "file"), file.toURI(), normalize(file)));
    }

    /**
     * Constructs a new {@code Source} from the specified Path.
     *
     * @param path the Path where the input stream originated
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public Source(final Path path) {
        delegate = new org.apache.logging.log4j.common.util.Source(path);
    }

    /**
     * Constructs a new {@code Source} from the specified URI.
     *
     * @param uri the URI where the input stream originated
     * @throws NullPointerException if {@code uri} is {@code null}.
     */
    public Source(final URI uri) {
        delegate = new org.apache.logging.log4j.common.util.Source(uri);
    }

    /**
     * Constructs a new {@code Source} from the specified URI.
     *
     * @param uri the URI where the input stream originated
     * @param ignored Not used.
     * @deprecated Use {@link Source#Source(URI)}.
     * @throws NullPointerException if {@code uri} is {@code null}.
     */
    @Deprecated
    public Source(URI uri, long ignored) {
        this(uri);
    }

    /**
     * Constructs a new {@code Source} from the specified URL.
     *
     * @param url the URL where the input stream originated
     * @throws NullPointerException if this URL is {@code null}.
     * @throws IllegalArgumentException if this URL is not formatted strictly according to RFC2396 and cannot be
     *         converted to a URI.
     */
    public Source(final URL url) {
        delegate = new org.apache.logging.log4j.common.util.Source(url);
    }

    /**
     * Returns the shared {@link org.apache.logging.log4j.common.util.Source} delegate.
     *
     * @return the common source delegate
     * @since 3.0.0
     */
    public org.apache.logging.log4j.common.util.Source asCommonSource() {
        return delegate;
    }

    /**
     * Creates a core {@code Source} from a {@link org.apache.logging.log4j.common.util.Source}.
     *
     * @param source the common source
     * @return a core source facade
     * @since 3.0.0
     */
    public static Source fromCommon(final org.apache.logging.log4j.common.util.Source source) {
        return new Source(requireNonNull(source, "source"));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Source)) {
            return false;
        }
        final Source other = (Source) obj;
        return delegate.equals(other.delegate);
    }

    /**
     * Gets the file configuration source, or {@code null} if this configuration source is based on an URL or has
     * neither a file nor an URL.
     *
     * @return the configuration source file, or {@code null}
     */
    public @Nullable File getFile() {
        return delegate.getFile();
    }

    /**
     * Gets a string describing the configuration source file or URI, or {@code null} if this configuration source
     * has neither a file nor an URI.
     *
     * @return a string describing the configuration source file or URI, or {@code null}
     */
    public String getLocation() {
        return delegate.getLocation();
    }

    /**
     * Gets this source as a Path.
     *
     * @return this source as a Path.
     */
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The `file`, `uri` and `location` fields come from Log4j properties.")
    public Path getPath() {
        final File file = delegate.getFile();
        return file != null ? file.toPath() : Paths.get(delegate.getURI());
    }

    /**
     * Gets the configuration source URI, or {@code null} if this configuration source is based on a file or has
     * neither a file nor an URI.
     *
     * @return the configuration source URI, or {@code null}
     */
    public URI getURI() {
        return delegate.getURI();
    }

    /**
     * Gets the configuration source URL.
     *
     * @return the configuration source URI, or {@code null}
     */
    public URL getURL() {
        return delegate.getURL();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
