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
import org.apache.logging.log4j.core.config.di.ProviderFactory;
import org.apache.logging.log4j.plugins.di.Provider;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

class ProvidedBean<T> implements Bean<T> {
    private final Collection<Type> types;
    private final Bean<Provider<T>> providerBean;
    private final ProviderFactory<T> providerFactory;

    ProvidedBean(final Type type, final Bean<Provider<T>> providerBean, final ProviderFactory<T> providerFactory) {
        this.types = TypeUtil.getTypeClosure(type);
        this.providerBean = providerBean;
        this.providerFactory = providerFactory;
    }

    @Override
    public T create(final InitializationContext<T> context) {
        return providerFactory.getProvider(context).get();
    }

    @Override
    public void destroy(final T instance, final InitializationContext<T> context) {
        context.close();
    }

    @Override
    public Collection<InjectionPoint> getInjectionPoints() {
        return providerBean.getInjectionPoints();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return providerBean.getDeclaringClass();
    }

    @Override
    public Collection<Type> getTypes() {
        return types;
    }

    @Override
    public String getName() {
        return providerBean.getName();
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return providerBean.getScopeType();
    }
}
