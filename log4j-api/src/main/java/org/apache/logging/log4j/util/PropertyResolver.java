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

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public interface PropertyResolver {

    String DEFAULT_CONTEXT = PropertySource.DEFAULT_CONTEXT;

    void addSource(final PropertySource source);

    default boolean hasProperty(final String key) {
        return hasProperty(DEFAULT_CONTEXT, key);
    }

    boolean hasProperty(final String context, final String key);

    default Optional<String> getString(final String key) {
        return getString(DEFAULT_CONTEXT, key);
    }

    Optional<String> getString(final String context, final String key);

    default List<String> getList(final String key) {
        return getList(DEFAULT_CONTEXT, key);
    }

    List<String> getList(final String context, final String key);

    default boolean getBoolean(final String key) {
        return getBoolean(DEFAULT_CONTEXT, key);
    }

    default boolean getBoolean(final String context, final String key) {
        return getBoolean(context, key, false);
    }

    default boolean getBoolean(final String key, final boolean defaultValue) {
        return getBoolean(DEFAULT_CONTEXT, key, defaultValue);
    }

    boolean getBoolean(final String context, final String key, final boolean defaultValue);

    default boolean getBoolean(final String key,
                               final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
        return getBoolean(DEFAULT_CONTEXT, key, defaultValueIfAbsent, defaultValueIfPresent);
    }

    boolean getBoolean(final String context, final String key,
                       final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent);

    default OptionalInt getInt(final String key) {
        return getInt(DEFAULT_CONTEXT, key);
    }

    OptionalInt getInt(final String context, final String key);

    default OptionalLong getLong(final String key) {
        return getLong(DEFAULT_CONTEXT, key);
    }

    OptionalLong getLong(final String context, final String key);
}
