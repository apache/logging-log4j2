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
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonResourcePropertySource implements PropertySource {

    public static JsonResourcePropertySource fromUrl(final URL url, final int priority) {
        final String json;
        try (final InputStream in = url.openStream()) {
            final byte[] bytes = in.readAllBytes();
            // JSON is defined as UTF-8 which is convenient
            json = new String(bytes, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final Object read = JsonReader.read(json);
        if (!(read instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected JSON object key " + ROOT_KEY + " from " + url);
        }
        final Object root = ((Map<?, ?>) read).get(ROOT_KEY);
        if (!(root instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected map in " + ROOT_KEY + " from " + url);
        }
        return new JsonResourcePropertySource(Cast.cast(root), priority);
    }

    public static final String ROOT_KEY = "log4j2";
    private static final Pattern COMPOSITE_KEY = Pattern.compile("log4j2\\.(?<context>[^.]+)\\.(?<component>[^.]+)\\.(?<key>.+)");
    private final Map<String, Object> data;
    private final int priority;

    private JsonResourcePropertySource(final Map<String, Object> data, final int priority) {
        this.priority = priority;
        Objects.requireNonNull(data);
        this.data = data;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public String getProperty(final String key) {
        return getString(splitCompositeKey(key));
    }

    @Override
    public String getProperty(final String context, final String key) {
        return getString(splitKey(context, key));
    }

    @Override
    public List<String> getList(final String context, final String key) {
        final Object value = lookup(splitKey(context, key));
        if (value instanceof String) {
            return List.of(Strings.splitList((String) value));
        }
        if (value instanceof List<?>) {
            return Cast.cast(value);
        }
        return List.of();
    }

    @Override
    public BooleanProperty getBoolean(final String context, final String key) {
        if (hasProperty(splitKey(context, key))) {
            final Object value = lookup(splitKey(context, key));
            if (value instanceof Boolean) {
                return (boolean) value ? BooleanProperty.TRUE : BooleanProperty.FALSE;
            }
            if (value instanceof String) {
                return BooleanProperty.parse((String) value);
            }
            return BooleanProperty.PRESENT;
        }
        return BooleanProperty.ABSENT;
    }

    @Override
    public OptionalInt getInt(final String context, final String key) {
        final Object value = lookup(splitKey(context, key));
        if (value instanceof Number) {
            return OptionalInt.of(((Number) value).intValue());
        }
        if (value instanceof String) {
            try {
                final int i = Integer.parseInt((String) value);
                return OptionalInt.of(i);
            } catch (final NumberFormatException ignored) {
            }
        }
        return OptionalInt.empty();
    }

    @Override
    public OptionalLong getLong(final String context, final String key) {
        final Object value = lookup(splitKey(context, key));
        if (value instanceof Number) {
            return OptionalLong.of(((Number) value).longValue());
        }
        if (value instanceof String) {
            try {
                final long i = Long.parseLong((String) value);
                return OptionalLong.of(i);
            } catch (final NumberFormatException ignored) {
            }
        }
        return OptionalLong.empty();
    }

    private String getString(final String[] path) {
        final Object value = lookup(path);
        return value != null ? value.toString() : null;
    }

    @Override
    public boolean containsProperty(final String key) {
        return hasProperty(splitCompositeKey(key));
    }

    @Override
    public boolean containsProperty(final String context, final String key) {
        return hasProperty(splitKey(context, key));
    }

    private boolean hasProperty(final String[] path) {
        Object value = data;
        for (final String key : path) {
            if (value instanceof Map<?, ?>) {
                final Map<String, Object> map = Cast.cast(value);
                if (!map.containsKey(key)) {
                    return false;
                }
                value = map.get(key);
            } else {
                return false;
            }
        }
        return true;
    }

    private Object lookup(final String[] path) {
        Object value = data;
        for (final String key : path) {
            if (value instanceof Map<?, ?>) {
                final Map<String, Object> map = Cast.cast(value);
                value = map.get(key);
            } else {
                return null;
            }
        }
        return value;
    }

    private static String[] splitCompositeKey(final String key) {
        final Matcher matcher = COMPOSITE_KEY.matcher(key);
        if (matcher.matches()) {
            final String[] parts = new String[3];
            parts[0] = matcher.group("context");
            parts[1] = matcher.group("component");
            parts[2] = matcher.group("key");
            return parts;
        }
        throw new IllegalArgumentException("Invalid key: " + key);
    }

    private static String[] splitKey(final String context, final String key) {
        final String[] parts = splitCompositeKey(key);
        assert context != null;
        parts[0] = context;
        return parts;
    }
}
