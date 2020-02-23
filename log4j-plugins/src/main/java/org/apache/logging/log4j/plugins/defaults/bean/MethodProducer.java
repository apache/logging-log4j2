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

import org.apache.logging.log4j.plugins.spi.InjectionException;
import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.scope.InitializationContext;

import java.lang.reflect.Type;
import java.util.Collection;

class MethodProducer<D, T> extends AbstractProducer<D, T> {
    private final MetaMethod<D, T> producerMethod;
    private final Collection<InjectionPoint<?>> producerInjectionPoints;

    MethodProducer(final BeanManager beanManager, final Bean<D> declaringBean,
                   final MetaMethod<D, T> producerMethod, final Collection<InjectionPoint<?>> producerInjectionPoints,
                   final MetaMethod<D, ?> disposerMethod, final Collection<InjectionPoint<?>> disposerInjectionPoints) {
        super(beanManager, declaringBean, disposerMethod, disposerInjectionPoints);
        if (!producerMethod.isStatic() && declaringBean == null) {
            // TODO: more informative error message
            throw new InjectionException("Instance method annotated @Produces must be associated with a declaring bean");
        }
        this.producerMethod = producerMethod;
        this.producerInjectionPoints = producerInjectionPoints;
    }

    @Override
    Type getType() {
        return producerMethod.getBaseType();
    }

    @Override
    public T produce(final InitializationContext<T> context) {
        try (final InitializationContext<D> parentContext = createContext()) {
            final D declaringInstance = producerMethod.isStatic() ? null : getDeclaringInstance(parentContext);
            return injectionContextFactory.forMethod(producerMethod, producerInjectionPoints, declaringInstance)
                    .invoke(context);
        }
    }

    @Override
    public Collection<InjectionPoint<?>> getInjectionPoints() {
        return producerInjectionPoints;
    }
}
