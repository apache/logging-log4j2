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

package org.apache.logging.log4j.plugins.spi.bean;

import org.apache.logging.log4j.plugins.spi.ValidationException;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;
import org.apache.logging.log4j.plugins.spi.scope.Scoped;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Central SPI for injecting and managing beans and their instances.
 */
public interface BeanManager extends AutoCloseable {

    /**
     * Loads beans from the given classes. This looks for injectable classes and producers, validates them, registers
     * them in this manager, then returns the loaded beans.
     *
     * @param beanClasses classes to load beans from
     * @return beans loaded from the given classes
     */
    Collection<Bean<?>> loadBeans(final Collection<Class<?>> beanClasses);

    /**
     * Loads beans from the given classes. This looks for injectable classes and producers, validates them, registers
     * them in this manager, then returns the loaded beans.
     *
     * @param beanClasses classes to load beans from
     * @return beans loaded from the given classes
     */
    default Collection<Bean<?>> loadBeans(final Class<?>... beanClasses) {
        return loadBeans(Arrays.asList(beanClasses));
    }

    /**
     * Validates beans and throws a {@link ValidationException} if there are any errors.
     *
     * @param beans beans to check for validation errors
     */
    void validateBeans(final Iterable<Bean<?>> beans);

    // TODO: re-add query methods for beans as needed
//    Collection<Bean<?>> getBeans();

    /**
     * Creates an InitializationContext for a given Scoped instance for use in dependency injection SPIs.
     *
     * @param scoped scoped object to create an initialization context for
     * @param <T>    type of object created by scope
     * @return new InitializationContext for the given Scoped
     */
    <T> InitializationContext<T> createInitializationContext(final Scoped<T> scoped);

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
    <T> Optional<T> getInjectableValue(final InjectionPoint<T> point, final InitializationContext<?> parentContext);

    @Override
    void close();

    // TODO: integrate with constraint validators
    // TODO: integrate with TypeConverters
    // TODO: need some sort of default value strategy to bridge over @PluginAttribute and optional injected values
    // TODO: need to support @PluginAliases still
    // TODO: add support for injecting collections and arrays
}
