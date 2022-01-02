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
import org.apache.logging.log4j.plugins.spi.InitializationContext;
import org.apache.logging.log4j.plugins.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.di.DependentScoped;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.util.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;

class OptionalBean<T> implements Bean<Optional<T>> {
    private final Collection<Type> types;
    private final String name;
    private final Value<Optional<Bean<T>>> optionalBean;
    private final BiFunction<Bean<T>, InitializationContext<?>, T> getBeanValue;

    OptionalBean(final Type type, final String name, final Value<Optional<Bean<T>>> optionalBean,
                 final BiFunction<Bean<T>, InitializationContext<?>, T> getBeanValue) {
        this.types = TypeUtil.getTypeClosure(type);
        this.name = name;
        this.optionalBean = optionalBean;
        this.getBeanValue = getBeanValue;
    }

    @Override
    public Collection<Type> getTypes() {
        return types;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        final Optional<Class<? extends Annotation>> scopeType = optionalBean.get().map(Bean::getScopeType);
        return scopeType.orElse(DependentScoped.class);
    }

    @Override
    public Optional<T> create(final InitializationContext<Optional<T>> context) {
        return optionalBean.get().map(bean -> getBeanValue.apply(bean, context));
    }

    @Override
    public void destroy(final Optional<T> instance, final InitializationContext<Optional<T>> context) {
        context.close();
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return optionalBean.get().map(Bean::getInjectionPoints).orElse(Collections.emptySet());
    }

    @Override
    public Class<?> getDeclaringClass() {
        return optionalBean.get().map(Bean::getDeclaringClass).orElse(null);
    }
}
