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

package org.apache.logging.log4j.plugins.spi.impl;

import org.apache.logging.log4j.plugins.spi.Bean;
import org.apache.logging.log4j.plugins.spi.InitializationContext;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.util.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultInitializationContext<T> implements InitializationContext<T> {

    private final Bean<T> bean;
    private Map<Bean<?>, Object> incompleteInstances;
    private final List<DependentInstance<?>> dependentInstances = new CopyOnWriteArrayList<>();
    private final List<DependentInstance<?>> parentDependentInstances;
    private final InitializationContext<?> parentContext;

    public DefaultInitializationContext(final Bean<T> bean) {
        this(bean, null, new CopyOnWriteArrayList<>(), null);
    }

    private DefaultInitializationContext(final Bean<T> bean, final Map<Bean<?>, Object> incompleteInstances) {
        this(bean, incompleteInstances, new CopyOnWriteArrayList<>(), null);
    }

    private DefaultInitializationContext(final Bean<T> bean, final Map<Bean<?>, Object> incompleteInstances,
                                         final List<DependentInstance<?>> parentDependentInstances,
                                         final InitializationContext<?> parentContext) {

        this.bean = bean;
        this.incompleteInstances = incompleteInstances;
        this.parentDependentInstances = parentDependentInstances;
        this.parentContext = parentContext;
    }

    @Override
    public void addIncompleteInstance(final T incompleteInstance) {
        if (incompleteInstances == null) {
            incompleteInstances = new ConcurrentHashMap<>();
        }
        if (bean != null) {
            incompleteInstances.put(bean, incompleteInstance);
        }
    }

    @Override
    public boolean isTrackingDependencies(final Bean<T> bean) {
        return !dependentInstances.isEmpty() ||
                (bean instanceof AbstractBean<?> && ((AbstractBean<?>) bean).isTrackingDependencies());
    }

    @Override
    public void addDependentInstance(final T dependentInstance) {
        parentDependentInstances.add(new DependentInstance<>(bean, this, dependentInstance));
    }

    @Override
    public Optional<Bean<?>> getNonDependentScopedDependent() {
        if (parentContext == null || bean == null) {
            return Optional.empty();
        }
        return bean.isDependentScoped() ? parentContext.getNonDependentScopedDependent() : Optional.of(bean);
    }

    @Override
    public <S> InitializationContext<S> createDependentContext(final Bean<S> bean) {
        return new DefaultInitializationContext<>(bean, incompleteInstances, dependentInstances, this);
    }

    @Override
    public <S> InitializationContext<S> createProducerContext(final Bean<S> bean) {
        return new DefaultInitializationContext<>(bean, incompleteInstances == null ? null : new ConcurrentHashMap<>(incompleteInstances));
    }

    @Override
    public <S> Optional<S> getIncompleteInstance(final Bean<S> bean) {
        return Optional.ofNullable(incompleteInstances)
                .map(map -> map.get(bean))
                .map(TypeUtil::cast);
    }

    @Override
    public void close() {
        for (final DependentInstance<?> dependentInstance : dependentInstances) {
            if (dependentInstance.bean.equals(bean)) {
                dependentInstance.close();
            }
        }
    }

    private static class DependentInstance<T> implements Value<T> {
        private final Bean<T> bean;
        private final InitializationContext<T> context;
        private final T instance;

        private DependentInstance(final Bean<T> bean, final InitializationContext<T> context, final T instance) {
            this.bean = bean;
            this.context = context;
            this.instance = instance;
        }

        @Override
        public T get() {
            return instance;
        }

        @Override
        public void close() {
            bean.destroy(instance, context);
        }
    }

}
