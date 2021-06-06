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

import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotation;
import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotationElement;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

class DefaultMetaAnnotationElement<T> implements MetaAnnotationElement<T> {
    private final String name;
    private final T value;
    private final Collection<MetaAnnotation> annotations;

    DefaultMetaAnnotationElement(final String name, final T value, final Collection<MetaAnnotation> annotations) {
        this.name = name;
        this.value = value;
        this.annotations = Collections.unmodifiableCollection(annotations);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return value.getClass();
    }

    @Override
    public Collection<MetaAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public MetaAnnotationElement<T> withNewValue(final T value) {
        return new DefaultMetaAnnotationElement<>(name, value, annotations);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefaultMetaAnnotationElement<?> that = (DefaultMetaAnnotationElement<?>) o;
        return name.equals(that.name) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }
}
