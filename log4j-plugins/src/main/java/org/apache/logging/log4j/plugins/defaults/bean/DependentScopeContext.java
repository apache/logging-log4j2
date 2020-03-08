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

import org.apache.logging.log4j.plugins.api.Dependent;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.ScopeContext;
import org.apache.logging.log4j.plugins.spi.bean.Scoped;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class DependentScopeContext implements ScopeContext {
    @Override
    public Class<? extends Annotation> getScopeType() {
        return Dependent.class;
    }

    @Override
    public <T> T getOrCreate(final Scoped<T> scoped, final InitializationContext<T> context) {
        if (context == null) {
            return null;
        }
        final T instance = scoped.create(context);
        if (context.isTrackingDependencies(scoped)) {
            context.addDependentInstance(instance);
        }
        return instance;
    }

    @Override
    public <T> Optional<T> getIfExists(final Scoped<T> scoped) {
        return Optional.empty();
    }

    @Override
    public void destroy(final Scoped<?> scoped) {
    }

    @Override
    public void close() {
    }
}
