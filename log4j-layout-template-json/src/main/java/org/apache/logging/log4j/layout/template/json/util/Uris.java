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
package org.apache.logging.log4j.layout.template.json.util;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

public final class Uris {

    private Uris() {}

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Reads {@link URI} specs of scheme <tt>classpath</tt> and <tt>file</tt>.
     *
     * @param spec the {@link URI} spec, e.g., <tt>file:/holy/cow.txt</tt> or
     *             <tt>classpath:/holy/cat.txt</tt>
     * @param charset used {@link Charset} for decoding the file
     */
    public static String readUri(final String spec, final Charset charset) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(charset, "charset");
        try {
            final URI uri = new URI(spec);
            return unsafeReadUri(uri, charset);
        } catch (final Exception error) {
            throw new RuntimeException("failed reading URI: " + spec, error);
        }
    }

    /**
     * Reads {@link URI}s of scheme <tt>classpath</tt> and <tt>file</tt>.
     *
     * @param uri the {@link URI}, e.g., <tt>file:/holy/cow.txt</tt> or
     *             <tt>classpath:/holy/cat.txt</tt>
     * @param charset used {@link Charset} for decoding the file
     */
    public static String readUri(final URI uri, final Charset charset) {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(charset, "charset");
        try {
            return unsafeReadUri(uri, charset);
        } catch (final Exception error) {
            throw new RuntimeException("failed reading URI: " + uri, error);
        }
    }

    private static String unsafeReadUri(final URI uri, final Charset charset) throws Exception {
        final String uriScheme = toRootLowerCase(uri.getScheme());
        switch (uriScheme) {
            case "classpath":
                return readClassPathUri(uri, charset);
            case "file":
                return readFileUri(uri, charset);
            default: {
                throw new IllegalArgumentException("unknown scheme in URI: " + uri);
            }
        }
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The uri parameter comes from aconfiguration file.")
    private static String readFileUri(final URI uri, final Charset charset) throws IOException {
        final Path path = Paths.get(uri);
        try (final BufferedReader fileReader = Files.newBufferedReader(path, charset)) {
            return consumeReader(fileReader);
        }
    }

    @SuppressFBWarnings(
            value = "URLCONNECTION_SSRF_FD",
            justification = "The uri parameter comes fro a configuration file.")
    private static String readClassPathUri(final URI uri, final Charset charset) throws IOException {
        final String spec = uri.toString();
        final String path = spec.substring("classpath:".length());
        final List<URL> resources = new ArrayList<>(LoaderUtil.findResources(path));
        if (resources.isEmpty()) {
            final String message = String.format("could not locate classpath resource (path=%s)", path);
            throw new RuntimeException(message);
        }
        final URL resource = resources.get(0);
        if (resources.size() > 1) {
            final String message = String.format(
                    "for URI %s found %d resources, using the first one: %s", uri, resources.size(), resource);
            LOGGER.warn(message);
        }
        try (final InputStream inputStream = resource.openStream()) {
            try (final InputStreamReader reader = new InputStreamReader(inputStream, charset);
                    final BufferedReader bufferedReader = new BufferedReader(reader)) {
                return consumeReader(bufferedReader);
            }
        }
    }

    private static String consumeReader(final BufferedReader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }
}
