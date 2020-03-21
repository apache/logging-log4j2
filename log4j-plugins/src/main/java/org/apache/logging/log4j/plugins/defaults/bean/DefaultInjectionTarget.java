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

import org.apache.logging.log4j.plugins.api.Disposes;
import org.apache.logging.log4j.plugins.api.Inject;
import org.apache.logging.log4j.plugins.api.Produces;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.InjectionTarget;
import org.apache.logging.log4j.plugins.spi.bean.Injector;
import org.apache.logging.log4j.plugins.spi.model.ElementManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.model.MetaConstructor;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaMember;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class DefaultInjectionTarget<T> implements InjectionTarget<T> {
    private final Injector injector;
    private final ElementManager elementManager;
    private final Collection<InjectionPoint> injectionPoints;
    private final MetaConstructor<T> constructor;
    private final List<MetaMethod<T, ?>> postConstructMethods;
    private final List<MetaMethod<T, ?>> preDestroyMethods;

    DefaultInjectionTarget(final Injector injector, final ElementManager elementManager,
                           final Collection<InjectionPoint> injectionPoints, final MetaConstructor<T> constructor,
                           final List<MetaMethod<T, ?>> postConstructMethods, final List<MetaMethod<T, ?>> preDestroyMethods) {
        this.injector = injector;
        this.elementManager = elementManager;
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
        final Class<T> clazz = TypeUtil.cast(instance.getClass());
        final MetaClass<T> metaClass = elementManager.getMetaClass(clazz);
        for (final MetaMethod<T, ?> method : metaClass.getMethods()) {
            if (method.isAnnotationPresent(Inject.class) && method.getParameters().isEmpty()) {
                injector.invoke(instance, method, Collections.emptySet(), context);
            }
        }
    }

    private void injectFields(final T instance, final InitializationContext<T> context) {
        injectionPoints.stream()
                .filter(point -> point.getElement() instanceof MetaField<?, ?>)
                .forEachOrdered(point -> injector.set(instance, TypeUtil.cast(point.getElement()), point, context));
    }

    private void injectMethods(final T instance, final InitializationContext<T> context) {
        final Set<MetaMember<?>> injectedMethods = new HashSet<>();
        for (final InjectionPoint point : injectionPoints) {
            if (point.getMember() instanceof MetaMethod<?, ?> &&
                    point.getMember().getDeclaringClass().getJavaClass().isInstance(instance) &&
                    !injectedMethods.contains(point.getMember()) &&
                    !point.getElement().isAnnotationPresent(Produces.class) &&
                    !point.getElement().isAnnotationPresent(Disposes.class)) {
                final MetaMethod<T, ?> method = TypeUtil.cast(point.getMember());
                final Set<InjectionPoint> methodInjectionPoints = injectionPoints.stream()
                        .filter(p -> method.equals(p.getMember()))
                        .collect(Collectors.toSet());
                injector.invoke(instance, method, methodInjectionPoints, context);
                injectedMethods.add(method);
            }
        }
    }

    @Override
    public void postConstruct(final T instance) {
        for (final MetaMethod<T, ?> method : postConstructMethods) {
            method.invoke(instance);
        }
    }

    @Override
    public void preDestroy(final T instance) {
        for (final MetaMethod<T, ?> method : preDestroyMethods) {
            method.invoke(instance);
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
