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

package org.apache.logging.log4j.plugins.defaults.scope;

import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;
import org.apache.logging.log4j.plugins.spi.scope.Scoped;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultInitializationContext<T> implements InitializationContext<T> {

    private final Scoped<T> scoped;
    private final Map<Scoped<?>, Object> incompleteInstances;
    private final List<ScopedInstance<?>> dependentInstances;
    private final List<ScopedInstance<?>> parentDependentInstances;
    private final InitializationContext<?> parentContext;

    public DefaultInitializationContext(final Scoped<T> scoped) {
        this(scoped, null);
    }

    private DefaultInitializationContext(final Scoped<T> scoped, final Map<Scoped<?>, Object> incompleteInstances) {
        this(scoped, incompleteInstances, null, null);
    }

    private DefaultInitializationContext(final Scoped<T> scoped, final Map<Scoped<?>, Object> incompleteInstances,
                                         final List<ScopedInstance<?>> parentDependentInstances,
                                         final InitializationContext<?> parentContext) {
        this.scoped = scoped;
        this.incompleteInstances = incompleteInstances != null ? incompleteInstances : new ConcurrentHashMap<>();
        this.dependentInstances = newSynchronizedList();
        this.parentDependentInstances = parentDependentInstances != null ? parentDependentInstances : newSynchronizedList();
        this.parentContext = parentContext;
    }

    @Override
    public void push(final T incompleteInstance) {
        if (scoped != null) {
            incompleteInstances.put(scoped, incompleteInstance);
        }
    }

    @Override
    public Optional<Scoped<T>> getScoped() {
        return Optional.ofNullable(scoped);
    }

    @Override
    public Optional<InitializationContext<?>> getParentContext() {
        return Optional.ofNullable(parentContext);
    }

    @Override
    public <S> InitializationContext<S> createDependentContext(final Scoped<S> scoped) {
        return new DefaultInitializationContext<>(scoped, incompleteInstances, dependentInstances, this);
    }

    @Override
    public <S> InitializationContext<S> createIndependentContext(final Scoped<S> scoped) {
        return new DefaultInitializationContext<>(scoped, new ConcurrentHashMap<>(incompleteInstances));
    }

    @Override
    public <S> Optional<S> getIncompleteInstance(final Scoped<S> scoped) {
        return Optional.ofNullable(TypeUtil.cast(incompleteInstances.get(scoped)));
    }

    void addDependentInstance(final T dependentInstance) {
        // TODO: why does this use the parent dependent when remove uses these dependent?
        parentDependentInstances.add(new DefaultScopedInstance<>(scoped, dependentInstance, this));
    }

    @Override
    public void close() {
        for (final ScopedInstance<?> dependentInstance : dependentInstances) {
            if (!(scoped != null && scoped.equals(dependentInstance.getScoped()))) {
                dependentInstance.close();
            }
        }
    }

    private static <T> List<T> newSynchronizedList() {
        return Collections.synchronizedList(new ArrayList<>());
    }
}
