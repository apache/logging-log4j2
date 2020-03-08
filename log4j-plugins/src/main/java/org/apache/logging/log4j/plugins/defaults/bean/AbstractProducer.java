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

import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.Injector;
import org.apache.logging.log4j.plugins.spi.bean.Producer;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

abstract class AbstractProducer<D, T> implements Producer<T> {
    private final BeanManager beanManager;

    private final Bean<D> declaringBean;
    private final MetaMethod<D, ?> disposerMethod;
    private final Collection<InjectionPoint<?>> disposerInjectionPoints;
    final Injector injector;

    AbstractProducer(final BeanManager beanManager, final Bean<D> declaringBean, final MetaMethod<D, ?> disposerMethod,
                     final Collection<InjectionPoint<?>> disposerInjectionPoints) {
        this.beanManager = beanManager;
        this.declaringBean = declaringBean;
        this.disposerMethod = disposerMethod;
        this.disposerInjectionPoints = Objects.requireNonNull(disposerInjectionPoints);
        this.injector = new DefaultInjector(beanManager);
    }

    // context is managed separately as the declaring instance is only used for producing the object and is not a dependent of the bean
    InitializationContext<D> createContext() {
        return beanManager.createInitializationContext(declaringBean);
    }

    D getDeclaringInstance(final InitializationContext<D> context) {
        return context.getIncompleteInstance(declaringBean).orElseGet(() ->
                beanManager.getValue(declaringBean, context.createIndependentContext(declaringBean)));
    }

    abstract Type getType();

    boolean hasDisposerMethod() {
        return disposerMethod != null;
    }

    @Override
    public void dispose(final T instance) {
        if (hasDisposerMethod()) {
            // as producer and disposer bean is unrelated to this bean, we need to recreate it on demand
            try (final InitializationContext<D> context = createContext()) {
                final D declaringInstance = disposerMethod.isStatic() ? null : getDeclaringInstance(context);
                injector.dispose(declaringInstance, disposerMethod, disposerInjectionPoints, instance, context);
            }
        }
    }
}
