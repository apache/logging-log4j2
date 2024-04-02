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
package org.apache.logging.log4j.kit.env.internal;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.kit.json.JsonReader;
import org.apache.logging.log4j.util.LoaderUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Utility class to parse {@code propertyMapping.json} files.
 * <p>
 *     The file is a JSON object that maps a Log4j 3.x key to an array of Log4j 2.x keys:
 * </p>
 * <pre>
 *     {
 *         "log4j3.foo.bar": [
 *             "log4j2.baz",
 *             "log4j2.prop"
 *         ]
 *     }
 * </pre>
 * <p>
 *     For complex Log4j 3.x keys, they can be split into nested objects:
 * </p>
 * <pre>
 *     {
 *         "log4j3": {
 *             "foo": {
 *                 "bar": [
 *  *                  "log4j2.baz",
 *  *                  "log4j2.prop"
 *  *              ]
 *             }
 *         }
 *     }
 * </pre>
 * @since 3.0.0
 */
@NullMarked
public final class DefaultPropertyMappingParser {

    /**
     * Parses the contents of a {@code propertyMapping.json} file.
     * @param url The location of the {@code propertyMapping.json} file.
     * @return A parsed {@link PropertyMapping}.
     * @throws IOException if an error occurs while accessing the file.
     * @throws IllegalArgumentException if a parsing error occurs.
     */
    @SuppressWarnings("unchecked")
    public static PropertyMapping parse(final URL url) throws IllegalArgumentException, IOException {
        try (final InputStream stream = url.openStream()) {
            final Object json = JsonReader.read(new String(stream.readAllBytes(), UTF_8));
            final Map<String, List<String>> mapping = new HashMap<>();
            if (json instanceof Map) {
                visitMap(null, (Map<String, ?>) json, mapping);
                return new PropertyMapping(mapping.entrySet().stream());
            } else {
                throw new IllegalArgumentException("Property mapping file needs to contain a JSON object.");
            }
        }
    }

    /**
     * Parses the contents of all {@code propertyMapping.json} files corresponding to a classpath resource.
     * @param resource The classpath resource to use
     * @return A parsed {@link PropertyMapping}.
     * @throws IOException if an error occurs while accessing the file.
     * @throws IllegalArgumentException if a parsing error occurs.
     */
    public static PropertyMapping parse(final String resource) throws IllegalArgumentException, IOException {
        final ClassLoader loader = LoaderUtil.getClassLoader();
        return Collections.list(loader.getResources(resource)).stream()
                .map(url -> {
                    try {
                        return parse(url);
                    } catch (final IOException e) {
                        throw new IllegalArgumentException(
                                "Unable to load legacy property mappings from URL '" + url + "'.", e);
                    }
                })
                .reduce(PropertyMapping::merge)
                .orElse(PropertyMapping.EMPTY);
    }

    @SuppressWarnings("unchecked")
    private static void visitMap(
            final @Nullable String prefix, final Map<String, ?> node, final Map<String, List<String>> mapping) {
        node.forEach((k, value) -> {
            final String key = prefix != null ? prefix + "." + k : k;
            if (value instanceof Map) {
                visitMap(key, (Map<String, ?>) value, mapping);
            }
            if (value instanceof List) {
                visitArray(key, (List<?>) value, mapping);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void visitArray(final String key, final List<?> source, final Map<String, List<String>> mapping) {
        source.forEach(value -> {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(String.format("Expecting `%s` to contain only strings.", source));
            }
        });
        mapping.merge(key, (List<String>) source, (c1, c2) -> {
            final List<String> result = new ArrayList<>(c1.size() + c2.size());
            result.addAll(c1);
            result.addAll(c2);
            return result;
        });
    }
}
