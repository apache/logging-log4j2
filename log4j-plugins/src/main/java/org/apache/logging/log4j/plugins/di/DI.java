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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.apache.logging.log4j.plugins.di.spi.Scope;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.ServiceLoaderUtil;

/**
 * Factory for {@linkplain InstanceFactory instance factories}.
 */
public final class DI {
    private DI() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a new {@linkplain ConfigurableInstanceFactory initialized} instance factory.
     *
     * @return the initialized instance factory
     */
    public static ConfigurableInstanceFactory createInitializedFactory() {
        return builder().build();
    }

    public static FactoryBuilder builder() {
        return new FactoryBuilder();
    }

    public static class FactoryBuilder {
        private final Supplier<ConfigurableInstanceFactory> provider;
        private final List<ConfigurableInstanceFactoryPostProcessor> preInitializationBindings = new ArrayList<>();
        private final ConfigurableInstanceFactoryPostProcessor initializer;

        private final List<ConfigurableInstanceFactoryPostProcessor> postInitializationBindings = new ArrayList<>();

        public FactoryBuilder() {
            provider = DefaultInstanceFactory::new;
            initializer = factory -> ServiceLoaderUtil.safeStream(ServiceLoader.load(
                            ConfigurableInstanceFactoryPostProcessor.class, DI.class.getClassLoader()))
                    .sorted(Comparator.comparing(
                            ConfigurableInstanceFactoryPostProcessor::getClass, OrderedComparator.INSTANCE))
                    .forEachOrdered(processor -> processor.postProcessFactory(factory));
        }

        private FactoryBuilder(final FactoryBuilder copy) {
            this.provider = copy.provider;
            this.preInitializationBindings.addAll(copy.preInitializationBindings);
            this.initializer = copy.initializer;
            this.postInitializationBindings.addAll(copy.postInitializationBindings);
        }

        public <T> UnscopedBindingBuilder<T> addInitialBindingFrom(final Key<T> key) {
            return new UnscopedBindingBuilder<>(key, this, preInitializationBindings::add);
        }

        public <T> UnscopedBindingBuilder<T> addInitialBindingFrom(final Class<T> type) {
            return addInitialBindingFrom(Key.forClass(type));
        }

        public <T> UnscopedBindingBuilder<T> addBindingFrom(final Key<T> key) {
            return new UnscopedBindingBuilder<>(key, this, postInitializationBindings::add);
        }

        public <T> UnscopedBindingBuilder<T> addBindingFrom(final Class<T> type) {
            return addBindingFrom(Key.forClass(type));
        }

        public FactoryBuilder addInitialBundle(final Object bundle) {
            preInitializationBindings.add(factory -> factory.registerBundle(bundle));
            return this;
        }

        public FactoryBuilder addBundle(final Object bundle) {
            postInitializationBindings.add(factory -> factory.registerBundle(bundle));
            return this;
        }

        public ConfigurableInstanceFactory build() {
            final ConfigurableInstanceFactory factory = provider.get();
            preInitializationBindings.forEach(binding -> binding.postProcessFactory(factory));
            initializer.postProcessFactory(factory);
            postInitializationBindings.forEach(binding -> binding.postProcessFactory(factory));
            return factory;
        }

        public FactoryBuilder copy() {
            return new FactoryBuilder(this);
        }
    }

    public static class UnscopedBindingBuilder<T> {
        final Key<T> key;
        final FactoryBuilder builder;
        final Consumer<ConfigurableInstanceFactoryPostProcessor> addBinding;

        private UnscopedBindingBuilder(
                final Key<T> key,
                final FactoryBuilder builder,
                final Consumer<ConfigurableInstanceFactoryPostProcessor> addBinding) {
            this.key = key;
            this.builder = builder;
            this.addBinding = addBinding;
        }

        private UnscopedBindingBuilder(final UnscopedBindingBuilder<T> copy) {
            this(copy.key, copy.builder, copy.addBinding);
        }

        public ScopedBindingBuilder<T> inScope(final Class<? extends Annotation> scopeType) {
            return new ScopedBindingBuilder<>(this, scopeType);
        }

        public FactoryBuilder toInstance(final T instance) {
            addBinding.accept(factory -> factory.registerBinding(key, Lazy.value(instance)));
            return builder;
        }

        public FactoryBuilder toSingleton(final Supplier<? extends T> provider) {
            addBinding.accept(factory -> factory.registerBinding(key, Lazy.lazy(provider)));
            return builder;
        }

        public FactoryBuilder toUnscoped(final Supplier<? extends T> provider) {
            addBinding.accept(factory -> factory.registerBinding(key, provider));
            return builder;
        }

        public FactoryBuilder toFunction(final Function<InstanceFactory, ? extends Supplier<? extends T>> function) {
            addBinding.accept(factory -> factory.registerBinding(key, function.apply(factory)));
            return builder;
        }
    }

    public static class ScopedBindingBuilder<T> extends UnscopedBindingBuilder<T> {
        private final Class<? extends Annotation> scopeType;

        private ScopedBindingBuilder(
                final UnscopedBindingBuilder<T> builder, final Class<? extends Annotation> scopeType) {
            super(builder);
            this.scopeType = scopeType;
        }

        public FactoryBuilder toProvider(final Supplier<? extends T> provider) {
            addBinding.accept(factory -> {
                final Scope scope = factory.getRegisteredScope(scopeType);
                if (scope != null) {
                    final Supplier<T> scoped = scope.get(key, provider::get);
                    factory.registerBinding(key, scoped);
                } else {
                    factory.registerBinding(key, provider);
                }
            });
            return builder;
        }
    }
}
