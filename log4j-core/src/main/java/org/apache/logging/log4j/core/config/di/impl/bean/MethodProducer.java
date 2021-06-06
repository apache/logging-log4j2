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

package org.apache.logging.log4j.core.config.di.impl.bean;

import org.apache.logging.log4j.core.config.di.DefinitionException;
import org.apache.logging.log4j.core.config.di.api.bean.Bean;
import org.apache.logging.log4j.core.config.di.api.bean.BeanManager;
import org.apache.logging.log4j.core.config.di.api.bean.InitializationContext;
import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;
import org.apache.logging.log4j.core.config.di.api.model.MetaMethod;

import java.lang.reflect.Type;
import java.util.Collection;

class MethodProducer<P, T> extends AbstractProducer<P, T> {
    private final MetaMethod<P, T> producerMethod;
    private final Collection<InjectionPoint> producerInjectionPoints;

    MethodProducer(final BeanManager beanManager, final Bean<P> producerBean,
                   final MetaMethod<P, T> producerMethod, final Collection<InjectionPoint> producerInjectionPoints,
                   final MetaMethod<P, ?> disposerMethod, final Collection<InjectionPoint> disposerInjectionPoints) {
        super(beanManager, producerBean, disposerMethod, disposerInjectionPoints);
        if (!producerMethod.isStatic() && producerBean == null) {
            throw new DefinitionException("Producer instance method must be in a bean");
        }
        this.producerMethod = producerMethod;
        this.producerInjectionPoints = producerInjectionPoints;
    }

    @Override
    Type getType() {
        return producerMethod.getType();
    }

    @Override
    public T produce(final InitializationContext<T> context) {
        try (final InitializationContext<P> parentContext = createContext()) {
            final P declaringInstance = producerMethod.isStatic() ? null : getProducerInstance(parentContext);
            return injector.produce(declaringInstance, producerMethod, producerInjectionPoints, parentContext);
        }
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return producerInjectionPoints;
    }
}
