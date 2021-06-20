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

import org.apache.logging.log4j.core.config.di.Bean;
import org.apache.logging.log4j.core.config.di.BeanManager;
import org.apache.logging.log4j.core.config.di.InitializationContext;
import org.apache.logging.log4j.core.config.di.InjectionPoint;
import org.apache.logging.log4j.core.config.di.Producer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

abstract class AbstractProducer<P, T> implements Producer<T> {
    private final BeanManager beanManager;
    private final Bean<P> producerBean;
    private final Method disposerMethod;
    private final Collection<InjectionPoint> disposerInjectionPoints;
    final Injector injector;

    AbstractProducer(final BeanManager beanManager, final Bean<P> producerBean, final Method disposerMethod,
                     final Collection<InjectionPoint> disposerInjectionPoints) {
        this.beanManager = beanManager;
        this.producerBean = producerBean;
        this.disposerMethod = disposerMethod;
        this.disposerInjectionPoints = Objects.requireNonNull(disposerInjectionPoints);
        this.injector = new Injector(beanManager);
    }

    // context is managed separately as the declaring instance is only used for producing the object and is not a dependent of the bean
    InitializationContext<P> createContext() {
        return beanManager.createInitializationContext(producerBean);
    }

    P getProducerInstance(final InitializationContext<P> context) {
        return context.getIncompleteInstance(producerBean).orElseGet(() ->
                beanManager.getValue(producerBean, context.createProducerContext(producerBean)));
    }

    abstract Type getType();

    boolean hasDisposerMethod() {
        return disposerMethod != null;
    }

    @Override
    public void dispose(final T instance) {
        if (hasDisposerMethod()) {
            // as producer and disposer bean is unrelated to this bean, we need to recreate it on demand
            try (final InitializationContext<P> context = createContext()) {
                final P declaringInstance = Modifier.isStatic(disposerMethod.getModifiers()) ? null : getProducerInstance(context);
                injector.dispose(declaringInstance, disposerMethod, disposerInjectionPoints, instance, context);
            }
        }
    }
}
