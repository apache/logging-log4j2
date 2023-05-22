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

import org.apache.logging.log4j.plugins.di.Key;

/**
 * Immutable chain of {@linkplain Key dependency keys} assembled while constructing dependency-injected
 * instances.
 */
public interface DependencyChain extends Iterable<Key<?>> {
    /**
     * Indicates if the given key is contained in this dependency chain.
     */
    boolean hasDependency(final Key<?> key);

    /**
     * Returns a new dependency chain containing the provided key as an additional dependency.
     * If the given key already exists, then it will not be added to the chain.
     */
    DependencyChain withDependency(final Key<?> key);

    /**
     * Indicates whether this dependency chain is empty.
     */
    boolean isEmpty();

    /**
     * Returns an empty dependency chain.
     */
    static DependencyChain empty() {
        return DependencyChains.EMPTY;
    }

    /**
     * Returns a dependency chain containing a single key.
     */
    static DependencyChain of(final Key<?> key) {
        return DependencyChains.singleton(key);
    }
}
