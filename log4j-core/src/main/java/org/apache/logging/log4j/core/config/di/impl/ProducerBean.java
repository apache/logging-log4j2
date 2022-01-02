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

import org.apache.logging.log4j.core.config.di.IllegalProductException;
import org.apache.logging.log4j.core.config.di.InitializationContext;
import org.apache.logging.log4j.core.config.di.InjectionPoint;
import org.apache.logging.log4j.core.config.di.Producer;
import org.apache.logging.log4j.core.config.di.ProducerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

class ProducerBean<T> extends AbstractBean<T> {
    private final Producer<T> producer;
    private final Type type;

    ProducerBean(final Collection<Type> types, final String name, final Class<? extends Annotation> scopeType,
                 final Class<?> declaringClass, final ProducerFactory factory) {
        super(types, name, scopeType, declaringClass);
        Objects.requireNonNull(factory);
        producer = factory.createProducer(this);
        if (producer instanceof AbstractProducer<?, ?>) {
            type = ((AbstractProducer<?, ?>) producer).getType();
        } else {
            type = types.iterator().next();
        }
    }

    Type getType() {
        return type;
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return producer.getInjectionPoints();
    }

    @Override
    public T create(final InitializationContext<T> context) {
        final T instance = producer.produce(context);
        if (instance == null) {
            throw new IllegalProductException("Producer created null instance: " + producer);
        }
        // FIXME: this logic doesn't seem to be in Weld? only seems to be required in InjectionTargetBean
        if (isDependentScoped()) {
            context.addIncompleteInstance(instance);
        }
        return instance;
    }

    @Override
    public void destroy(final T instance, final InitializationContext<T> context) {
        try {
            if (isDependentScoped()) {
                producer.dispose(instance);
            }
        } finally {
            context.close();
        }
    }

    @Override
    public String toString() {
        return "ProducerBean{" +
                "types=" + getTypes() +
                ", scope=@" + getScopeType().getSimpleName() +
                ", name=" + getName() +
                ", declaringClass=" + getDeclaringClass() +
                '}';
    }

    @Override
    boolean isTrackingDependencies() {
        return producer instanceof AbstractProducer<?, ?> && ((AbstractProducer<?, ?>) producer).hasDisposerMethod();
    }
}
