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

import org.apache.logging.log4j.plugins.di.DependentScoped;
import org.apache.logging.log4j.core.config.di.api.bean.Bean;
import org.apache.logging.log4j.core.config.di.api.bean.InitializationContext;
import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;
import org.apache.logging.log4j.core.config.di.api.model.MetaClass;
import org.apache.logging.log4j.core.config.di.api.model.Qualifiers;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.util.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;

class OptionalBean<T> implements Bean<Optional<T>> {
    private final Type type;
    private final Qualifiers qualifiers;
    private final Value<Optional<Bean<T>>> optionalBean;
    private final BiFunction<Bean<T>, InitializationContext<?>, T> getBeanValue;

    OptionalBean(final Type type, final Qualifiers qualifiers, final Value<Optional<Bean<T>>> optionalBean,
                 final BiFunction<Bean<T>, InitializationContext<?>, T> getBeanValue) {
        this.type = type;
        this.qualifiers = qualifiers;
        this.optionalBean = optionalBean;
        this.getBeanValue = getBeanValue;
    }

    @Override
    public Collection<Type> getTypes() {
        return TypeUtil.getTypeClosure(type);
    }

    @Override
    public Qualifiers getQualifiers() {
        return qualifiers;
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
    public MetaClass<?> getDeclaringClass() {
        return optionalBean.get().map(Bean::getDeclaringClass).orElse(null);
    }
}
