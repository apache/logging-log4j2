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

import org.apache.logging.log4j.plugins.di.AnnotationAlias;
import org.apache.logging.log4j.plugins.di.Named;
import org.apache.logging.log4j.core.config.di.InitializationException;
import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotation;
import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotationElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class DefaultMetaAnnotation implements MetaAnnotation {

    static List<MetaAnnotation> fromAnnotations(final Annotation... annotations) {
        if (annotations.length == 0) {
            return Collections.emptyList();
        }
        final List<MetaAnnotation> metaAnnotations = new ArrayList<>(annotations.length);
        for (final Annotation annotation : annotations) {
            metaAnnotations.add(fromAnnotation(annotation));
        }
        return Collections.unmodifiableList(metaAnnotations);
    }

    private static DefaultMetaAnnotation fromAnnotation(final Annotation annotation) {
        final Class<? extends Annotation> originalType = annotation.annotationType();
        final Method[] annotationElements = originalType.getDeclaredMethods();
        final Set<MetaAnnotationElement<?>> elements = new LinkedHashSet<>(annotationElements.length);
        for (final Method element : annotationElements) {
            final String name;
            final Named named = element.getAnnotation(Named.class);
            if (named != null && !named.value().isEmpty()) {
                name = named.value();
            } else {
                name = element.getName();
            }
            final Object value = getAnnotationElementValue(annotation, element);
            elements.add(new DefaultMetaAnnotationElement<>(name, value, fromAnnotations(element.getAnnotations())));
        }
        final AnnotationAlias alias = originalType.getAnnotation(AnnotationAlias.class);
        final Class<? extends Annotation> annotationType = alias != null ? alias.value() : originalType;
        return new DefaultMetaAnnotation(annotationType, elements);
    }

    private static Object getAnnotationElementValue(final Annotation annotation, final Method element) {
        try {
            return element.invoke(annotation);
        } catch (final IllegalAccessException e) {
            throw new InitializationException("Cannot access element " + element.getName() + " of annotation " + annotation, e);
        } catch (final InvocationTargetException e) {
            throw new InitializationException("Cannot access element " + element.getName() + " of annotation " + annotation,
                    e.getCause());
        }
    }

    private final Class<? extends Annotation> annotationType;
    private final Set<MetaAnnotationElement<?>> elements;

    DefaultMetaAnnotation(final Class<? extends Annotation> annotationType, final Set<MetaAnnotationElement<?>> elements) {
        this.annotationType = annotationType;
        this.elements = Collections.unmodifiableSet(elements);
    }

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    @Override
    public Collection<MetaAnnotationElement<?>> getAnnotationElements() {
        return elements;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefaultMetaAnnotation that = (DefaultMetaAnnotation) o;
        return annotationType.equals(that.annotationType) &&
                elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annotationType, elements);
    }
}
