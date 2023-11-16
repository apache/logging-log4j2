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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * Represents the source for the logging configuration as an immutable object.
 */
public class Source {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static String normalize(final File file) {
        try {
            return file.getCanonicalFile().getAbsolutePath();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static File toFile(final Path path) {
        try {
            return Objects.requireNonNull(path, "path").toFile();
        } catch (final UnsupportedOperationException e) {
            return null;
        }
    }

    // LOG4J2-3527 - Don't use Paths.get().
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The URI should be specified in a configuration file.")
    private static File toFile(final URI uri) {
        try {
            final String scheme = Objects.requireNonNull(uri, "uri").getScheme();
            if (Strings.isBlank(scheme) || scheme.equals("file")) {
                return new File(uri.getPath());
            } else {
                LOGGER.debug("uri does not represent a local file: " + uri);
                return null;
            }
        } catch (final Exception e) {
            LOGGER.debug("uri is malformed: " + uri.toString());
            return null;
        }
    }

    private static URI toURI(final URL url) {
        try {
            return Objects.requireNonNull(url, "url").toURI();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private final File file;
    private final URI uri;
    private final String location;

    /**
     * Constructs a Source from a ConfigurationSource.
     *
     * @param source The ConfigurationSource.
     */
    public Source(final ConfigurationSource source) {
        this.file = source.getFile();
        this.uri = source.getURI();
        this.location = source.getLocation();
    }

    /**
     * Constructs a new {@code Source} with the specified file.
     * file.
     *
     * @param file the file where the input stream originated
     */
    public Source(final File file) {
        this.file = Objects.requireNonNull(file, "file");
        this.location = normalize(file);
        this.uri = file.toURI();
    }

    /**
     * Constructs a new {@code Source} from the specified Path.
     *
     * @param path the Path where the input stream originated
     */
    public Source(final Path path) {
        final Path normPath = Objects.requireNonNull(path, "path").normalize();
        this.file = toFile(normPath);
        this.uri = normPath.toUri();
        this.location = normPath.toString();
    }

    /**
     * Constructs a new {@code Source} from the specified URI.
     *
     * @param uri the URI where the input stream originated
     */
    public Source(final URI uri) {
        final URI normUri = Objects.requireNonNull(uri, "uri").normalize();
        this.uri = normUri;
        this.location = normUri.toString();
        this.file = toFile(normUri);
    }

    /**
     * Constructs a new {@code Source} from the specified URI.
     *
     * @param uri the URI where the input stream originated
     * @param lastModified Not used.
     * @deprecated Use {@link Source#Source(URI)}.
     */
    @Deprecated
    public Source(final URI uri, final long lastModified) {
        this(uri);
    }

    /**
     * Constructs a new {@code Source} from the specified URL.
     *
     * @param url the URL where the input stream originated
     * @throws IllegalArgumentException if this URL is not formatted strictly according to RFC2396 and cannot be
     *         converted to a URI.
     */
    public Source(final URL url) {
        this.uri = toURI(url);
        this.location = uri.toString();
        this.file = toFile(uri);
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
        return Objects.equals(location, other.location);
    }

    /**
     * Gets the file configuration source, or {@code null} if this configuration source is based on an URL or has
     * neither a file nor an URL.
     *
     * @return the configuration source file, or {@code null}
     */
    public File getFile() {
        return file;
    }

    /**
     * Gets a string describing the configuration source file or URI, or {@code null} if this configuration source
     * has neither a file nor an URI.
     *
     * @return a string describing the configuration source file or URI, or {@code null}
     */
    public String getLocation() {
        return location;
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
        return file != null ? file.toPath() : uri != null ? Paths.get(uri) : Paths.get(location);
    }

    /**
     * Gets the configuration source URI, or {@code null} if this configuration source is based on a file or has
     * neither a file nor an URI.
     *
     * @return the configuration source URI, or {@code null}
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Gets the configuration source URL.
     *
     * @return the configuration source URI, or {@code null}
     */
    public URL getURL() {
        try {
            return uri.toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return location;
    }
}
