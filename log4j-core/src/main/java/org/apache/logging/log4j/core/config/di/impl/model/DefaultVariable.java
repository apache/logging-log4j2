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

package org.apache.logging.log4j.core.config.di.impl.model;

import org.apache.logging.log4j.core.config.di.api.model.Qualifiers;
import org.apache.logging.log4j.core.config.di.api.model.Variable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class DefaultVariable implements Variable {
    private final Collection<Type> types;
    private final Qualifiers qualifiers;
    private final Class<? extends Annotation> scopeType;

    DefaultVariable(final Collection<Type> types, final Qualifiers qualifiers,
                    final Class<? extends Annotation> scopeType) {
        this.types = Objects.requireNonNull(types);
        this.qualifiers = Objects.requireNonNull(qualifiers);
        this.scopeType = Objects.requireNonNull(scopeType);
    }

    @Override
    public Collection<Type> getTypes() {
        return Collections.unmodifiableCollection(types);
    }

    @Override
    public Qualifiers getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScopeType() {
        return scopeType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefaultVariable that = (DefaultVariable) o;
        return types.equals(that.types) &&
                qualifiers.equals(that.qualifiers) &&
                scopeType.equals(that.scopeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, qualifiers, scopeType);
    }

    @Override
    public String toString() {
        return "DefaultVariable{" +
                "types=" + types +
                ", qualifiers=" + qualifiers +
                ", scope=" + scopeType +
                '}';
    }
}
