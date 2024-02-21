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
package org.apache.logging.log4j.kit.env.support;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.ConfigurablePropertyEnvironment;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.PropertySource;
import org.apache.logging.log4j.kit.env.internal.CopyOnWriteNavigableSet;
import org.jspecify.annotations.Nullable;

public class PropertySourcePropertyEnvironment extends ClassloaderPropertyEnvironment
        implements ConfigurablePropertyEnvironment {

    private final Collection<PropertySource> sources =
            new CopyOnWriteNavigableSet<>(Comparator.comparing(PropertySource::getPriority));
    private final Map<String, Optional<String>> stringCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> classCache = new ConcurrentHashMap<>();

    public PropertySourcePropertyEnvironment(
            final @Nullable PropertyEnvironment parentEnvironment,
            final Collection<? extends PropertySource> sources,
            final ClassLoader loader,
            final Logger statusLogger) {
        super(loader, statusLogger);
        this.sources.addAll(sources);
        if (parentEnvironment != null) {
            this.sources.add(new ParentEnvironmentPropertySource(parentEnvironment));
        }
    }

    @Override
    public @Nullable String getStringProperty(final String name) {
        return stringCache
                .computeIfAbsent(name, key -> sources.stream()
                        .map(source -> source.getProperty(key))
                        .filter(Objects::nonNull)
                        .findFirst())
                .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(final Class<T> propertyClass) {
        return (T) classCache.computeIfAbsent(propertyClass, super::getProperty);
    }

    @Override
    public void addPropertySource(final PropertySource source) {
        sources.add(Objects.requireNonNull(source));
        stringCache.clear();
        classCache.clear();
    }

    @Override
    public void removePropertySource(final PropertySource source) {
        sources.remove(Objects.requireNonNull(source));
        stringCache.clear();
        classCache.clear();
    }

    private record ParentEnvironmentPropertySource(PropertyEnvironment parentEnvironment) implements PropertySource {

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }

        @Override
        public @Nullable String getProperty(final String name) {
            return parentEnvironment.getStringProperty(name);
        }
    }
}
