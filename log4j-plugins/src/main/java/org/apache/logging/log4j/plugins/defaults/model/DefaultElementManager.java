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

import org.apache.logging.log4j.plugins.api.AliasFor;
import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.plugins.api.Named;
import org.apache.logging.log4j.plugins.api.PrototypeScoped;
import org.apache.logging.log4j.plugins.api.QualifierType;
import org.apache.logging.log4j.plugins.api.ScopeType;
import org.apache.logging.log4j.plugins.api.Stereotype;
import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.model.ElementManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.model.MetaElement;
import org.apache.logging.log4j.plugins.spi.model.MetaExecutable;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;
import org.apache.logging.log4j.plugins.spi.model.Qualifier;
import org.apache.logging.log4j.plugins.spi.model.Variable;
import org.apache.logging.log4j.plugins.util.Cache;
import org.apache.logging.log4j.plugins.util.WeakCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultElementManager implements ElementManager {

    private enum AnnotationType {
        QUALIFIER, SCOPE, STEREOTYPE, UNKNOWN
    }

    private final Cache<Class<?>, MetaClass<?>> classCache = WeakCache.newWeakRefCache(DefaultMetaClass::newMetaClass);

    private final Cache<Class<? extends Annotation>, AnnotationType> annotationTypeCache = WeakCache.newCache(clazz -> {
        for (final Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == AliasFor.class) {
                type = ((AliasFor) annotation).value();
            }
            if (type == QualifierType.class) {
                return AnnotationType.QUALIFIER;
            }
            if (type == ScopeType.class) {
                return AnnotationType.SCOPE;
            }
            if (type == Stereotype.class) {
                return AnnotationType.STEREOTYPE;
            }
        }
        return AnnotationType.UNKNOWN;
    });

    @Override
    public <T> MetaClass<T> getMetaClass(final Class<T> clazz) {
        return classCache.get(clazz);
    }

    @Override
    public boolean isQualifierType(final Class<? extends Annotation> annotationType) {
        return getAnnotationType(annotationType) == AnnotationType.QUALIFIER;
    }

    @Override
    public Collection<Qualifier> getQualifiers(final MetaElement<?> element) {
        final Collection<Annotation> qualifiers = filterQualifiers(element.getAnnotations());
        if (qualifiers.stream().noneMatch(annotation -> annotation.annotationType() != Named.class)) {
            qualifiers.add(Default.INSTANCE);
        }
        return qualifiers.stream().map(Qualifier::fromAnnotation).collect(Collectors.toCollection(HashSet::new));
    }

    private Collection<Annotation> filterQualifiers(final Collection<Annotation> annotations) {
        final Collection<Annotation> qualifiers = new LinkedHashSet<>(annotations.size());
        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isQualifierType(annotationType)) {
                qualifiers.add(annotation);
            } else if (isStereotype(annotationType)) {
                qualifiers.addAll(filterQualifiers(getStereotypeDefinition(annotationType)));
            }
        }
        return qualifiers;
    }

    /**
     * Returns all the annotations associated with a {@linkplain Stereotype stereotype} annotation. This contains all
     * annotation values sans the stereotype annotation values themselves.
     */
    private Collection<Annotation> getStereotypeDefinition(final Class<? extends Annotation> annotationType) {
        final Stereotype stereotype = annotationType.getAnnotation(Stereotype.class);
        if (stereotype == null) {
            return Collections.emptySet();
        }
        final Annotation[] annotations = annotationType.getAnnotations();
        final Collection<Annotation> stereotypeDefinition = new LinkedHashSet<>(annotations.length);
        for (final Annotation annotation : annotations) {
            if (isStereotype(annotation.annotationType())) {
                stereotypeDefinition.addAll(getStereotypeDefinition(annotation.annotationType()));
            } else {
                stereotypeDefinition.add(annotation);
            }
        }
        return Collections.unmodifiableCollection(stereotypeDefinition);
    }

    private Class<? extends Annotation> getScopeType(final MetaElement<?> element) {
        final Collection<Class<? extends Annotation>> scopeTypes = filterScopeTypes(element.getAnnotations());
        return scopeTypes.isEmpty() ? PrototypeScoped.class : scopeTypes.iterator().next();
    }

    private Collection<Class<? extends Annotation>> filterScopeTypes(final Collection<Annotation> annotations) {
        // only expect at most one scope
        final Collection<Class<? extends Annotation>> scopeTypes = new LinkedHashSet<>(1);
        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isScopeType(annotationType)) {
                scopeTypes.add(annotationType);
            } else if (isStereotype(annotationType)) {
                scopeTypes.addAll(filterScopeTypes(getStereotypeDefinition(annotationType)));
            }
        }
        return Collections.unmodifiableCollection(scopeTypes);
    }

    private boolean isStereotype(final Class<? extends Annotation> annotationType) {
        return getAnnotationType(annotationType) == AnnotationType.STEREOTYPE;
    }

    private boolean isScopeType(final Class<? extends Annotation> annotationType) {
        return getAnnotationType(annotationType) == AnnotationType.SCOPE;
    }

    private AnnotationType getAnnotationType(final Class<? extends Annotation> annotationType) {
        return annotationTypeCache.get(annotationType);
    }

    @Override
    public <D, T> InjectionPoint<T> createFieldInjectionPoint(final MetaField<D, T> field, final Bean<D> owner) {
        Objects.requireNonNull(field);
        final Collection<Qualifier> qualifiers = getQualifiers(field);
        return new DefaultInjectionPoint<>(field.getBaseType(), qualifiers, owner, field, field);
    }

    @Override
    public <D, P> InjectionPoint<P> createParameterInjectionPoint(final MetaExecutable<D, ?> executable,
                                                                  final MetaParameter<P> parameter,
                                                                  final Bean<D> owner) {
        Objects.requireNonNull(executable);
        Objects.requireNonNull(parameter);
        final Collection<Qualifier> qualifiers = getQualifiers(parameter);
        return new DefaultInjectionPoint<>(parameter.getBaseType(), qualifiers, owner, executable, parameter);
    }

    @Override
    public <T> Variable<T> createVariable(final MetaElement<T> element) {
        Objects.requireNonNull(element);
        return createVariable(element, getQualifiers(element));
    }

    @Override
    public <T> Variable<T> createVariable(final InjectionPoint<T> point) {
        Objects.requireNonNull(point);
        return createVariable(point.getElement(), point.getQualifiers());
    }

    private <T> Variable<T> createVariable(final MetaElement<T> element, final Collection<Qualifier> qualifiers) {
        final Collection<Type> types = element.getTypeClosure();
        final Class<? extends Annotation> scopeType = getScopeType(element);
        return DefaultVariable.newVariable(types, qualifiers, scopeType);
    }

    @Override
    public void close() {
        classCache.close();
    }
}
