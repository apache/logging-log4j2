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
import org.apache.logging.log4j.core.config.di.ScopeContext;
import org.apache.logging.log4j.plugins.util.LazyValue;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.util.Value;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultScopeContext implements ScopeContext {
    private final Map<Bean<?>, Value<?>> beanValues = new ConcurrentHashMap<>();
    private final Class<? extends Annotation> scopeType;

    public DefaultScopeContext(final Class<? extends Annotation> scopeType) {
        this.scopeType = Objects.requireNonNull(scopeType);
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return scopeType;
    }

    @Override
    public <T> T getOrCreate(final Bean<T> bean, final InitializationContext<T> context) {
        final Value<T> value = TypeUtil.cast(beanValues.computeIfAbsent(bean,
                ignored -> LazyValue.forScope(() -> bean.create(context), instance -> bean.destroy(instance, context))));
        return value.get();
    }

    @Override
    public <T> Optional<T> getIfExists(final Bean<T> bean) {
        return Optional.ofNullable(beanValues.get(bean))
                .map(Value::get)
                .map(TypeUtil::cast);
    }

    @Override
    public void destroy(final Bean<?> bean) {
        beanValues.computeIfPresent(bean, (ignored, value) -> {
            value.close();
            return null;
        });
    }

    @Override
    public void close() {
        beanValues.keySet().forEach(this::destroy);
    }
}
