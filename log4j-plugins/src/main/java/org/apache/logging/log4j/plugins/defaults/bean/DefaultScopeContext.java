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

import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.ScopeContext;
import org.apache.logging.log4j.plugins.spi.bean.Scoped;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultScopeContext implements ScopeContext {
    private final Map<Scoped<?>, ScopedInstance<?>> cache = new ConcurrentHashMap<>();
    private final Class<? extends Annotation> scopeType;

    public DefaultScopeContext(final Class<? extends Annotation> scopeType) {
        this.scopeType = Objects.requireNonNull(scopeType);
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return scopeType;
    }

    @Override
    public <T> T getOrCreate(final Scoped<T> scoped, final InitializationContext<T> context) {
        final ScopedInstance<T> scopedInstance =
                TypeUtil.cast(cache.computeIfAbsent(scoped, ignored -> new LazyScopedInstance<>(scoped, context)));
        return scopedInstance.getInstance();
    }

    @Override
    public <T> Optional<T> getIfExists(final Scoped<T> scoped) {
        final ScopedInstance<T> scopedInstance = TypeUtil.cast(cache.get(scoped));
        return scopedInstance == null ? Optional.empty() : Optional.of(scopedInstance.getInstance());
    }

    @Override
    public void destroy(final Scoped<?> scoped) {
        final ScopedInstance<?> scopedInstance = cache.get(scoped);
        if (scopedInstance != null) {
            scopedInstance.close();
            cache.remove(scoped, scopedInstance);
        }
    }

    @Override
    public void close() {
        cache.keySet().forEach(this::destroy);
    }
}
