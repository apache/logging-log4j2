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

package org.apache.logging.log4j.plugins.spi.model;

import org.apache.logging.log4j.plugins.api.AliasFor;
import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.plugins.api.Ignore;
import org.apache.logging.log4j.plugins.spi.InjectionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// TODO: consider a composite class for qualifier collections or other potential ways to model this
public final class Qualifier {

    public static final Qualifier DEFAULT_QUALIFIER = new Qualifier(Default.class, Collections.emptyMap());

    public static Qualifier fromAnnotation(final Annotation annotation) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        final Method[] elements = annotationType.getDeclaredMethods();
        final Map<String, Object> attributes = new HashMap<>(elements.length);
        for (final Method element : elements) {
            if (!element.isAnnotationPresent(Ignore.class)) {
                attributes.put(element.getName(), getAnnotationElement(annotation, element));
            }
        }
        // FIXME: support default name for @Named when value is blank
        final AliasFor alias = annotationType.getAnnotation(AliasFor.class);
        final Class<? extends Annotation> qualifierType = alias != null ? alias.value() : annotationType;
        return new Qualifier(qualifierType, Collections.unmodifiableMap(attributes));
    }

    private static Object getAnnotationElement(final Annotation annotation, final Method element) {
        try {
            return element.invoke(annotation);
        } catch (final IllegalAccessException e) {
            throw new InjectionException("Cannot access element " + element.getName() + " of annotation " + annotation, e);
        } catch (final InvocationTargetException e) {
            throw new InjectionException("Cannot access element " + element.getName() + " of annotation " + annotation,
                    e.getCause());
        }
    }

    private final Class<? extends Annotation> qualifierType;
    private final Map<String, Object> attributes;

    private Qualifier(final Class<? extends Annotation> qualifierType, final Map<String, Object> attributes) {
        this.qualifierType = qualifierType;
        this.attributes = attributes;
    }

    public Class<? extends Annotation> getQualifierType() {
        return qualifierType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Qualifier that = (Qualifier) o;
        return qualifierType.equals(that.qualifierType) &&
                attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifierType, attributes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('@').append(qualifierType.getSimpleName());
        if (!attributes.isEmpty()) {
            sb.append(attributes);
        }
        return sb.toString();
    }
}
