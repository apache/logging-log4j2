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
package org.apache.logging.log4j.spi.recycler;

import static java.util.Objects.requireNonNull;

import aQute.bnd.annotation.spi.ServiceConsumer;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Registry of available {@link RecyclerFactoryProvider}s.
 */
@ServiceConsumer(RecyclerFactoryProvider.class)
public final class RecyclerFactoryRegistry {

    private static final Map<String, RecyclerFactoryProvider> PROVIDER_BY_NAME = loadProviders();

    private static Map<String, RecyclerFactoryProvider> loadProviders() {
        return ServiceLoaderUtil.safeStream(RecyclerFactoryProvider.class, null)
                .sorted(Comparator.comparing(RecyclerFactoryProvider::getOrder))
                .collect(Collectors.toMap(
                        RecyclerFactoryProvider::getName,
                        Function.identity(),
                        (provider1, provider2) -> {
                            final String message = String.format(
                                    "recycler factory providers `%s` (order=%d) and `%s` (order=%d) have conflicting names: `%s`",
                                    provider1.getClass().getCanonicalName(),
                                    provider1.getOrder(),
                                    provider2.getClass().getCanonicalName(),
                                    provider2.getOrder(),
                                    provider2.getName());
                            throw new IllegalStateException(message);
                        },
                        LinkedHashMap::new));
    }

    private RecyclerFactoryRegistry() {}

    public static Collection<RecyclerFactoryProvider> getRecyclerFactoryProviders() {
        return PROVIDER_BY_NAME.values();
    }

    public static RecyclerFactory findRecyclerFactory(final PropertyEnvironment environment) {

        // Check arguments
        requireNonNull(environment, "environment");

        // Short-circuit if there are no recycler factories available
        if (PROVIDER_BY_NAME.isEmpty()) {
            throw new IllegalStateException("couldn't find any recycler factories");
        }

        // If there is a particular recycler factory requested, get that done.
        @Nullable final String name = environment.getStringProperty(LoggingSystemProperty.RECYCLER_FACTORY);
        if (name != null) {
            return findRecyclerFactoryByName(environment, name);
        }

        // Otherwise, find one available
        return PROVIDER_BY_NAME.values().stream()
                .sorted(Comparator.comparing(RecyclerFactoryProvider::getOrder))
                .map(provider -> provider.createForEnvironment(environment))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> {
                    final String names = PROVIDER_BY_NAME.keySet().stream()
                            .map(name_ -> '`' + name_ + '`')
                            .collect(Collectors.joining(", "));
                    final String message = String.format(
                            "Couldn't find a recycler factory provider available for the provided environment. Names of the available recycler factory providers: %s",
                            names);
                    return new IllegalArgumentException(message);
                });
    }

    private static RecyclerFactory findRecyclerFactoryByName(final PropertyEnvironment environment, final String name) {
        @Nullable final RecyclerFactoryProvider provider = PROVIDER_BY_NAME.get(name);
        if (provider == null) {
            final String names = PROVIDER_BY_NAME.keySet().stream()
                    .map(name_ -> '`' + name_ + '`')
                    .collect(Collectors.joining(", "));
            final String message = String.format(
                    "Couldn't find a recycler factory provider of name `%s`. Names of the available recycler factory providers: %s",
                    name, names);
            throw new IllegalArgumentException(message);
        }
        @Nullable final RecyclerFactory factory = provider.createForEnvironment(environment);
        if (factory == null) {
            final String message = String.format(
                    "failed to configure recycler factory of name `%s` for the provided environment", name);
            throw new IllegalArgumentException(message);
        }
        return factory;
    }
}
