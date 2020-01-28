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
package org.apache.logging.log4j.jackson.json.template.layout.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public enum Uris {;

    public static String readUri(final String spec) {
        try {
            return unsafeReadUri(spec);
        } catch (final Exception error) {
            final String message = String.format("failed reading URI (spec=%s)", spec);
            throw new RuntimeException(message, error);
        }
    }

    private static String unsafeReadUri(final String spec) throws Exception {
        final URI uri = new URI(spec);
        final String uriScheme = uri.getScheme().toLowerCase();
        switch (uriScheme) {
            case "classpath":
                return readClassPathUri(uri);
            case "file":
                return readFileUri(uri);
            default: {
                final String message = String.format("unknown URI scheme (spec='%s')", spec);
                throw new IllegalArgumentException(message);
            }
        }

    }

    private static String readFileUri(final URI uri) throws IOException {
        final File file = new File(uri);
        try (final FileReader fileReader = new FileReader(file)) {
            return consumeReader(fileReader);
        }
    }

    private static String readClassPathUri(final URI uri) throws IOException {
        final String spec = uri.toString();
        final String path = spec.substring("classpath:".length());
        final URL resource = Uris.class.getClassLoader().getResource(path);
        if (resource == null) {
            final String message = String.format(
                    "could not locate classpath resource (path=%s)", path);
            throw new RuntimeException(message);
        }
        try (final InputStream inputStream = resource.openStream()) {
            try (final InputStreamReader reader =
                         new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                return consumeReader(reader);
            }
        }
    }

    private static String consumeReader(final Reader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();
        try (final BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

}
