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
package org.apache.logging.log4j.osgi.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves bundle JAR locations from Pax Exam link files generated at build time.
 */
final class BundleLocations {

    private BundleLocations() {}

    static Path resolveBundleJar(final String symbolicName) throws IOException {
        final String linkResource = symbolicName + ".link";
        try (InputStream in = BundleLocations.class.getClassLoader().getResourceAsStream(linkResource)) {
            if (in == null) {
                throw new IOException("Missing Pax Exam link file: " + linkResource);
            }
            final String link = readUtf8(in).trim();
            if (link.startsWith("link:")) {
                return Paths.get(URI.create(link.substring("link:".length())));
            }
            if (link.startsWith("reference:")) {
                return Paths.get(URI.create(link.substring("reference:".length())));
            }
            return Paths.get(URI.create(link));
        }
    }

    private static String readUtf8(final InputStream in) throws IOException {
        final byte[] buffer = new byte[4096];
        final StringBuilder content = new StringBuilder();
        int read;
        while ((read = in.read(buffer)) >= 0) {
            content.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return content.toString();
    }
}
