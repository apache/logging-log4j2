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

package org.apache.logging.log4j.plugins.spi;

import java.util.Optional;

/**
 * Provides operations used by {@link Bean} implementations for tracking lifecycle state.
 */
public interface InitializationContext<T> extends AutoCloseable {
    /**
     * Pushes an incomplete instance state. An instance is not completely initialized until it is returned from
     * {@link Bean#create(InitializationContext)}.
     *
     * @param incompleteInstance incompletely initialized instance
     */
    void addIncompleteInstance(final T incompleteInstance);

    // TODO: this API is needlessly complicated currently
    boolean isTrackingDependencies(final Bean<T> bean);

    void addDependentInstance(T dependentInstance);

    Optional<Bean<?>> getNonDependentScopedDependent();

    <S> Optional<S> getIncompleteInstance(Bean<S> bean);

    // dependent contexts share the same incomplete instances
    <D> InitializationContext<D> createDependentContext(Bean<D> bean);

    // independent contexts are used by producers to get their declaring bean separately
    <P> InitializationContext<P> createProducerContext(Bean<P> bean);

    /**
     * Destroys all dependent objects by propagating them to {@link Bean#destroy(Object, InitializationContext)}.
     */
    @Override
    void close();
}
