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
import org.apache.logging.log4j.core.config.di.InitializationContext;
import org.apache.logging.log4j.core.config.di.InjectionPoint;
import org.apache.logging.log4j.plugins.di.Provider;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Function;

class ProviderBean<T> implements Bean<Provider<T>> {
    private final Collection<Type> types;
    private final Bean<T> bean;
    private final Function<InitializationContext<?>, Provider<T>> providerFactory;

    ProviderBean(final Type providerType, final Bean<T> bean,
                 final Function<InitializationContext<?>, Provider<T>> providerFactory) {
        this.types = TypeUtil.getTypeClosure(providerType);
        this.bean = bean;
        this.providerFactory = providerFactory;
    }

    @Override
    public Provider<T> create(final InitializationContext<Provider<T>> context) {
        return providerFactory.apply(context);
    }

    @Override
    public void destroy(final Provider<T> instance, final InitializationContext<Provider<T>> context) {
        context.close();
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return bean.getInjectionPoints();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return bean.getDeclaringClass();
    }

    @Override
    public Collection<Type> getTypes() {
        return types;
    }

    @Override
    public String getName() {
        return bean.getName();
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return bean.getScopeType();
    }
}
