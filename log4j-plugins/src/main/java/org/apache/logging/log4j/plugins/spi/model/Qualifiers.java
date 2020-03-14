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
import org.apache.logging.log4j.plugins.api.Named;
import org.apache.logging.log4j.plugins.spi.InitializationException;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a normalized set of {@linkplain org.apache.logging.log4j.plugins.api.QualifierType qualifier annotations}.
 */
public final class Qualifiers {
    private final Map<Class<? extends Annotation>, Map<String, Object>> qualifiers;

    private Qualifiers(final Map<Class<? extends Annotation>, Map<String, Object>> qualifiers) {
        this.qualifiers = qualifiers;
    }

    public boolean hasDefaultQualifier() {
        return qualifiers.containsKey(Default.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Qualifiers that = (Qualifiers) o;
        return qualifiers.equals(that.qualifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiers);
    }

    @Override
    public String toString() {
        final Set<Map.Entry<Class<? extends Annotation>, Map<String, Object>>> entries = qualifiers.entrySet();
        if (entries.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder().append('[');
        for (final Map.Entry<Class<? extends Annotation>, Map<String, Object>> entry : entries) {
            sb.append('@').append(entry.getKey().getSimpleName());
            final Map<String, Object> attributes = entry.getValue();
            if (!attributes.isEmpty()) {
                sb.append('(');
                for (final Map.Entry<String, Object> attribute : attributes.entrySet()) {
                    StringBuilders.appendKeyDqValue(sb, attribute.getKey(), attribute.getValue());
                    sb.append(", ");
                }
                sb.delete(sb.length() - 2, sb.length()).append(')');
            }
            sb.append(", ");
        }
        return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
    }

    /**
     * Creates a normalized Qualifiers instance from a collection of qualifier annotation instances.
     */
    public static Qualifiers fromQualifierAnnotations(final Collection<Annotation> annotations) {
        return fromQualifierAnnotations(annotations, Strings.EMPTY);
    }

    /**
     * Creates a normalized Qualifiers instance from a collection of qualifier annotation instances and a default name
     * to use when {@linkplain Named named qualifiers} do not specify a value.
     */
    public static Qualifiers fromQualifierAnnotations(final Collection<Annotation> annotations, final String defaultName) {
        final Map<Class<? extends Annotation>, Map<String, Object>> qualifiers = new HashMap<>(annotations.size());
        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final AliasFor alias = annotationType.getAnnotation(AliasFor.class);
            final Class<? extends Annotation> qualifierType = alias != null ? alias.value() : annotationType;
            qualifiers.put(qualifierType, getQualifierAttributes(annotation, defaultName));
        }
        if (needsDefaultQualifier(qualifiers.keySet())) {
            qualifiers.put(Default.class, Collections.emptyMap());
        }
        return new Qualifiers(Collections.unmodifiableMap(qualifiers));
    }

    private static Map<String, Object> getQualifierAttributes(final Annotation annotation, final String defaultName) {
        final Class<? extends Annotation> annotationType = annotation.annotationType();
        final Method[] elements = annotationType.getDeclaredMethods();
        final Map<String, Object> attributes = new HashMap<>(elements.length);
        for (final Method element : elements) {
            if (!element.isAnnotationPresent(Ignore.class)) {
                final String name = element.getName();
                final Object value = getAnnotationElementValue(annotation, element);
                if (annotationType == Named.class && name.equals("value") && value.toString().isEmpty()) {
                    attributes.put("value", defaultName);
                } else {
                    attributes.put(name, value);
                }
            }
        }
        return Collections.unmodifiableMap(attributes);
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

    private static boolean needsDefaultQualifier(final Collection<Class<? extends Annotation>> qualifierTypes) {
        if (qualifierTypes.contains(Default.class)) {
            return false;
        }
        for (final Class<? extends Annotation> qualifierType : qualifierTypes) {
            if (qualifierType != Named.class) {
                return false;
            }
        }
        return true;
    }
}
