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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.logging.log4j.core.util.Assert;

/**
 * Represents the source for the logging configuration.
 */
public class ConfigurationSource {
    public static final ConfigurationSource NULL_SOURCE = new ConfigurationSource(new byte[0]);

    private final File file;
    private final URL url;
    private final String location;
    private final InputStream stream;
    private final byte[] data;

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
     * Constructs a new {@code ConfigurationSource} with the specified input stream. Since the stream is the only source
     * of data, this constructor makes a copy of the stream contents.
     * 
     * @param stream the input stream
     * @throws IOException if an exception occurred reading from the specified stream
     */
    public ConfigurationSource(final InputStream stream) throws IOException {
        this(toByteArray(stream));
    }

    private ConfigurationSource(final byte[] data) {
        this.data = Assert.requireNonNull(data, "data is null");
        this.stream = new ByteArrayInputStream(data);
        this.file = null;
        this.url = null;
        this.location = null;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * file.
     * 
     * @param stream the input stream
     * @param file the file where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final File file) {
        this.stream = Assert.requireNonNull(stream, "stream is null");
        this.file = Assert.requireNonNull(file, "file is null");
        this.location = file.getAbsolutePath();
        this.url = null;
        this.data = null;
    }

    /**
     * Constructs a new {@code ConfigurationSource} with the specified input stream that originated from the specified
     * url.
     * 
     * @param stream the input stream
     * @param url the URL where the input stream originated
     */
    public ConfigurationSource(final InputStream stream, final URL url) {
        this.stream = Assert.requireNonNull(stream, "stream is null");
        this.url = Assert.requireNonNull(url, "URL is null");
        this.location = url.toString();
        this.file = null;
        this.data = null;
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
        if (file != null) {
            return new ConfigurationSource(new FileInputStream(file), file);
        } else if (url != null) {
            return new ConfigurationSource(url.openStream(), url);
        } else {
            return new ConfigurationSource(data);
        }
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
}