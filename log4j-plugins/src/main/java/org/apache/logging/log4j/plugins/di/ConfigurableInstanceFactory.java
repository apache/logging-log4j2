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
import java.lang.reflect.AnnotatedElement;
import java.util.function.Supplier;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.FactoryType;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.InstancePostProcessor;
import org.apache.logging.log4j.plugins.di.spi.ReflectionAgent;
import org.apache.logging.log4j.plugins.di.spi.Scope;
import org.apache.logging.log4j.plugins.internal.util.AnnotatedAnnotation;
import org.apache.logging.log4j.plugins.internal.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.validation.Constraint;
import org.apache.logging.log4j.plugins.validation.ConstraintValidationException;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.util.Cast;
import org.jspecify.annotations.Nullable;

/**
 * Configuration manager for the state of an instance factory. Configurable instance factories can form a
 * hierarchy by {@linkplain #newChildInstanceFactory}  creating children factories} to enable inheritance
 * of bindings, scopes, factory resolvers, instance post-processors, and reflection agents.
 */
public interface ConfigurableInstanceFactory extends InstanceFactory {
    /**
     * Gets the registered Scope strategy for the given scope annotation type if defined or {@code null} otherwise.
     *
     * @param scopeType scope annotation type
     * @return the registered scope instance for the provided scope type
     */
    @Nullable
    Scope getRegisteredScope(final Class<? extends Annotation> scopeType);

    /**
     * Registers a scope annotation type to the given Scope strategy.
     *
     * @param scopeType scope annotation type
     * @param scope     scope strategy to use for the given scope type
     */
    void registerScope(final Class<? extends Annotation> scopeType, final Scope scope);

    /**
     * Registers a bundle. A bundle is an instance of a class with methods annotated with
     * {@link FactoryType}-annotated annotations which provide dependency-injected bindings. These bindings are
     * registered based on {@linkplain org.apache.logging.log4j.plugins.condition.Conditional conditional annotations}
     * if present.
     *
     * @param bundle bundle to install with factory methods for factories
     */
    void registerBundle(final Object bundle);

    /**
     * Registers multiple bundles.
     *
     * @param bundles bundles to install
     * @see #registerBundle(Object)
     */
    default void registerBundles(final Object... bundles) {
        for (final Object bundle : bundles) {
            registerBundle(bundle);
        }
    }

    /**
     * Registers a binding between a key and factory. This overwrites any existing binding the key may have had.
     *
     * @param key key for binding
     * @param factory factory for value to bind
     * @param <T> type of value returned by factory
     */
    <T> void registerBinding(final Key<? super T> key, final Supplier<T> factory);

    /**
     * Registers a binding between a key and factory only if no binding exists for that key.
     *
     * @param key key for binding
     * @param factory factory for value to bind
     * @param <T> type of value returned by factory
     */
    <T> void registerBindingIfAbsent(final Key<? super T> key, final Supplier<T> factory);

    /**
     * Removes any existing binding for the provided key.
     *
     * @param key key to remove previously registered bindings
     */
    void removeBinding(final Key<?> key);

    /**
     * Registers extensions for dependency injection.
     *
     * @param extension instance of extension to register
     * @see FactoryResolver
     * @see InstancePostProcessor
     * @see ReflectionAgent
     * @see AnnotatedElementNameProvider
     * @see AnnotatedElementAliasesProvider
     *
     * @implNote Factory resolvers are shared between parent and child {@link InstanceFactory} objects.
     * Child factories inherit copies of registered {@link InstancePostProcessor} objects (which are kept in
     * {@link Ordered} order), but processors added to child factories are not copied to the parent.
     * Use of a custom {@link ReflectionAgent} is specific to this factory and is inherited by child factories
     * unless overridden. Name and alias providers are shared by all factories.
     */
    void registerExtension(final Object extension);

    /**
     * Creates a new child instance factory from this factory which uses bindings from this factory as fallback
     * bindings.
     *
     * @return new child instance factory
     */
    ConfigurableInstanceFactory newChildInstanceFactory();

    /**
     * Creates a new child instance factory from this factory which uses bindings from this factory as fallback
     * bindings.
     *
     * @return new child instance factory
     */
    ConfigurableInstanceFactory newChildInstanceFactory(
            Supplier<PropertyEnvironment> environment, Supplier<ClassLoader> loader);

    /**
     * Injects dependencies into the members of the provided instance. Injectable fields are set, then injectable methods are
     * invoked (first those with parameters, then those without parameters).
     *
     * @param instance instance in which to inject member dependencies
     */
    void injectMembers(final Object instance);

    /**
     * Runs discovered constraint validators for the given annotated element, name, and value.
     *
     * @param element source of the value to check for constraint annotations
     * @param name    name to use for error reporting
     * @param value   value to validate
     * @throws ConstraintValidationException if validation fails
     */
    default void validate(final AnnotatedElement element, final String name, final @Nullable Object value) {
        // TODO(ms): can maybe move logic into a post processor
        final long errors = AnnotationUtil.findAnnotatedAnnotations(element, Constraint.class)
                .map(this::initialize)
                .filter(validator -> !validator.isValid(name, value))
                .count();
        if (errors > 0) {
            throw new ConstraintValidationException(element, name, value);
        }
    }

    private <A extends Annotation> ConstraintValidator<A> initialize(
            final AnnotatedAnnotation<A, Constraint> constraint) {
        final Class<? extends ConstraintValidator<A>> validatorType =
                Cast.cast(constraint.metaAnnotation().value());
        final ConstraintValidator<A> validator = getInstance(validatorType);
        validator.initialize(constraint.annotation());
        return validator;
    }
}
