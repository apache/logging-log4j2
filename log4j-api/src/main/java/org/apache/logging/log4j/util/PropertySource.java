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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A source for global and LoggerContext configuration properties.
 *
 * @since 2.10.0
 */
public interface PropertySource {
    /**
     * Constant value for the name used to represent the default context for providing default settings for
     * LoggerContext properties along with default values for global properties.
     *
     * @since 3.0.0
     */
    String DEFAULT_CONTEXT = "*";

    /**
     * Returns the order in which this PropertySource has priority. A higher value means that the source will be
     * searched later and can be overridden by other property sources.
     *
     * @return priority value
     */
    int getPriority();

    /**
     * Iterates over all properties and performs an action for each key/value pair.
     *
     * @param action action to perform on each key/value pair
     */
    @Deprecated(since = "3.0.0")
    default void forEach(final BiConsumer<String, String> action) {
    }

    /**
     * Returns the list of all property names.
     *
     * @return list of property names
     */
    @Deprecated(since = "3.0.0")
    default Collection<String> getPropertyNames() {
        return Collections.emptySet();
    }

    /**
     * Converts a list of property name tokens into a normal form. For example, a list of tokens such as
     * "foo", "bar", "baz", might be normalized into the property name "log4j2.fooBarBaz".
     *
     * @param tokens list of property name tokens
     * @return a normalized property name using the given tokens
     */
    @Deprecated(since = "3.0.0")
    default CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        return null;
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

    default String getProperty(final String context, final String key) {
        final String compositeKey = isDefaultContext(context) ? key : compositeKey(context, key);
        return getProperty(compositeKey);
    }

    default List<String> getList(final String context, final String key) {
        final String value = getProperty(context, key);
        return List.of(Strings.splitList(value));
    }

    default BooleanProperty getBoolean(final String context, final String key) {
        return BooleanProperty.parse(getProperty(context, key));
    }

    default OptionalInt getInt(final String context, final String key) {
        final String value = getProperty(context, key);
        if (value != null) {
            try {
                final int i = Integer.parseInt(value);
                return OptionalInt.of(i);
            } catch (final NumberFormatException ignored) {
            }
        }
        return OptionalInt.empty();
    }

    default OptionalLong getLong(final String context, final String key) {
        final String value = getProperty(context, key);
        if (value != null) {
            try {
                final long i = Long.parseLong(value);
                return OptionalLong.of(i);
            } catch (final NumberFormatException ignored) {
            }
        }
        return OptionalLong.empty();
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

    default boolean containsProperty(final String context, final String key) {
        return isDefaultContext(context) ?
                containsProperty(key) :
                containsProperty(compositeKey(context, key));
    }

    private static boolean isDefaultContext(final String context) {
        return context == null || context.equals(DEFAULT_CONTEXT);
    }

    private static String compositeKey(final String context, final String key) {
        final int i = key.indexOf('*');
        if (i == -1 || i + 1 == key.length()) {
            return "log4j2." + context + '.' + key;
        }
        return key.substring(0, i) + context + key.substring(i + 1);
    }

    /**
     * Comparator for ordering PropertySource instances by priority.
     *
     * @since 2.10.0
     */
    class Comparator implements java.util.Comparator<PropertySource>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final PropertySource o1, final PropertySource o2) {
            return Integer.compare(Objects.requireNonNull(o1).getPriority(), Objects.requireNonNull(o2).getPriority());
        }
    }

    /**
     * @deprecated no longer needed by PropertySource
     */
    @Deprecated(since = "3.0.0")
    final class Util {
        private static final Pattern PREFIX_PATTERN = Pattern.compile(
                // just lookahead for AsyncLogger
                "(^log4j2?[-._/]?|^org\\.apache\\.logging\\.log4j\\.)|(?=AsyncLogger(Config)?\\.)",
                Pattern.CASE_INSENSITIVE);
        private static final Pattern PROPERTY_TOKENIZER = Pattern.compile("([A-Z]*[a-z0-9]+|[A-Z0-9]+)[-._/]?");
        private static final Map<CharSequence, List<CharSequence>> CACHE = new ConcurrentHashMap<>();
        static {
            // Add legacy properties without Log4j prefix
            CACHE.put("disableThreadContext", Arrays.asList("disable", "thread", "context"));
            CACHE.put("disableThreadContextStack", Arrays.asList("disable", "thread", "context", "stack"));
            CACHE.put("disableThreadContextMap", Arrays.asList("disable", "thread", "context", "map"));
            CACHE.put("isThreadContextMapInheritable", Arrays.asList("is", "thread", "context", "map", "inheritable"));
        }

        /**
         * Converts a property name string into a list of tokens. This will strip a prefix of {@code log4j},
         * {@code log4j2}, {@code Log4j}, or {@code org.apache.logging.log4j}, along with separators of
         * dash {@code -}, dot {@code .}, underscore {@code _}, and slash {@code /}. Tokens can also be separated
         * by camel case conventions without needing a separator character in between.
         *
         * @param value property name
         * @return the property broken into lower case tokens
         */
        public static List<CharSequence> tokenize(final CharSequence value) {
            if (CACHE.containsKey(value)) {
                return CACHE.get(value);
            }
            final List<CharSequence> tokens = new ArrayList<>();
            int start = 0;
            final Matcher prefixMatcher = PREFIX_PATTERN.matcher(value);
            if (prefixMatcher.find(start)) {
                start = prefixMatcher.end();
                final Matcher matcher = PROPERTY_TOKENIZER.matcher(value);
                while (matcher.find(start)) {
                    tokens.add(matcher.group(1).toLowerCase());
                    start = matcher.end();
                }
            }
            CACHE.put(value, tokens);
            return tokens;
        }

        /**
         * Joins a list of strings using camelCaseConventions.
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

        private Util() {
        }
    }
}
