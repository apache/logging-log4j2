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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

abstract class AbstractBean<T> implements Bean<T> {
    private final Collection<Type> types;
    private final String name;
    private final Class<? extends Annotation> scopeType;
    private final Class<?> declaringClass;

    AbstractBean(final Collection<Type> types, final String name, final Class<? extends Annotation> scopeType,
                 final Class<?> declaringClass) {
        this.types = types;
        this.name = name;
        this.scopeType = scopeType;
        this.declaringClass = declaringClass;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
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
        return scopeType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractBean<?> that = (AbstractBean<?>) o;
        return types.equals(that.types) && name.equals(that.name) && scopeType.equals(that.scopeType) &&
                declaringClass.equals(that.declaringClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, name, scopeType, declaringClass);
    }

    abstract boolean isTrackingDependencies();
}
