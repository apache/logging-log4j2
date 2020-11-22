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

package org.apache.logging.log4j.core.util;

import java.io.File;
import java.net.URI;
import java.util.Objects;

import org.apache.logging.log4j.core.config.ConfigurationSource;

/**
 * Represents the source for the logging configuration.
 */
public class Source {

    /**
     * Captures a URI or File.
     */

    private final File file;
    private final URI uri;
    private final String location;

    /**
     * Constructs a Source from a ConfigurationSource.
     * @param source The ConfigurationSource.
     */
    public Source(ConfigurationSource source) {
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
        this.file = Objects.requireNonNull(file, "file is null");
        this.location = file.getAbsolutePath();
        this.uri = null;
    }

    /**
     * Constructs a new {@code Source} from the specified URI.
     *
     * @param uri the URL where the input stream originated
     */
    public Source(final URI uri, final long lastModified) {
        this.uri = Objects.requireNonNull(uri, "URI is null");
        this.location = uri.toString();
        this.file = null;
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
    public URI getURI() {
        return uri;
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

    @Override
    public String toString() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Source)) {
            return false;
        }
        Source source = (Source) o;
        return Objects.equals(location, source.location);
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(location);
    }
}
