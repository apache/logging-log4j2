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
package org.apache.logging.log4j.util;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A source for global configuration properties.
 *
 * @since 2.10.0
 */
public interface PropertySource {

    public static final String PREFIX = "log4j2.";
    public static final String SYSTEM_CONTEXT = "*";

    public static final int DEFAULT_PRIORITY = 200;

    static final String MAPPING_FILE = "log4j2.propertyMapping.json";

    /**
     * Returns the order in which this PropertySource has priority. A higher value means that the source will be
     * searched later and can be overridden by other property sources.
     *
     * @return priority value
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    /**
     * Iterates over all properties and performs an action for each key/value pair.
     *
     * @param action action to perform on each key/value pair
     */
    default void forEach(final BiConsumer<String, String> action) {
    }

    /**
     * Returns the list of all property names for the System Context.
     *
     * @return list of property names
     */
    default Collection<String> getPropertyNames() {
        return Collections.emptySet();
    }

    /**
     * Converts a list of property name tokens into a normal form. For example, by default a list of tokens such as
     * "foo", "bar", "baz", will be normalized into the property name "log4j2.foo.bar.baz". Note that this is
     * normally used in conjunction with Util.tokenize() which breaks a string into tokens and then this puts
     * them back together.
     *
     * @param tokens list of property name tokens
     * @return a normalized property name using the given tokens
     */
    default CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        return Util.join(tokens);
    }

    /**
     * For PropertySources that cannot iterate over all the potential properties this provides a direct lookup.
     * @param key The key to search for.
     * @return The value or null;
     * @since 2.13.0
     */
    default String getProperty(final String key) {
        return null;
    }

    /**
     * For PropertySources that cannot iterate over all the potential properties this provides a direct lookup.
     * @param key The key to search for.
     * @return The value or null;
     * @since 2.13.0
     */
    default boolean containsProperty(final String key) {
        return false;
    }

    /**
     * Comparator for ordering PropertySource instances by priority.
     *
     * @since 2.10.0
     */
    class Comparator implements java.util.Comparator<PropertySource> {

        @Override
        public int compare(final PropertySource o1, final PropertySource o2) {
            int result = Integer.compare(Objects.requireNonNull(o1).getPriority(), Objects.requireNonNull(o2).getPriority());
            if (result == 0) {
                result = o1.equals(o2) ? 0 : -1;
            }
            return result;
        }
    }

    /**
     * Utility methods useful for PropertySource implementations.
     *
     * @since 2.10.0
     */
    final class Util {
        private static final Map<CharSequence, List<CharSequence>> CACHE = new ConcurrentHashMap<>();
        // List of "shorthand" properties and the property they map to.
        static {
            populateCache();
        }

        /**
         * Converts a property name string into a list of tokens. This will strip a prefix of {@code log4j2} along
         * with separators of dash {@code -}, dot {@code .}, underscore {@code _}, and slash {@code /}. Tokens can
         * also be separated by camel case conventions without needing a separator character in between.
         *
         * @param value property name
         * @return the property broken into lower case tokens
         */
        public static List<CharSequence> tokenize(final CharSequence value) {
            if (CACHE.containsKey(value)) {
                return CACHE.get(value);
            }
            final List<CharSequence> tokens = Arrays.asList(value.toString().split("[._/]+"));
            CACHE.put(value, tokens);
            return tokens;
        }

        /**
         * Joins a list of strings separating each token with a ".". The first token (which should be the
         * component name) will be stored as is. The second, and subsequent, tokens will have their first
         * character forced to lower case.
         *
         * @param tokens tokens to convert
         * @return a CharSequence with each token delimited by a period.
         */
        public static CharSequence join(final Iterable<? extends CharSequence> tokens) {
            final StringBuilder sb = new StringBuilder();
            int tokenCount = 0;
            final Collection<CharSequence> tokenCollection = Cast.cast(tokens);
            final int lowerIndex = tokenCollection.size() > 3 ? 3 : 1;
            for (final CharSequence token : tokens) {
                if (sb.length() > 0) {
                    sb.append(".");
                    if (tokenCount >= lowerIndex) {
                        sb.append(Character.toLowerCase(token.charAt(0)));
                        sb.append(token.subSequence(1, token.length()));
                    } else {
                        sb.append(token);
                    }
                } else {
                    sb.append(token);
                }
                ++tokenCount;
            }
            return sb.toString();
        }

        /**
         * Joins a list of strings separating each token with a ".".
         *
         * @param tokens tokens to convert
         * @return tokensAsCamelCase
         */
        public static CharSequence joinAsCamelCase(final Iterable<? extends CharSequence> tokens) {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (final CharSequence token : tokens) {
                if (first) {
                    sb.append(token);
                } else {
                    sb.append(Character.toUpperCase(token.charAt(0)));
                    if (token.length() > 1) {
                        sb.append(token.subSequence(1, token.length()));
                    }
                }
                first = false;
            }
            return sb.toString();
        }

        static String resolveKey(final String key) {
            final List<CharSequence> tokens = getTokens(key);
            return tokens != null ? join(tokens).toString() : key;
        }

        static List<CharSequence> getTokens(final String key) {
            List<CharSequence> tokens = CACHE.get(key);
            if (tokens == null) {
                tokens = tokenize(key);
            }
            return tokens;
        }

        static void populateCache() {
            try {
                final Enumeration<URL> urls = PropertySource.class.getClassLoader().getResources(MAPPING_FILE);
                while (urls.hasMoreElements()) {
                    final URL url = urls.nextElement();
                    try (final InputStream is = url.openStream()) {
                        final String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        final Map<String, Object> root = Cast.cast(JsonReader.read(json));
                        populateCache("", root);
                    } catch (Exception ex) {
                        LowLevelLogUtil.logException("Unable to parse JSON for " + url.toString(), ex);
                    }
                }
            } catch (Exception ex) {
                LowLevelLogUtil.logException("Unable to access " + MAPPING_FILE, ex);
            }
        }

        static void populateCache(final String prefix, final Map<String, Object> root) {
            if (!root.isEmpty()) {
                for (Map.Entry<String, Object> entry : root.entrySet()) {
                    final String delim = entry.getKey().equals(entry.getKey().toUpperCase(Locale.ROOT)) ? "_" : ".";
                    if (entry.getValue() instanceof String) {
                        final String value = Cast.cast(entry.getValue());
                        CACHE.put(prefix + delim + entry.getKey(), Collections.singletonList(value));
                    } else if (entry.getValue() instanceof List) {
                        CACHE.put(prefix + delim + entry.getKey(), Cast.cast(entry.getValue()));
                    } else {
                        final String key = Strings.isEmpty(prefix) ? entry.getKey() : prefix + delim + entry.getKey();
                        populateCache(key, Cast.cast(entry.getValue()));
                    }
                }
            }
        }

        private Util() {
        }
    }
}
