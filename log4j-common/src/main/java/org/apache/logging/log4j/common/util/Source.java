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
package org.apache.logging.log4j.common.util;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the source for the logging configuration as an immutable object.
 */
@NullMarked
public final class Source implements Serializable {

    private static final long serialVersionUID = 1L;

    private final @Nullable File file;
    private final URI uri;
    private final String location;

    /**
     * Constructs a new {@code Source} with the specified file.
     *
     * @param file the file where the input stream originated.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public Source(final File file) {
        this(requireNonNull(file, "file"), file.toURI(), file.getPath());
    }

    /**
     * Constructs a new {@code Source} from the specified Path.
     *
     * @param path the Path where the input stream originated
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public Source(final Path path) {
        final Path normPath = requireNonNull(path, "path").normalize();
        this.file = toFile(normPath);
        this.uri = normPath.toUri();
        this.location = normPath.toString();
    }

    /**
     * Constructs a new {@code Source} from the specified URI.
     *
     * @param uri the URI where the input stream originated
     * @throws NullPointerException if {@code uri} is {@code null}.
     */
    public Source(final URI uri) {
        final URI normUri = requireNonNull(uri, "uri").normalize();
        this.uri = normUri;
        this.location = normUri.toString();
        this.file = toFile(normUri);
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
        this(toUri(url));
    }

    /**
     * Constructs a new {@code Source} from its constituent parts.
     *
     * @param file the configuration source file, or {@code null}
     * @param uri the configuration source URI
     * @param location a string describing the configuration source file or URI
     * @throws NullPointerException if {@code uri} or {@code location} is {@code null}.
     */
    public Source(final @Nullable File file, final URI uri, final String location) {
        this.file = file;
        this.uri = requireNonNull(uri, "uri");
        this.location = requireNonNull(location, "location");
    }

    private static @Nullable File toFile(final Path path) {
        try {
            return requireNonNull(path, "path").toFile();
        } catch (final UnsupportedOperationException e) {
            return null;
        }
    }

    private static @Nullable File toFile(final URI uri) {
        try {
            final String scheme = requireNonNull(uri, "uri").getScheme();
            if (Strings.isBlank(scheme) || scheme.equals("file")) {
                return new File(uri.getPath());
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    private static URI toUri(final URL url) {
        try {
            return requireNonNull(url, "url").toURI();
        } catch (final java.net.URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
    public @Nullable File getFile() {
        return file;
    }

    /**
     * Gets a string describing the configuration source file or URI.
     *
     * @return a string describing the configuration source file or URI
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the configuration source URI.
     *
     * @return the configuration source URI
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Gets the configuration source URL.
     *
     * @return the configuration source URL
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
