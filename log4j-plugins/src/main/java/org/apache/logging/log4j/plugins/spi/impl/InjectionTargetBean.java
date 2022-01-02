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

import org.apache.logging.log4j.plugins.spi.IllegalProductException;
import org.apache.logging.log4j.plugins.spi.InitializationContext;
import org.apache.logging.log4j.plugins.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.InjectionTarget;
import org.apache.logging.log4j.plugins.spi.InjectionTargetFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

class InjectionTargetBean<T> extends AbstractBean<T> {
    private final InjectionTarget<T> injectionTarget;

    InjectionTargetBean(final Collection<Type> types, final String name, final Class<? extends Annotation> scopeType,
                        final Class<T> declaringClass, final InjectionTargetFactory<T> factory) {
        super(types, name, scopeType, declaringClass);
        Objects.requireNonNull(factory);
        injectionTarget = factory.createInjectionTarget(this);
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return injectionTarget.getInjectionPoints();
    }

    @Override
    public T create(final InitializationContext<T> context) {
        final T instance = injectionTarget.produce(context);
        if (instance == null) {
            throw new IllegalProductException("Injection target created null instance: " + injectionTarget);
        }
        injectionTarget.inject(instance, context);
        injectionTarget.postConstruct(instance);
        if (isDependentScoped()) {
            context.addIncompleteInstance(instance);
        }
        return instance;
    }

    @Override
    public void destroy(final T instance, final InitializationContext<T> context) {
        try {
            if (isDependentScoped()) {
                injectionTarget.preDestroy(instance);
            }
        } finally {
            context.close();
        }
    }

    @Override
    public String toString() {
        return "InjectionTargetBean{" +
                "types=" + getTypes() +
                ", scope=@" + getScopeType().getSimpleName() +
                ", name=" + getName() +
                ", declaringClass=" + getDeclaringClass() +
                '}';
    }

    @Override
    boolean isTrackingDependencies() {
        return injectionTarget instanceof DefaultInjectionTarget<?> &&
                ((DefaultInjectionTarget<?>) injectionTarget).hasPreDestroyMethods();
    }
}
