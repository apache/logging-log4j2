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

package org.apache.logging.log4j.plugins.defaults.bean;

import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.Scoped;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultInitializationContext<T> implements InitializationContext<T> {

    private final Scoped<T> scoped;
    private Map<Scoped<?>, Object> incompleteInstances;
    private final List<ScopedInstance<?>> dependentInstances = new CopyOnWriteArrayList<>();
    private final List<ScopedInstance<?>> parentDependentInstances;
    private final InitializationContext<?> parentContext;

    public DefaultInitializationContext(final Scoped<T> scoped) {
        this(scoped, null, new CopyOnWriteArrayList<>(), null);
    }

    private DefaultInitializationContext(final Scoped<T> scoped, final Map<Scoped<?>, Object> incompleteInstances) {
        this(scoped, incompleteInstances, new CopyOnWriteArrayList<>(), null);
    }

    private DefaultInitializationContext(final Scoped<T> scoped, final Map<Scoped<?>, Object> incompleteInstances,
                                         final List<ScopedInstance<?>> parentDependentInstances,
                                         final InitializationContext<?> parentContext) {

        this.scoped = scoped;
        this.incompleteInstances = incompleteInstances;
        this.parentDependentInstances = parentDependentInstances;
        this.parentContext = parentContext;
    }

    @Override
    public void addIncompleteInstance(final T incompleteInstance) {
        if (incompleteInstances == null) {
            incompleteInstances = new ConcurrentHashMap<>();
        }
        if (scoped != null) {
            incompleteInstances.put(scoped, incompleteInstance);
        }
    }

    @Override
    public boolean isTrackingDependencies(final Scoped<T> scoped) {
        return !dependentInstances.isEmpty() ||
                (scoped instanceof AbstractBean<?, ?> && ((AbstractBean<?, ?>) scoped).isTrackingDependencies());
    }

    @Override
    public void addDependentInstance(final T dependentInstance) {
        parentDependentInstances.add(new DefaultScopedInstance<>(scoped, dependentInstance, this));
    }

    @Override
    public Optional<Scoped<?>> getNonDependentScopedDependent() {
        if (parentContext == null || scoped == null) {
            return Optional.empty();
        }
        return scoped.isDependentScoped() ? parentContext.getNonDependentScopedDependent() : Optional.of(scoped);
    }

    @Override
    public <S> InitializationContext<S> createDependentContext(final Scoped<S> scoped) {
        return new DefaultInitializationContext<>(scoped, incompleteInstances, dependentInstances, this);
    }

    @Override
    public <S> InitializationContext<S> createIndependentContext(final Scoped<S> scoped) {
        return new DefaultInitializationContext<>(scoped, incompleteInstances == null ? null : new ConcurrentHashMap<>(incompleteInstances));
    }

    @Override
    public <S> Optional<S> getIncompleteInstance(final Scoped<S> scoped) {
        return Optional.ofNullable(incompleteInstances)
                .map(map -> map.get(scoped))
                .map(TypeUtil::cast);
    }

    @Override
    public void close() {
        for (final ScopedInstance<?> dependentInstance : dependentInstances) {
            if (!(scoped != null && scoped.equals(dependentInstance.getScoped()))) {
                dependentInstance.close();
            }
        }
    }

}
