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
 * Factory for {@linkplain InstanceFactory instance factories}. This provides a builder DSL for setting up a
 * {@link ConfigurableInstanceFactory} using bindings registered before and after standard
 * {@link ConfigurableInstanceFactoryPostProcessor} service provider classes are invoked.
 */
public final class DI {
    private DI() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a new {@linkplain ConfigurableInstanceFactory initialized} instance factory.
     *
     * @return an initialized instance factory
     */
    public static ConfigurableInstanceFactory createInitializedFactory() {
        return builder().build();
    }

    /**
     * Creates a new builder for customizing a {@link ConfigurableInstanceFactory}.
     *
     * @return new builder
     */
    public static FactoryBuilder builder() {
        return new FactoryBuilder();
    }

    /**
     * Builder DSL for configuring a {@link ConfigurableInstanceFactory} using
     * {@link ConfigurableInstanceFactoryPostProcessor} instances. This DSL is used for adding bindings to be
     * registered before or after standard post-processor service classes have been invoked.
     */
    public static class FactoryBuilder {
        private final Supplier<ConfigurableInstanceFactory> provider;
        private final List<ConfigurableInstanceFactoryPostProcessor> preInitializationBindings = new ArrayList<>();
        private final ConfigurableInstanceFactoryPostProcessor initializer;
        private final List<ConfigurableInstanceFactoryPostProcessor> postInitializationBindings = new ArrayList<>();

        /**
         * Constructs a fresh builder DSL using {@link DefaultInstanceFactory} and all available
         * {@link ConfigurableInstanceFactoryPostProcessor} service provider classes in {@linkplain OrderedComparator sorted order}.
         */
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

        /**
         * Begins to define a binding that will be registered <em>before</em> the {@link ConfigurableInstanceFactory}
         * is initialized with service provider classes.
         *
         * @param key binding key for which a binding will be registered
         * @return builder DSL to specify the binding to register
         * @param <T> type of binding result being registered
         */
        public <T> UnscopedBindingBuilder<T> addInitialBindingFrom(final Key<T> key) {
            return new UnscopedBindingBuilder<>(key, this, preInitializationBindings::add);
        }

        /**
         * Begins to define a binding that will be registered <em>before</em> the {@link ConfigurableInstanceFactory}
         * is initialized with service provider classes.
         *
         * @param type class for which a binding will be registered
         * @return builder DSL to specify the binding to register
         * @param <T> type of binding result being registered
         */
        public <T> UnscopedBindingBuilder<T> addInitialBindingFrom(final Class<T> type) {
            return addInitialBindingFrom(Key.forClass(type));
        }

        /**
         * Begins to define a binding that will be registered <em>after</em> the {@link ConfigurableInstanceFactory}
         * is initialized with service provider classes.
         *
         * @param key binding key for which a binding will be registered
         * @return builder DSL to specify the binding to register
         * @param <T> type of binding result being registered
         */
        public <T> UnscopedBindingBuilder<T> addBindingFrom(final Key<T> key) {
            return new UnscopedBindingBuilder<>(key, this, postInitializationBindings::add);
        }

        /**
         * Begins to define a binding that will be registered <em>after</em> the {@link ConfigurableInstanceFactory}
         * is initialized with service provider classes.
         *
         * @param type class for which a binding will be registered
         * @return builder DSL to specify the binding to register
         * @param <T> type of binding result being registered
         */
        public <T> UnscopedBindingBuilder<T> addBindingFrom(final Class<T> type) {
            return addBindingFrom(Key.forClass(type));
        }

        /**
         * Adds a bundle class or instance to be registered <em>before</em> the {@link ConfigurableInstanceFactory}
         * is initialized with service provider classes.
         *
         * @param bundle bundle class or instance to register
         * @return this builder DSL
         */
        public FactoryBuilder addInitialBundle(final Object bundle) {
            preInitializationBindings.add(factory -> factory.registerBundle(bundle));
            return this;
        }

        /**
         * Adds a bundle class or instance to be registered <em>after</em> the {@link ConfigurableInstanceFactory}
         * is initialized with service provider classes.
         *
         * @param bundle bundle class or instance to register
         * @return this builder DSL
         */
        public FactoryBuilder addBundle(final Object bundle) {
            postInitializationBindings.add(factory -> factory.registerBundle(bundle));
            return this;
        }

        /**
         * Constructs and configures a {@link ConfigurableInstanceFactory} using the configured bindings before
         * and after service provider initialization. All initial bindings are registered before invoking the
         * {@link ConfigurableInstanceFactoryPostProcessor} service providers, and then the remaining bindings
         * are registered.
         *
         * @return the initialized ConfigurableInstanceFactory
         */
        public ConfigurableInstanceFactory build() {
            final ConfigurableInstanceFactory factory = provider.get();
            preInitializationBindings.forEach(binding -> binding.postProcessFactory(factory));
            initializer.postProcessFactory(factory);
            postInitializationBindings.forEach(binding -> binding.postProcessFactory(factory));
            return factory;
        }

        /**
         * Creates a new builder DSL with a copy of the configured bindings from this builder.
         *
         * @return copy of this builder
         */
        public FactoryBuilder copy() {
            return new FactoryBuilder(this);
        }
    }

    /**
     * Builder DSL for configuring a binding to be registered with a {@link ConfigurableInstanceFactory}.
     *
     * @param <T> type of binding result being registered
     */
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

        /**
         * Continues defining a binding in a particular {@linkplain org.apache.logging.log4j.plugins.ScopeType scope}.
         *
         * @param scopeType annotation of the scope to use for this binding
         * @return builder DSL to specify the scoped binding
         */
        public ScopedBindingBuilder<T> inScope(final Class<? extends Annotation> scopeType) {
            return new ScopedBindingBuilder<>(this, scopeType);
        }

        /**
         * Adds the given instance as a binding to the configured key.
         *
         * @param instance instance to bind to the key
         * @return builder DSL for ConfigurableInstanceFactory
         */
        public FactoryBuilder toInstance(final T instance) {
            addBinding.accept(factory -> factory.registerBinding(key, Lazy.value(instance)));
            return builder;
        }

        /**
         * Adds a singleton binding for the given supplier function.
         *
         * @param provider function for creating the initial binding value
         * @return builder DSL for ConfigurableInstanceFactory
         */
        public FactoryBuilder toSingleton(final Supplier<? extends T> provider) {
            addBinding.accept(factory -> factory.registerBinding(key, Lazy.lazy(provider)));
            return builder;
        }

        /**
         * Adds an unscoped binding for the given supplier function. This function will be invoked every time an
         * instance of this binding is requested.
         *
         * @param provider unscoped function for creating the binding value
         * @return builder DSL for ConfigurableInstanceFactory
         */
        public FactoryBuilder toUnscoped(final Supplier<? extends T> provider) {
            addBinding.accept(factory -> factory.registerBinding(key, provider));
            return builder;
        }

        /**
         * Adds a binding using a function that depends on {@link InstanceFactory}. This is useful for registering
         * dependent bindings or more complex bindings in general.
         *
         * @param function unscoped function for creating the binding value from an InstanceFactory
         * @return builder DSL for ConfigurableInstanceFactory
         */
        public FactoryBuilder toFunction(final Function<InstanceFactory, ? extends Supplier<? extends T>> function) {
            addBinding.accept(factory -> factory.registerBinding(key, function.apply(factory)));
            return builder;
        }
    }

    /**
     * Builder DSL for configuring a scoped binding to be registered with a {@link ConfigurableInstanceFactory}.
     *
     * @param <T> type of binding result being registered
     */
    public static class ScopedBindingBuilder<T> extends UnscopedBindingBuilder<T> {
        private final Class<? extends Annotation> scopeType;

        private ScopedBindingBuilder(
                final UnscopedBindingBuilder<T> builder, final Class<? extends Annotation> scopeType) {
            super(builder);
            this.scopeType = scopeType;
        }

        /**
         * Adds a scoped binding to the given provider function. If no {@link Scope} can be found corresponding to
         * the requested scope annotation type, then this provider will be registered as an unscoped binding.
         *
         * @param provider unscoped function for creating the binding value
         * @return builder DSL for ConfigurableInstanceFactory
         */
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
