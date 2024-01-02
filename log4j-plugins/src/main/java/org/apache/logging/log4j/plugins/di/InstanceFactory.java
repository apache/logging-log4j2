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
package org.apache.logging.log4j.plugins.di;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverterFactory;
import org.apache.logging.log4j.plugins.di.spi.DependencyChain;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.di.spi.Scope;

/**
 * Manages dependency injection of a set of bindings between {@link Key}s and {@link Supplier}s
 * lifecycle-bound to {@link Scope}s. Keys describe the type, name, namespace, qualifier type, and order of a binding.
 * Suppliers are known as <i>factories</i>, and factories may have injectable dependencies on other bindings upon
 * creation. Scopes control the lifecycle of instances returned by a binding's factory. Factories for keys can be
 * looked up {@linkplain #getFactory(Key) by key} or {@linkplain #getFactory(Class) by class}.
 */
public interface InstanceFactory {

    /**
     * Gets a factory for instances that match the given resolvable key.
     *
     * @param resolvableKey key with alias and dependency chain to look up a factory for
     * @param <T>           return type of factory
     * @return a factory for the given resolvable key
     */
    <T> Supplier<T> getFactory(final ResolvableKey<T> resolvableKey);

    /**
     * Gets a factory for instances that match the given key with dependencies.
     * This is used for recursive factory lookups while initializing dependent factories.
     *
     * @param key             key to look up a factory for
     * @param aliases         alias names to include in the search
     * @param dependencyChain chain of dependencies (possibly {@linkplain DependencyChain#empty() empty}) already being
     *                        created in the context of this factory lookup
     * @param <T>             return type of factory
     * @return a factory for the provided key with the given dependency chain context
     */
    default <T> Supplier<T> getFactory(
            final Key<T> key, final Collection<String> aliases, final DependencyChain dependencyChain) {
        return getFactory(ResolvableKey.of(key, aliases, dependencyChain));
    }

    /**
     * Gets a factory for instances that match the given key or aliases.
     *
     * @param key     key to look up a factory for
     * @param aliases alias names to include in the search
     * @param <T>     return type of factory
     * @return a factory for the provided key with the given dependency chain context
     */
    default <T> Supplier<T> getFactory(final Key<T> key, final Collection<String> aliases) {
        return getFactory(ResolvableKey.of(key, aliases));
    }

    /**
     * Gets a factory for instances that match the given key.
     *
     * @param key key to look up a factory for
     * @param <T> return type of factory
     * @return a factory for the provided key
     */
    default <T> Supplier<T> getFactory(final Key<T> key) {
        return getFactory(ResolvableKey.of(key));
    }

    /**
     * Gets a factory for instances of the provided class.
     *
     * @param clazz class to look up a factory for
     * @param <T>   return type of factory
     * @return a factory for the provided class
     */
    default <T> Supplier<T> getFactory(final Class<T> clazz) {
        return getFactory(Key.forClass(clazz));
    }

    /**
     * Gets an instance that matches the provided key.
     *
     * @param key key to get or create an instance matching
     * @param <T> type of instance
     * @return an instance matching the provided key
     */
    default <T> T getInstance(final Key<T> key) {
        return getFactory(key).get();
    }

    /**
     * Gets an instance for the provided class.
     *
     * @param clazz class to get or create an instance of
     * @param <T>   type of instance
     * @return an instance of the provided class
     */
    default <T> T getInstance(final Class<T> clazz) {
        return getFactory(clazz).get();
    }

    /**
     * Gets an instance that matches the given resolvable key.
     *
     * @param resolvableKey key to get or create an instance of
     * @param <T>           type of instance
     * @return an instance matching the provided key
     */
    default <T> T getInstance(final ResolvableKey<T> resolvableKey) {
        return getFactory(resolvableKey).get();
    }

    /**
     * Gets an instance that matches the given key with aliases and dependencies.
     *
     * @param key             key to get or create an instance of
     * @param aliases         alias names to include in the search
     * @param dependencyChain chain of dependencies (possibly {@linkplain DependencyChain#empty() empty}) already being
     *                        created in the context of this instance lookup
     * @param <T>             type of instance
     * @return an instance matching the provided key
     */
    default <T> T getInstance(
            final Key<T> key, final Collection<String> aliases, final DependencyChain dependencyChain) {
        return getFactory(ResolvableKey.of(key, aliases, dependencyChain)).get();
    }

    /**
     * Gets an instance that matches the provided key with dependencies.
     *
     * @param key             key to get or create an instance matching
     * @param dependencyChain chain of dependencies (possibly {@linkplain DependencyChain#empty() empty}) already being
     *                        created in the context of this instance lookup
     * @param <T>             type of instance
     * @return an instance matching the provided key
     */
    default <T> T getInstance(final Key<T> key, final DependencyChain dependencyChain) {
        return getFactory(ResolvableKey.of(key, dependencyChain)).get();
    }

    /**
     * Gets an instance for the provided class with dependencies.
     *
     * @param clazz           class to get or create an instance of
     * @param dependencyChain chain of dependencies (possibly {@linkplain DependencyChain#empty() empty}) already being
     *                        created in the context of this instance lookup
     * @param <T>             type of instance
     * @return an instance of the provided class
     */
    default <T> T getInstance(final Class<T> clazz, final DependencyChain dependencyChain) {
        return getFactory(ResolvableKey.of(Key.forClass(clazz), dependencyChain))
                .get();
    }

    /**
     * Gets a type converter for the provided type.
     *
     * @param type target type of type converter to get
     * @param <T>  the type to convert to
     * @return a type converter for the requested type if available
     */
    default <T> TypeConverter<T> getTypeConverter(final Type type) {
        return getInstance(TypeConverterFactory.class).getTypeConverter(type);
    }

    /**
     * Indicates if a binding exists for the provided key.
     */
    boolean hasBinding(Key<?> key);
}
