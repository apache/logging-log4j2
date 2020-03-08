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
import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.Injector;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaConstructor;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;

import java.util.Collection;
import java.util.List;

public class DefaultInjector implements Injector {
    private final BeanManager beanManager;

    public DefaultInjector(final BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    private Object[] createArguments(final List<MetaParameter<?>> parameters,
                                     final Collection<InjectionPoint<?>> injectionPoints,
                                     final InitializationContext<?> context, final Object producedInstance) {
        return parameters.stream().map(parameter ->
                parameter.isAnnotationPresent(Disposes.class) ? producedInstance : injectionPoints.stream()
                        .filter(point -> parameter.equals(point.getElement()))
                        .findAny()
                        .flatMap(point -> beanManager.getInjectableValue(point, context))
                        .orElseThrow(() -> new UnsupportedOperationException("TODO: primitives and defaults")))
                .toArray();
    }

    @Override
    public <T> T construct(final MetaConstructor<T> constructor, final Collection<InjectionPoint<?>> points,
                           final InitializationContext<T> context) {
        return constructor.construct(createArguments(constructor.getParameters(), points, context, null));
    }

    @Override
    public <D, T> T produce(final D producerInstance, final MetaMethod<D, T> producerMethod,
                            final Collection<InjectionPoint<?>> points, final InitializationContext<T> context) {
        return producerMethod.invoke(producerInstance, createArguments(producerMethod.getParameters(), points, context, null));
    }

    @Override
    public <T> void dispose(final T disposerInstance, final MetaMethod<T, ?> disposerMethod,
                            final Collection<InjectionPoint<?>> points, final Object instance,
                            final InitializationContext<T> context) {
        disposerMethod.invoke(disposerInstance, createArguments(disposerMethod.getParameters(), points, context, instance));
    }

    @Override
    public <T> void invoke(final T instance, final MetaMethod<T, ?> method, final Collection<InjectionPoint<?>> points,
                           final InitializationContext<T> context) {
        method.invoke(instance, createArguments(method.getParameters(), points, context, null));
    }

    @Override
    public <D, T> void set(final D instance, final MetaField<D, T> field, final InjectionPoint<T> point,
                           final InitializationContext<D> context) {
        beanManager.getInjectableValue(point, context).ifPresent(value -> field.set(instance, value));
    }
}
