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

package org.apache.logging.log4j.plugins.di;

import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Objects;

public class Key<T> {
    private final Type type;
    private final Class<T> rawType;
    private final Class<? extends Annotation> qualifierType;
    private final String name;
    private final int hashCode;

    protected Key() {
        type = TypeUtil.getSuperclassTypeParameter(getClass());
        rawType = TypeUtil.cast(TypeUtil.getRawType(type));
        final AnnotatedType superclass = getClass().getAnnotatedSuperclass();
        final Annotation qualifier = AnnotationUtil.getMetaAnnotation(superclass, QualifierType.class);
        qualifierType = qualifier != null ? qualifier.annotationType() : null;
        name = AnnotatedElementNameProvider.getName(superclass);
        hashCode = Objects.hash(type, qualifierType, name.toLowerCase(Locale.ROOT));
    }

    private Key(final Type type, final Class<T> rawType, final Class<? extends Annotation> qualifierType, final String name) {
        this.type = type;
        this.rawType = rawType;
        this.qualifierType = qualifierType;
        this.name = name;
        hashCode = Objects.hash(type, qualifierType, name.toLowerCase(Locale.ROOT));
    }

    public final Class<T> getRawType() {
        return rawType;
    }

    public Key<T> withName(final String name) {
        return new Key<>(type, rawType, qualifierType, name);
    }

    public Key<T> withQualifierType(final Class<? extends Annotation> qualifierType) {
        return new Key<>(type, rawType, qualifierType, name);
    }

    @Override
    public final boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Key<?>)) {
            return false;
        }
        final Key<?> that = (Key<?>) o;
        return TypeUtil.isEqual(this.type, that.type) &&
                this.name.equalsIgnoreCase(that.name) &&
                Objects.equals(this.qualifierType, that.qualifierType);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "Key{" +
                "type=" + type +
                ", qualifierType=" + qualifierType +
                ", name='" + name + '\'' +
                '}';
    }

    public static <T> Key<T> forClass(final Class<T> clazz) {
        return new Key<>(clazz, clazz, null, Strings.EMPTY);
    }

    public static <T> Key<T> forQualifiedNamedType(
            final Class<? extends Annotation> qualifierType, final String name, final Type type) {
        final Class<T> rawType = TypeUtil.cast(TypeUtil.getRawType(type));
        return new Key<>(type, rawType, qualifierType, name);
    }
}
