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

package org.apache.logging.log4j.plugins.defaults.model;

import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.plugins.spi.model.MetaAnnotation;
import org.apache.logging.log4j.plugins.spi.model.MetaAnnotationElement;
import org.apache.logging.log4j.plugins.spi.model.Qualifiers;
import org.apache.logging.log4j.util.StringBuilders;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a normalized set of {@linkplain org.apache.logging.log4j.plugins.api.QualifierType qualifier annotations}.
 */
class DefaultQualifiers implements Qualifiers {
    private final Set<MetaAnnotation> qualifiers;

    DefaultQualifiers(final Set<MetaAnnotation> qualifiers) {
        this.qualifiers = qualifiers;
    }

    @Override
    public boolean hasDefaultQualifier() {
        return qualifiers.stream().anyMatch(q -> q.getAnnotationType() == Default.class);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefaultQualifiers that = (DefaultQualifiers) o;
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
        for (final MetaAnnotation qualifier : qualifiers) {
            sb.append('@').append(qualifier.getAnnotationType().getSimpleName());
            final Collection<MetaAnnotationElement<?>> elements = qualifier.getAnnotationElements();
            if (!elements.isEmpty()) {
                sb.append('(');
                for (final MetaAnnotationElement<?> element : elements) {
                    StringBuilders.appendKeyDqValue(sb, element.getName(), element.getValue()).append(", ");
                }
                sb.delete(sb.length() - 2, sb.length()).append(')');
            }
            sb.append(", ");
        }
        return sb.delete(sb.length() - 2, sb.length()).append(']').toString();
    }
}
