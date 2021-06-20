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

package org.apache.logging.log4j.core.config.di.impl;

import org.apache.logging.log4j.core.config.di.InitializationContext;
import org.apache.logging.log4j.core.config.di.InitializationException;
import org.apache.logging.log4j.core.config.di.InjectionPoint;
import org.apache.logging.log4j.core.config.di.InjectionTarget;
import org.apache.logging.log4j.plugins.di.Disposes;
import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Produces;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class DefaultInjectionTarget<T> implements InjectionTarget<T> {
    private final Injector injector;
    private final Class<T> metaClass;
    private final Collection<InjectionPoint> injectionPoints;
    private final Constructor<T> constructor;
    private final List<Method> postConstructMethods;
    private final List<Method> preDestroyMethods;

    DefaultInjectionTarget(final Injector injector, final Class<T> metaClass,
                           final Collection<InjectionPoint> injectionPoints, final Constructor<T> constructor,
                           final List<Method> postConstructMethods, final List<Method> preDestroyMethods) {
        this.injector = injector;
        this.metaClass = metaClass;
        this.injectionPoints = Objects.requireNonNull(injectionPoints);
        this.constructor = Objects.requireNonNull(constructor);
        this.postConstructMethods = Objects.requireNonNull(postConstructMethods);
        this.preDestroyMethods = Objects.requireNonNull(preDestroyMethods);
    }

    @Override
    public T produce(final InitializationContext<T> context) {
        final Set<InjectionPoint> constructorInjectionPoints = injectionPoints.stream()
                .filter(point -> constructor.equals(point.getMember()))
                .collect(Collectors.toSet());
        return injector.construct(constructor, constructorInjectionPoints, context);
    }

    @Override
    public void inject(final T instance, final InitializationContext<T> context) {
        injectFields(instance, context);
        injectMethods(instance, context);
    }

    private void injectFields(final T instance, final InitializationContext<T> context) {
        injectionPoints.stream()
                .filter(point -> point.getElement() instanceof Field)
                .forEachOrdered(point -> injector.set(instance, TypeUtil.cast(point.getElement()), point, context));
    }

    private void injectMethods(final T instance, final InitializationContext<T> context) {
        final Set<Method> injectedMethods = new HashSet<>();
        for (final InjectionPoint point : injectionPoints) {
            final Member member = point.getMember();
            final AnnotatedElement element = point.getElement();
            if (member instanceof Method && !injectedMethods.contains(member) &&
                    !AnnotationUtil.isAnnotationPresent(element, Produces.class) &&
                    !AnnotationUtil.isAnnotationPresent(element, Disposes.class)) {
                final Method method = TypeUtil.cast(member);
                final Set<InjectionPoint> methodInjectionPoints = injectionPoints.stream()
                        .filter(p -> method.equals(p.getMember()))
                        .collect(Collectors.toSet());
                injector.invoke(instance, method, methodInjectionPoints, context);
                injectedMethods.add(method);
            }
        }
        for (final Method method : metaClass.getMethods()) {
            if (method.isAnnotationPresent(Inject.class) && method.getParameterCount() == 0) {
                injector.invoke(instance, method, Collections.emptySet(), context);
            }
        }
    }

    @Override
    public void postConstruct(final T instance) {
        for (final Method method : postConstructMethods) {
            try {
                method.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new InitializationException("Error invoking post construct method " + method.getName(), e);
            }
        }
    }

    @Override
    public void preDestroy(final T instance) {
        for (final Method method : preDestroyMethods) {
            try {
                method.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new InitializationException("Error invoking pre destroy method " + method.getName(), e);
            }
        }
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    boolean hasPreDestroyMethods() {
        return !preDestroyMethods.isEmpty();
    }
}
