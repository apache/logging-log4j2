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
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.ScopeContext;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultScopeContext implements ScopeContext {
    private final Map<Bean<?>, BeanInstance<?>> cache = new ConcurrentHashMap<>();
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
        final BeanInstance<T> beanInstance =
                TypeUtil.cast(cache.computeIfAbsent(bean, ignored -> new LazyBeanInstance<>(bean, context)));
        return beanInstance.getInstance();
    }

    @Override
    public <T> Optional<T> getIfExists(final Bean<T> bean) {
        final BeanInstance<T> beanInstance = TypeUtil.cast(cache.get(bean));
        return beanInstance == null ? Optional.empty() : Optional.of(beanInstance.getInstance());
    }

    @Override
    public void destroy(final Bean<?> bean) {
        final BeanInstance<?> beanInstance = cache.get(bean);
        if (beanInstance != null) {
            beanInstance.close();
            cache.remove(bean, beanInstance);
        }
    }

    @Override
    public void close() {
        cache.keySet().forEach(this::destroy);
    }
}
