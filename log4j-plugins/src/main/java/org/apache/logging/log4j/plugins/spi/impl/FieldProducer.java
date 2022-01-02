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

import org.apache.logging.log4j.plugins.spi.Bean;
import org.apache.logging.log4j.plugins.spi.BeanManager;
import org.apache.logging.log4j.plugins.spi.InitializationContext;
import org.apache.logging.log4j.plugins.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

class FieldProducer<P, T> extends AbstractProducer<P, T> {
    private final Field field;

    FieldProducer(final BeanManager beanManager, final Bean<P> producerBean, final Field field,
                  final Method disposerMethod, final Collection<InjectionPoint> disposerInjectionPoints) {
        super(beanManager, producerBean, disposerMethod, disposerInjectionPoints);
        this.field = field;
    }

    @Override
    Type getType() {
        return field.getType();
    }

    @Override
    public T produce(final InitializationContext<T> context) {
        if (Modifier.isStatic(field.getModifiers())) {
            try {
                return TypeUtil.cast(field.get(null));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        try (final InitializationContext<P> parentContext = createContext()) {
            return TypeUtil.cast(field.get(getProducerInstance(parentContext)));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }
}
