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

import org.apache.logging.log4j.plugins.di.DependentScoped;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Provides lifecycle and dependency injection functionality to managed classes. A bean represents an injectable class
 * via {@link org.apache.logging.log4j.plugins.di.Inject} or a producer field or method via
 * {@link org.apache.logging.log4j.plugins.di.Produces} along with their annotation aliases. A bean has a
 * {@linkplain #getName() name} which can be the empty string to indicate a default bean. Beans provide a
 * {@linkplain #getTypes() type closure} of matching generic types to allow for injecting more complex types. The
 * {@linkplain #getScopeType() scope} of a bean controls the lifecycle of instances
 * {@linkplain #create(InitializationContext) created} and {@linkplain #destroy(Object, InitializationContext) destroyed}
 * by this bean. Dependencies are injected based on metadata exposed through
 * {@linkplain #getInjectionPoints() injection points}.
 *
 * @param <T> type of instance being managed by this bean
 */
public interface Bean<T> {

    /**
     * Returns the name of this bean or an empty string to indicate a default bean.
     */
    String getName();

    /**
     * Returns the type closure of this bean.
     */
    Collection<Type> getTypes();

    default boolean hasMatchingType(final Type requiredType) {
        for (final Type type : getTypes()) {
            if (TypeUtil.typesMatch(requiredType, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the declaring class that creates this bean. For injectable classes, this returns that class. For producing
     * methods and fields, this returns their declaring class.
     */
    Class<?> getDeclaringClass();

    /**
     * Returns the scope type of this bean.
     */
    Class<? extends Annotation> getScopeType();

    default boolean isDependentScoped() {
        return getScopeType() == DependentScoped.class;
    }

    /**
     * Creates a new instance of this bean. The given {@link InitializationContext} should be used by implementations
     * to track dependent objects.
     *
     * @param context the context in which the instance is being managed
     * @return a managed, initialized instance
     */
    T create(final InitializationContext<T> context);

    /**
     * Destroys a managed instance in the given context. Implementations should call {@link InitializationContext#close()} to
     * allow dependent objects to be destroyed.
     *
     * @param instance the managed instance to destroy
     * @param context  the context in which the instance is being managed
     */
    void destroy(final T instance, final InitializationContext<T> context);

    /**
     * Returns the collection of injection points to inject dependencies when constructing instances of this bean.
     */
    Collection<InjectionPoint> getInjectionPoints();
}
