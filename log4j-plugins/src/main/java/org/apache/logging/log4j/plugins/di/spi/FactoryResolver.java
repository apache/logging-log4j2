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
package org.apache.logging.log4j.plugins.di.spi;

import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;

/**
 * Strategy for resolving factories from existing factories. This is useful for supporting plugin system
 * dependency injection such as configuration attributes, trees of plugin objects, and other conveniences.
 */
public interface FactoryResolver<T> {
    /**
     * Checks if this resolver supports the provided key. If this returns {@code true}, then the factory
     * returned by {@link #getFactory(ResolvableKey, InstanceFactory)} will be used to create a binding
     * for the key.
     *
     * @param key the key to check for support
     * @return true if this resolver supports the key
     */
    boolean supportsKey(final Key<?> key);

    /**
     * Gets a factory for the given resolvable key using existing bindings from the given instance factory.
     * A resolvable key in this context is a {@link Key} combined with alias names and the annotated element
     * this factory is for.
     *
     * @param resolvableKey   the resolvable key to create a binding for
     * @param instanceFactory the existing instance factory to use for composing bindings
     * @return a factory for instances described by the provided key
     */
    Supplier<T> getFactory(final ResolvableKey<T> resolvableKey, final InstanceFactory instanceFactory);
}
