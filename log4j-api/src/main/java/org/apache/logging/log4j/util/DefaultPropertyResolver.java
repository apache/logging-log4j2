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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class DefaultPropertyResolver implements PropertyResolver {

    private final Set<PropertySource> sources = new ConcurrentSkipListSet<>(new PropertySource.Comparator());

    @Override
    public void addSource(final PropertySource source) {
        sources.add(source);
    }

    @Override
    public boolean hasProperty(final String context, final String key) {
        return sources.stream().anyMatch(source -> source.containsProperty(context, key)) ||
                notDefaultContext(context) && sources.stream()
                        .anyMatch(source -> source.containsProperty(DEFAULT_CONTEXT, key));
    }

    @Override
    public Optional<String> getString(final String context, final String key) {
        if (notDefaultContext(context)) {
            final Optional<PropertySource> foundSource = findSourceWithProperty(context, key);
            if (foundSource.isPresent()) {
                return foundSource
                        .map(source -> source.getProperty(context, key));
            }
        }
        return findSourceWithProperty(DEFAULT_CONTEXT, key)
                .map(source -> source.getProperty(DEFAULT_CONTEXT, key));
    }

    @Override
    public List<String> getList(final String context, final String key) {
        if (notDefaultContext(context)) {
            final Optional<PropertySource> foundSource = findSourceWithProperty(context, key);
            if (foundSource.isPresent()) {
                return foundSource
                        .map(source -> source.getList(context, key))
                        .orElseGet(List::of);
            }
        }
        return findSourceWithProperty(DEFAULT_CONTEXT, key)
                .map(source -> source.getList(DEFAULT_CONTEXT, key))
                .orElseGet(List::of);
    }

    @Override
    public boolean getBoolean(final String context, final String key, final boolean defaultValue) {
        if (notDefaultContext(context)) {
            final Optional<PropertySource> foundSource = findSourceWithProperty(context, key);
            if (foundSource.isPresent()) {
                return foundSource
                        .map(source -> source.getBoolean(context, key))
                        .orElse(BooleanProperty.ABSENT)
                        .orElse(defaultValue);
            }
        }
        return findSourceWithProperty(DEFAULT_CONTEXT, key)
                .map(source -> source.getBoolean(DEFAULT_CONTEXT, key))
                .orElse(BooleanProperty.ABSENT)
                .orElse(defaultValue);
    }

    @Override
    public boolean getBoolean(final String context, final String key,
                              final boolean defaultValueIfAbsent, final boolean defaultValueIfPresent) {
        if (notDefaultContext(context)) {
            final Optional<PropertySource> foundSource = findSourceWithProperty(context, key);
            if (foundSource.isPresent()) {
                return foundSource
                        .map(source -> source.getBoolean(context, key))
                        .orElse(BooleanProperty.ABSENT)
                        .orElse(defaultValueIfAbsent, defaultValueIfPresent);
            }
        }
        return findSourceWithProperty(DEFAULT_CONTEXT, key)
                .map(source -> source.getBoolean(DEFAULT_CONTEXT, key))
                .orElse(BooleanProperty.ABSENT)
                .orElse(defaultValueIfAbsent, defaultValueIfPresent);
    }

    @Override
    public OptionalInt getInt(final String context, final String key) {
        if (notDefaultContext(context)) {
            final Optional<PropertySource> foundSource = findSourceWithProperty(context, key);
            if (foundSource.isPresent()) {
                return foundSource
                        .map(source -> source.getInt(context, key))
                        .orElseGet(OptionalInt::empty);
            }
        }
        return findSourceWithProperty(DEFAULT_CONTEXT, key)
                .map(source -> source.getInt(DEFAULT_CONTEXT, key))
                .orElseGet(OptionalInt::empty);
    }

    @Override
    public OptionalLong getLong(final String context, final String key) {
        if (notDefaultContext(context)) {
            final Optional<PropertySource> foundSource = findSourceWithProperty(context, key);
            if (foundSource.isPresent()) {
                return foundSource
                        .map(source -> source.getLong(context, key))
                        .orElseGet(OptionalLong::empty);
            }
        }
        return findSourceWithProperty(DEFAULT_CONTEXT, key)
                .map(source -> source.getLong(context, key))
                .orElseGet(OptionalLong::empty);
    }

    private Optional<PropertySource> findSourceWithProperty(final String context, final String key) {
        return sources.stream()
                .filter(source -> source.containsProperty(context, key))
                .findFirst();
    }

    private static boolean notDefaultContext(final String context) {
        return context != null && !context.equals(DEFAULT_CONTEXT);
    }
}
