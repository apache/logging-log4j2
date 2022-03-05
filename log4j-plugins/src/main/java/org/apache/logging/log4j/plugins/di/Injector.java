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

package org.apache.logging.log4j.plugins.di;

import org.apache.logging.log4j.plugins.FactoryType;
import org.apache.logging.log4j.plugins.Node;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

public interface Injector {

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
     * Gets a factory for instances that match the given key.
     *
     * @param key key to look up a factory for
     * @param <T> return type of factory
     * @return a factory for the provided key
     */
    <T> Supplier<T> getFactory(final Key<T> key);

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
     * Creates a configured instance using the provided configuration node.
     *
     * @param node root node to create a configured instance using
     * @param <T>  type of instance the given node configures
     * @return the configured instance
     */
    <T> T getInstance(final Node node);

    /**
     * Removes any existing bindings matching the provided key.
     *
     * @param key key to remove binding for
     */
    void removeBinding(final Key<?> key);

    /**
     * Installs a module into this Injector. A module is an instance of a class with methods annotated with
     * {@link FactoryType}-annotated annotations. If all factory methods in the module are static, then the
     * {@link Class} instance may be used directly.
     *
     * @param module module to install with factory methods for factories
     */
    void installModule(final Object module);

    /**
     * Sets the reflective caller context this Injector should use for reflection operations.
     *
     * @param callerContext strategy for performing reflection operations
     */
    void setCallerContext(final ReflectiveCallerContext callerContext);

    /**
     * Binds a scope annotation type to the given Scope strategy.
     *
     * @param scopeType scope annotation type
     * @param scope     scope strategy to use for the given scope type
     */
    void bindScope(final Class<? extends Annotation> scopeType, final Scope scope);

    /**
     * Gets the Scope strategy for the given scope annotation type.
     *
     * @param scopeType scope annotation type
     * @return the bound scope instance for the provided scope type
     */
    Scope getScope(final Class<? extends Annotation> scopeType);

    /**
     * Initializes this Injector with all registered {@link InjectorCallback} services in priority order.
     */
    void init();

    /**
     * Creates a new Injector copied from the current bindings and scopes of this instance.
     *
     * @return a fresh copy of this instance
     */
    Injector copy();

    /**
     * Binds a key to a factory. This overwrites any existing binding the provided key may have already had.
     *
     * @param key     key of the factory to create a binding with
     * @param factory the factory to bind to the key
     * @param <T>     the return type of the factory
     * @return this Injector
     */
    <T> Injector bindFactory(final Key<T> key, final Supplier<? extends T> factory);

    /**
     * Binds a key to a factory only if no bindings for that key already exist.
     *
     * @param key     key of the factory to create a binding with if none exist
     * @param factory the factory to bind to the key
     * @param <T>     the return type of the factory
     * @return this Injector
     */
    <T> Injector bindIfAbsent(final Key<T> key, final Supplier<? extends T> factory);

}
