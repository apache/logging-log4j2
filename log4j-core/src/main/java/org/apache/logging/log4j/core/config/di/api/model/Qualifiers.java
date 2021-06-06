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

package org.apache.logging.log4j.core.config.di.api.model;

import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.util.StringBuilders;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a normalized set of {@linkplain org.apache.logging.log4j.plugins.api.QualifierType qualifier annotations}.
 */
public final class Qualifiers {

    public static final Qualifiers DEFAULT = new Qualifiers(Collections.singletonMap(Default.class, Collections.emptyMap()));

    public static Qualifiers fromAnnotations(final Set<MetaAnnotation> qualifiers) {
        return new Qualifiers(qualifiers.stream()
                .collect(Collectors.toMap(MetaAnnotation::getAnnotationType,
                        ann -> ann.getAnnotationElements().stream()
                                .collect(Collectors.toMap(MetaElement::getName, MetaAnnotationElement::getValue)))));
    }

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
        if (qualifiers.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder().append('[');
        for (final Map.Entry<Class<? extends Annotation>, Map<String, Object>> qualifier : qualifiers.entrySet()) {
            sb.append('@').append(qualifier.getKey().getSimpleName());
            final Map<String, Object> elements = qualifier.getValue();
            if (!elements.isEmpty()) {
                sb.append('(');
                for (final Map.Entry<String, Object> element : elements.entrySet()) {
                    StringBuilders.appendKeyDqValue(sb, element.getKey(), element.getValue()).append(", ");
                }
                sb.delete(sb.length() - 2, sb.length()).append(')');
            }
            sb.append(", ");
        }
        return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
    }
}
