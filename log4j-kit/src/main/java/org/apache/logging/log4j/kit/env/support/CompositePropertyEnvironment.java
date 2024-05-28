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
import java.util.Objects;
import java.util.TreeSet;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.kit.env.PropertySource;
import org.jspecify.annotations.Nullable;

/**
 * An environment implementation that supports multiple {@link PropertySource}s.
 */
public class CompositePropertyEnvironment extends ClassLoaderPropertyEnvironment {

    private final Collection<PropertySource> sources =
            new TreeSet<>(Comparator.comparing(PropertySource::getPriority).reversed());

    public CompositePropertyEnvironment(
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
    public @Nullable String getProperty(final String name) {
        return sources.stream()
                .map(source -> source.getProperty(name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private record ParentEnvironmentPropertySource(PropertyEnvironment parentEnvironment) implements PropertySource {

        @Override
        public int getPriority() {
            return Integer.MIN_VALUE;
        }

        @Override
        public @Nullable String getProperty(final String name) {
            return parentEnvironment.getProperty(name);
        }
    }
}
