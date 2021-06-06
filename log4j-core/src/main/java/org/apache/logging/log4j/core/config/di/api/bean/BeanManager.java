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

package org.apache.logging.log4j.core.config.di.api.bean;

import org.apache.logging.log4j.core.config.di.DefinitionException;
import org.apache.logging.log4j.core.config.di.UnsatisfiedBeanException;
import org.apache.logging.log4j.core.config.di.ValidationException;
import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;
import org.apache.logging.log4j.core.config.di.api.model.Qualifiers;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Central SPI for injecting and managing beans and their instances.
 */
public interface BeanManager extends AutoCloseable {

    /**
     * Loads beans from the given classes. This looks for injectable classes and producers in the provided classes,
     * loads them into this manager, and returns the loaded beans.
     *
     * @param beanClasses classes to load beans from
     * @return beans loaded from the given classes
     */
    Collection<Bean<?>> loadBeans(final Collection<Class<?>> beanClasses);

    /**
     * Loads beans from the given classes. This looks for injectable classes and producers, registers them in this
     * manager, validates them, then returns the validated beans.
     *
     * @param beanClasses classes to load beans from
     * @throws ValidationException if any beans have validation errors
     */
    default void loadAndValidateBeans(final Class<?>... beanClasses) {
        final Collection<Bean<?>> beans = loadBeans(Arrays.asList(beanClasses));
        validateBeans(beans);
    }

    /**
     * Validates beans and throws a {@link ValidationException} if there are any errors.
     *
     * @param beans beans to check for validation errors
     * @throws ValidationException if any beans have validation errors
     */
    void validateBeans(final Iterable<Bean<?>> beans);

    /**
     * Validates the given injection point.
     *
     * @param point injection point to validate
     * @throws DefinitionException      if the injection point is improperly defined
     * @throws UnsatisfiedBeanException if no beans can satisfy the injection point
     */
    void validateInjectionPoint(InjectionPoint point);

    <T> Optional<Bean<T>> getBean(final Type type, final Qualifiers qualifiers);

    /**
     * Creates an InitializationContext for a given Bean instance for use in dependency injection SPIs.
     *
     * @param bean bean to create an initialization context for
     * @param <T>  type of object created by bean
     * @return new InitializationContext for the given Bean
     */
    <T> InitializationContext<T> createInitializationContext(final Bean<T> bean);

    /**
     * Gets or creates the value for a given bean inside a given InitializationContext.
     *
     * @param bean          bean to get or create value for
     * @param parentContext which context this bean is being used in
     * @param <T>           type of value
     * @return value of the bean in the given context
     */
    <T> T getValue(final Bean<T> bean, final InitializationContext<?> parentContext);

    /**
     * Gets the value to use for injecting into a given InjectionPoint in a given InitializationContext.
     *
     * @param point         location where injectable value would be injected
     * @param parentContext which context this value is being injected under
     * @param <T>           type of injectable value
     * @return value to inject if defined or empty otherwise
     */
    <T> Optional<T> getInjectableValue(final InjectionPoint point, final InitializationContext<?> parentContext);

    @Override
    void close();

    // TODO: integrate with constraint validators
    // TODO: integrate with TypeConverters
    // TODO: need some sort of default value strategy to bridge over @PluginAttribute and optional injected values
    // TODO: need to support @PluginAliases still
    // TODO: add support for injecting collections and arrays
}
